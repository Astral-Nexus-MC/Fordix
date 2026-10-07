package com.omneria.fordix.wiki;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Copies pages of a MediaWiki wiki (Fandom, minecraft.wiki, most mod wikis) into the server's {@link WikiStore}: the page
 * through the wiki's {@code api.php} ({@code action=parse}), converted to the guide markup by {@link HtmlToMarkup}, and
 * its pictures saved in the {@link ImageStore}. Runs on its own thread, one page at a time and politely slowly, and
 * reports to a callback. The text stays the wiki's: the page keeps its address and the wiki its licence.
 */
public final class WikiImporter {
    public record Report(int pages, int images, List<String> failures) {
    }

    private static final int MAX_LINKED_PAGES = 40;
    private static final int MAX_IMAGES_PER_PAGE = 8;
    private static final int MAX_DOWNLOAD = 4_000_000;
    private static final long PAUSE_MS = 350L;
    private static final String AGENT = "Fordix-wiki-import/1.0 (Minecraft server mod; in-game copy of a wiki page)";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "fordix-wiki-import");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    private final WikiStore store;
    private final ImageStore images;
    private final Consumer<String> progress;
    private int importedImages;

    private WikiImporter(WikiStore store, ImageStore images, Consumer<String> progress) {
        this.store = store;
        this.images = images;
        this.progress = progress;
    }

    /**
     * Imports one page, and with {@code withLinks} the pages it links to (up to {@link #MAX_LINKED_PAGES}).
     * {@code input} is a page address, or a page title for a wiki that already has its address.
     */
    public static CompletableFuture<Report> importPage(WikiStore store, ImageStore images, String wikiId, String input,
                                                       boolean withLinks, Consumer<String> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new WikiImporter(store, images, progress).run(wikiId, input.strip(), withLinks);
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
            }
        }, WORKER);
    }

    /** "iron_sword" becomes the page title "Iron_Sword". */
    public static String titleOf(String path) {
        StringBuilder title = new StringBuilder();
        for (String word : path.split("_")) {
            if (!word.isEmpty()) {
                title.append(title.length() > 0 ? "_" : "").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return title.toString();
    }

    /**
     * Imports the pages of these titles (the wiki must already have its address), skipping the ones it has and the ones the
     * wiki does not know; reports every 25 pages.
     */
    public static CompletableFuture<Report> importTitles(WikiStore store, ImageStore images, String wikiId, List<String> titles,
                                                         Consumer<String> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new WikiImporter(store, images, progress).runTitles(wikiId, titles);
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
            }
        }, WORKER);
    }

    private Report runTitles(String wikiId, List<String> titles) throws IOException {
        WikiInfo info = store.info(wikiId);
        if (info == null || info.source.isEmpty()) {
            throw new IOException("wiki '" + wikiId + "' has no address yet: import one page of it first");
        }
        if (info.api.isEmpty()) {
            info.api = discoverApi(info.source, info);
            store.update(info);
        }
        List<String> failures = new ArrayList<>();
        int done = 0;
        int asked = 0;
        int missing = 0;
        for (String title : titles) {
            if (store.page(wikiId, title) != null) {
                continue;
            }
            pause();
            try {
                importOne(info, info.source, title, failures);
                done++;
            } catch (IOException e) {
                missing++;
                if (failures.size() < 20) {
                    failures.add(title + " : " + e.getMessage());
                }
            }
            if (++asked % 25 == 0) {
                progress.accept(asked + "/" + titles.size());
            }
        }
        progress.accept(missing + " pages not found on the wiki");
        return new Report(done, importedImages, failures);
    }

    /**
     * Copies, from the same wiki in another language, the page of each page {@code sourceWiki} has: the source wiki's own
     * API says what each page is called in {@code lang} ({@code prop=langlinks}), and the page of that name is imported into
     * {@code targetWiki} under its own name, with the English name leading to it. A page the other language does not have
     * is left out (the client falls back to the source wiki).
     */
    public static CompletableFuture<Report> translate(WikiStore store, ImageStore images, String sourceWiki, String targetWiki,
                                                      String lang, Consumer<String> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new WikiImporter(store, images, progress).runTranslate(sourceWiki, targetWiki, lang);
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
            }
        }, WORKER);
    }

    private Report runTranslate(String sourceWiki, String targetWiki, String lang) throws IOException {
        WikiInfo from = store.info(sourceWiki);
        WikiInfo to = store.info(targetWiki);
        if (from == null || from.api.isEmpty() || to == null || to.source.isEmpty()) {
            throw new IOException("both wikis must exist, and the source wiki needs an import first");
        }
        if (to.api.isEmpty()) {
            to.api = discoverApi(to.source, to);
            store.update(to);
        }
        List<WikiStore.Entry> english = store.index(sourceWiki);
        List<String> failures = new ArrayList<>();
        int done = 0;
        int noTranslation = 0;
        for (int at = 0; at < english.size(); at += 50) {
            List<WikiStore.Entry> batch = english.subList(at, Math.min(english.size(), at + 50));
            Map<String, String> translated = langlinks(from, batch, lang);
            pause();
            for (WikiStore.Entry entry : batch) {
                String name = translated.get(entry.title());
                if (name == null) {
                    noTranslation++;
                    continue;
                }
                if (store.page(targetWiki, entry.slug()) != null) {
                    continue;
                }
                try {
                    importOne(to, to.source, name.replace(' ', '_'), failures);
                    String frSlug = name.replace(' ', '_');
                    if (!frSlug.equals(entry.slug()) && store.page(targetWiki, entry.slug()) == null) {
                        WikiPage alias = new WikiPage(entry.slug(), entry.title(), "", to.source, System.currentTimeMillis());
                        alias.redirectTo = frSlug;
                        store.save(targetWiki, alias);
                    }
                    done++;
                } catch (IOException e) {
                    if (failures.size() < 20) {
                        failures.add(name + " : " + e.getMessage());
                    }
                }
                pause();
            }
            progress.accept(Math.min(english.size(), at + 50) + "/" + english.size());
        }
        progress.accept(noTranslation + " pages without " + lang + " version");
        return new Report(done, importedImages, failures);
    }

    /** The titles of a batch of pages in another language: {"Diamond Sword" -> "\u00c9p\u00e9e"} (a section anchor is dropped). */
    private Map<String, String> langlinks(WikiInfo wiki, List<WikiStore.Entry> batch, String lang) throws IOException {
        StringBuilder titles = new StringBuilder();
        for (WikiStore.Entry entry : batch) {
            titles.append(titles.length() > 0 ? "|" : "").append(entry.title());
        }
        String url = wiki.api + "?action=query&prop=langlinks&lllang=" + lang + "&lllimit=500&format=json&formatversion=2&titles="
                + URLEncoder.encode(titles.toString(), StandardCharsets.UTF_8);
        return langlinksOf(text(url));
    }

    /** Reads a {@code prop=langlinks} answer (format version 2) as source title to translated title. Public for tests. */
    public static Map<String, String> langlinksOf(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            for (JsonElement page : JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("query").getAsJsonArray("pages")) {
                JsonObject object = page.getAsJsonObject();
                if (!object.has("langlinks") || object.getAsJsonArray("langlinks").isEmpty()) {
                    continue;
                }
                JsonObject link = object.getAsJsonArray("langlinks").get(0).getAsJsonObject();
                String name = link.has("title") ? link.get("title").getAsString() : link.get("*").getAsString();
                int anchor = name.indexOf('#');
                name = (anchor >= 0 ? name.substring(0, anchor) : name).strip();
                if (!name.isEmpty()) {
                    result.put(object.get("title").getAsString(), name);
                }
            }
        } catch (RuntimeException e) {
            // an odd answer: nothing translated
        }
        return result;
    }

    /** A wiki installed in a folder ("/tfwiki/index.php/Main_Page" or "/folder/wiki/Page") keeps that folder in its address. */
    static String wikiRoot(String path) {
        for (String marker : List.of("/index.php", "/wiki/")) {
            int at = path == null ? -1 : path.indexOf(marker);
            if (at > 0) {
                return path.substring(0, at);
            }
        }
        return "";
    }

    private Report run(String wikiId, String input, boolean withLinks) throws IOException {
        String origin;
        String title;
        WikiInfo info = store.info(wikiId);
        if (input.startsWith("http://") || input.startsWith("https://")) {
            URI uri = URI.create(input);
            origin = uri.getScheme() + "://" + uri.getAuthority() + wikiRoot(uri.getRawPath());
            String path = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
            title = HtmlToMarkup.pageOf(path);
            if (title == null) {
                throw new IOException("no page name in " + input);
            }
        } else {
            if (info == null || info.source.isEmpty()) {
                throw new IOException("wiki '" + wikiId + "' has no address yet: give a full page address the first time");
            }
            origin = info.source;
            title = input.replace(' ', '_');
        }
        if (info == null) {
            info = store.ensure(wikiId, wikiId, origin, "");
        }
        if (info.source.isEmpty()) {
            info.source = origin;
            store.update(info);
        }
        if (info.api.isEmpty()) {
            info.api = discoverApi(origin, info);
            store.update(info);
        }
        List<String> done = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        List<String> links = importOne(info, origin, title, failures);
        done.add(title);
        if (withLinks) {
            List<String> queue = new ArrayList<>();
            for (String link : links) {
                if (store.page(wikiId, link) == null && !link.equals(title) && queue.size() < MAX_LINKED_PAGES) {
                    queue.add(link);
                }
            }
            int count = 0;
            for (String link : queue) {
                pause();
                try {
                    importOne(info, origin, link, failures);
                    done.add(link);
                } catch (IOException e) {
                    failures.add(link + " : " + e.getMessage());
                }
                if (++count % 10 == 0) {
                    progress.accept(count + "/" + queue.size());
                }
            }
        }
        return new Report(done.size(), importedImages, failures);
    }

    /** The wiki's api.php: tries the usual two places, and takes the wiki's name and licence from it when not set yet. */
    private String discoverApi(String origin, WikiInfo info) throws IOException {
        IOException last = null;
        for (String candidate : List.of(origin + "/api.php", origin + "/w/api.php")) {
            try {
                String json = text(candidate + "?action=query&meta=siteinfo&siprop=general%7Crightsinfo&format=json");
                JsonObject query = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("query");
                JsonObject general = query.getAsJsonObject("general");
                if (info.title.equals(info.id) && general.has("sitename")) {
                    info.title = general.get("sitename").getAsString();
                }
                if (info.license.isEmpty() && query.has("rightsinfo") && query.getAsJsonObject("rightsinfo").has("text")) {
                    info.license = query.getAsJsonObject("rightsinfo").get("text").getAsString();
                }
                return candidate;
            } catch (IOException | RuntimeException e) {
                last = e instanceof IOException io ? io : new IOException(e.getMessage());
            }
        }
        throw new IOException("no MediaWiki API found at " + origin + (last == null ? "" : " (" + last.getMessage() + ")"));
    }

    /** Imports one page; returns the pages it links to. */
    private List<String> importOne(WikiInfo info, String origin, String title, List<String> failures) throws IOException {
        return importOne(info, origin, title, failures, 0);
    }

    private List<String> importOne(WikiInfo info, String origin, String title, List<String> failures, int depth) throws IOException {
        // A wiki frozen at a date takes the page as it was then: what did not exist yet is skipped, like newer content.
        String target = info.asOf.isEmpty() ? "page=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                : "oldid=" + revisionAt(info, title);
        String url = info.api + "?action=parse&" + target
                + "&prop=text%7Cdisplaytitle&format=json&formatversion=2&redirects=1&disableeditsection=1&disablelimitreport=1&disabletoc=1";
        JsonObject root = JsonParser.parseString(text(url)).getAsJsonObject();
        if (root.has("error")) {
            JsonElement info1 = root.getAsJsonObject("error").get("info");
            throw new IOException(info1 == null ? "page not found" : info1.getAsString());
        }
        JsonObject parse = root.getAsJsonObject("parse");
        String pageTitle = parse.get("title").getAsString();
        String slug = pageTitle.replace(' ', '_');
        // The old version of a page was only a redirect (the wiki has been reorganized since): copy what it led to.
        String redirect = redirectTarget(parse.get("text").getAsString());
        if (redirect != null && redirect.matches("^[A-Za-z_ ]{2,20}:.*")) {
            // "Dungeons:Mace", "Earth:..." : another game's page, so this one is not part of the game on that date.
            throw new IOException("only a redirect to " + redirect + " on that date");
        }
        if (redirect != null && depth < 3) {
            String targetSlug = redirect.replace(' ', '_');
            WikiPage stub = new WikiPage(slug, pageTitle, "", origin + "/index.php?title=" + URLEncoder.encode(slug, StandardCharsets.UTF_8),
                    System.currentTimeMillis());
            stub.redirectTo = targetSlug;
            store.save(info.id, stub);
            return store.page(info.id, targetSlug) != null ? List.of() : importOne(info, origin, redirect, failures, depth + 1);
        }
        Map<String, String> downloads = new LinkedHashMap<>();
        HtmlToMarkup.Result result = HtmlToMarkup.convert(parse.get("text").getAsString(), image -> {
            String absolute = absolute(origin, image.src());
            if (absolute == null || downloads.size() >= MAX_IMAGES_PER_PAGE) {
                return null;
            }
            absolute = original(absolute);
            String name = ImageStore.nameFor(absolute);
            downloads.put(name, absolute);
            return name;
        }, info.javaOnly);
        String markup = result.markup();
        for (Map.Entry<String, String> download : downloads.entrySet()) {
            if (images.exists(download.getKey())) {
                continue;
            }
            try {
                String saved = images.save(bytes(download.getValue(), MAX_DOWNLOAD), download.getValue());
                if (saved == null) {
                    throw new IOException("not a readable picture");
                }
                importedImages++;
            } catch (IOException e) {
                markup = markup.replaceAll("(?m)^@image " + java.util.regex.Pattern.quote(download.getKey()) + "(\\s*\\|.*)?$\\n?\\n?", "");
                failures.add(download.getValue() + " : " + e.getMessage());
            }
        }
        // The page's own name opens most wiki pages as a first line: the screen already shows it as the title.
        String firstLine = markup.split("\n", 2)[0];
        if (firstLine.equalsIgnoreCase(pageTitle)) {
            markup = markup.length() > firstLine.length() ? markup.substring(firstLine.length()).stripLeading() : "";
        }
        WikiPage page = new WikiPage(slug, pageTitle, markup.strip(), origin + "/index.php?title=" + URLEncoder.encode(slug, StandardCharsets.UTF_8),
                System.currentTimeMillis());
        page.links = new ArrayList<>(result.links());
        store.save(info.id, page);
        // Asked as "Oak_Planks", answered as "Planks": the name asked leads to the page too (an item sheet looks it up by its id).
        String requested = title.replace(' ', '_');
        if (!requested.equals(slug) && store.page(info.id, requested) == null) {
            WikiPage alias = new WikiPage(requested, requested.replace('_', ' '), "", page.source, System.currentTimeMillis());
            alias.redirectTo = slug;
            store.save(info.id, alias);
        }
        return result.links();
    }

    /** The page a redirect page leads to (from its HTML), or null when the page is not a redirect. */
    public static String redirectTarget(String html) {
        if (!html.contains("redirectMsg")) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("redirectText.*?<a [^>]*title=\"([^\"]+)\"", java.util.regex.Pattern.DOTALL).matcher(html);
        return matcher.find() ? HtmlToMarkup.decode(matcher.group(1)) : null;
    }

    /** The revision of a page as it stood on the wiki's date ({@code asOf}), or an error when the page did not exist yet. */
    private long revisionAt(WikiInfo info, String title) throws IOException {
        String url = info.api + "?action=query&prop=revisions&titles=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&rvlimit=1&rvprop=ids%7Ctimestamp&rvdir=older&rvstart=" + info.asOf + "T23:59:59Z&redirects=1&format=json&formatversion=2";
        Long revision = revisionId(text(url));
        if (revision == null) {
            throw new IOException("did not exist yet on " + info.asOf);
        }
        return revision;
    }

    /** The revision id in a {@code prop=revisions} answer, or null when the page is missing or has no revision at that date. */
    public static Long revisionId(String json) {
        try {
            JsonObject query = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("query");
            JsonObject page = query.getAsJsonArray("pages").get(0).getAsJsonObject();
            if (page.has("missing") || !page.has("revisions") || page.getAsJsonArray("revisions").isEmpty()) {
                return null;
            }
            return page.getAsJsonArray("revisions").get(0).getAsJsonObject().get("revid").getAsLong();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Fandom serves WebP unless asked for the original file, and the game only reads PNG. */
    private static String original(String url) {
        if (url.contains("nocookie.net") && !url.contains("format=original")) {
            return url + (url.contains("?") ? "&" : "?") + "format=original";
        }
        return url;
    }

    private static String absolute(String origin, String src) {
        if (src.startsWith("//")) {
            return "https:" + src;
        }
        if (src.startsWith("/")) {
            // A root-relative address starts at the host, not at the wiki's folder.
            return URI.create(origin).resolve(src).toString();
        }
        return src.startsWith("http://") || src.startsWith("https://") ? src : null;
    }

    private static void pause() {
        try {
            Thread.sleep(PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String text(String url) throws IOException {
        return new String(bytes(url, 12_000_000), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String url, int limit) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(25))
                        .header("User-Agent", AGENT).GET().build();
                HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() == 429 || response.statusCode() >= 500) {
                    if (attempt == 0) {
                        pause();
                        continue;
                    }
                }
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode());
                }
                try (InputStream in = response.body()) {
                    byte[] data = in.readNBytes(limit + 1);
                    if (data.length > limit) {
                        throw new IOException("answer too big");
                    }
                    return data;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted");
            } catch (IllegalArgumentException e) {
                throw new IOException("bad address " + url);
            }
        }
    }
}
