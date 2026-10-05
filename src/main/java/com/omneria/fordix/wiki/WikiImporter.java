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

    private Report run(String wikiId, String input, boolean withLinks) throws IOException {
        String origin;
        String title;
        WikiInfo info = store.info(wikiId);
        if (input.startsWith("http://") || input.startsWith("https://")) {
            URI uri = URI.create(input);
            origin = uri.getScheme() + "://" + uri.getAuthority();
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
        String url = info.api + "?action=parse&page=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&prop=text%7Cdisplaytitle&format=json&formatversion=2&redirects=1&disableeditsection=1&disablelimitreport=1&disabletoc=1";
        JsonObject root = JsonParser.parseString(text(url)).getAsJsonObject();
        if (root.has("error")) {
            JsonElement info1 = root.getAsJsonObject("error").get("info");
            throw new IOException(info1 == null ? "page not found" : info1.getAsString());
        }
        JsonObject parse = root.getAsJsonObject("parse");
        String pageTitle = parse.get("title").getAsString();
        String slug = pageTitle.replace(' ', '_');
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
        });
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
        return result.links();
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
            return origin + src;
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
