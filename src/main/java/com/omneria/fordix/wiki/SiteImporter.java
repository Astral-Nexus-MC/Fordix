package com.omneria.fordix.wiki;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copies a documentation site that is not a MediaWiki (MineColonies' wiki is a static site): starting from one page, it follows
 * the links that stay under the same folder of the same host (the site's own menu lists every page), and converts the main part of
 * each page ({@code <main>}, else {@code <article>}) with {@link HtmlToMarkup}. A page is named after its path
 * ("buildings/builder" becomes "buildings_builder").
 */
public final class SiteImporter {
    public static final int MAX_PAGES = 400;
    private static final int MAX_IMAGES_PER_PAGE = 8;
    private static final int MAX_DOWNLOAD = 4_000_000;
    private static final Pattern LINK = Pattern.compile("<a\\s[^>]*?href\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern H1 = Pattern.compile("<h1[^>]*>(.*?)</h1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "fordix-site-import");
        thread.setDaemon(true);
        return thread;
    });

    private SiteImporter() {
    }

    public static CompletableFuture<WikiImporter.Report> importSite(WikiStore store, ImageStore images, String wikiId, String start,
                                                                   Consumer<String> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return run(store, images, wikiId, start, progress);
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
            }
        }, WORKER);
    }

    private static WikiImporter.Report run(WikiStore store, ImageStore images, String wikiId, String start, Consumer<String> progress)
            throws IOException {
        URI first;
        try {
            first = URI.create(start.strip());
        } catch (IllegalArgumentException e) {
            throw new IOException("bad address " + start);
        }
        if (first.getScheme() == null || first.getAuthority() == null || !first.getScheme().startsWith("http")) {
            throw new IOException("give a full address (https://...)");
        }
        String origin = first.getScheme() + "://" + first.getAuthority();
        String prefix = folderOf(first.getPath());
        WikiInfo info = store.info(wikiId);
        if (info == null) {
            info = store.ensure(wikiId, wikiId, origin + prefix, "");
        }
        if (info.source.isEmpty()) {
            info.source = origin + prefix;
            store.update(info);
        }
        Deque<String> queue = new ArrayDeque<>();
        Set<String> seen = new LinkedHashSet<>();
        String startPath = normalize(first.getPath());
        queue.add(startPath);
        seen.add(startPath);
        List<String> failures = new ArrayList<>();
        int pages = 0;
        int imageCount = 0;
        while (!queue.isEmpty() && pages < MAX_PAGES) {
            String path = queue.poll();
            String url = origin + path;
            try {
                String html = fetch(url);
                for (String link : links(html, URI.create(url), origin, prefix)) {
                    if (seen.size() < MAX_PAGES * 2 && seen.add(link)) {
                        queue.add(link);
                    }
                }
                imageCount += save(store, images, info, origin, prefix, path, url, html, failures);
                pages++;
            } catch (IOException e) {
                if (failures.size() < 20) {
                    failures.add(path + " : " + e.getMessage());
                }
            }
            if (pages > 0 && pages % 10 == 0) {
                progress.accept(pages + " pages, " + queue.size() + " left");
            }
            WikiImporter.pause();
        }
        return new WikiImporter.Report(pages, imageCount, failures);
    }

    private static final java.net.http.HttpClient HTTP = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(15)).followRedirects(java.net.http.HttpClient.Redirect.NEVER).build();

    /**
     * A page's HTML, following redirects by hand: the redirect Java refuses to follow by itself (https to http, which some static
     * sites answer for an address without its final slash) is followed on the same host, kept in https.
     */
    static String fetch(String url) throws IOException {
        String current = url;
        for (int hop = 0; hop < 5; hop++) {
            try {
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(URI.create(current))
                        .timeout(java.time.Duration.ofSeconds(30)).header("User-Agent", "Fordix-wiki-import/1.0 (Minecraft server mod; in-game copy of a page)")
                        .GET().build();
                java.net.http.HttpResponse<byte[]> response = HTTP.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
                int status = response.statusCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = response.headers().firstValue("location").orElse("");
                    if (location.isEmpty()) {
                        throw new IOException("HTTP " + status);
                    }
                    URI next = URI.create(current).resolve(location);
                    if (next.getScheme().equals("http") && URI.create(current).getScheme().equals("https")
                            && next.getAuthority().equalsIgnoreCase(URI.create(current).getAuthority())) {
                        next = URI.create("https" + next.toString().substring(4));
                    }
                    if (!next.getAuthority().equalsIgnoreCase(URI.create(url).getAuthority())) {
                        throw new IOException("redirected to another site");
                    }
                    current = next.toString();
                    continue;
                }
                if (status != 200) {
                    throw new IOException("HTTP " + status);
                }
                if (response.body().length > 12_000_000) {
                    throw new IOException("answer too big");
                }
                return new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted");
            } catch (IllegalArgumentException e) {
                throw new IOException("bad address " + current);
            }
        }
        throw new IOException("too many redirects");
    }

    private static int save(WikiStore store, ImageStore images, WikiInfo info, String origin, String prefix, String path, String url,
                            String html, List<String> failures) {
        String slug = slugOf(path, prefix);
        String title = titleOf(html, slug);
        Map<String, String> downloads = new LinkedHashMap<>();
        String content = forVersion(mainPart(html), versionCode(info.gameVersion)).replaceAll("(?is)<div[^>]*version-selector.*?</select>\\s*</div>", " ");
        HtmlToMarkup.Result result = HtmlToMarkup.convert(content, image -> {
            String absolute = WikiImporter.absolute(origin, image.src());
            // The game reads PNG and JPEG only: a WebP or SVG picture would only be a failure to report. Site logos are not content.
            String file = image.src().toLowerCase(Locale.ROOT).replaceAll("[?#].*$", "");
            if (absolute == null || downloads.size() >= MAX_IMAGES_PER_PAGE || file.endsWith(".webp") || file.endsWith(".svg")
                    || image.alt().toLowerCase(Locale.ROOT).contains("logo")) {
                return null;
            }
            String name = ImageStore.nameFor(absolute);
            downloads.put(name, absolute);
            return name;
        }, false);
        String markup = result.markup();
        int saved = 0;
        for (Map.Entry<String, String> download : downloads.entrySet()) {
            if (images.exists(download.getKey())) {
                continue;
            }
            try {
                if (images.save(WikiImporter.bytes(download.getValue(), MAX_DOWNLOAD), download.getValue()) == null) {
                    throw new IOException("not a readable picture");
                }
                saved++;
            } catch (IOException e) {
                markup = markup.replaceAll("(?m)^@image " + Pattern.quote(download.getKey()) + "(\\s*\\|.*)?$\\n?\\n?", "");
                if (failures.size() < 20) {
                    failures.add(download.getValue() + " : " + e.getMessage());
                }
            }
        }
        String firstLine = markup.split("\n", 2)[0];
        if (firstLine.equalsIgnoreCase(title)) {
            markup = markup.length() > firstLine.length() ? markup.substring(firstLine.length()).stripLeading() : "";
        }
        WikiPage page = new WikiPage(slug, title, markup.strip(), url, System.currentTimeMillis());
        page.category = categoryOf(path, prefix);
        store.save(info.id, page);
        return saved;
    }

    /** The folder an address lives in, with slashes at both ends: "/wiki/" for "/wiki/" or "/wiki/index.html"; "/" at the root. */
    static String folderOf(String path) {
        String folder = path == null || path.isEmpty() ? "/" : path;
        int slash = folder.lastIndexOf('/');
        if (!folder.endsWith("/")) {
            folder = slash >= 0 ? folder.substring(0, slash + 1) : "/";
        }
        return folder.startsWith("/") ? folder : "/" + folder;
    }

    /** A path without a trailing "index.html" and with a trailing slash dropped (except for the folder itself). */
    static String normalize(String path) {
        String result = path == null || path.isEmpty() ? "/" : path;
        if (result.endsWith("/index.html")) {
            result = result.substring(0, result.length() - "index.html".length());
        }
        if (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /** The first folder under the site's folder ("buildings" for "/wiki/buildings/builder"), or empty for a page at the top. */
    static String categoryOf(String path, String prefix) {
        String relative = path.startsWith(prefix) ? path.substring(prefix.length()) : path;
        relative = relative.replaceAll("^/+|/+$", "");
        int slash = relative.indexOf('/');
        return slash > 0 ? relative.substring(0, slash) : "";
    }

    /** "/wiki/buildings/builder" under "/wiki/" is "buildings_builder"; the folder's own page is "Home". */
    static String slugOf(String path, String prefix) {
        String relative = path.startsWith(prefix) ? path.substring(prefix.length()) : path.startsWith(prefix.substring(0, prefix.length() - 1))
                ? path.substring(prefix.length() - 1) : path;
        relative = relative.replaceAll("^/+|/+$", "");
        return relative.isEmpty() ? "Home" : relative.replace('/', '_');
    }

    /** The paths of the pages this one links to, under the same folder of the same host, one by page (no query, no anchor, no file). */
    static List<String> links(String html, URI base, String origin, String prefix) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = LINK.matcher(html);
        while (matcher.find()) {
            String href = HtmlToMarkup.decode(matcher.group(1) != null ? matcher.group(1) : matcher.group(2)).strip();
            if (href.isEmpty() || href.startsWith("#") || href.startsWith("mailto:") || href.startsWith("javascript:")) {
                continue;
            }
            try {
                URI target = base.resolve(href);
                if (target.getAuthority() == null || !(origin).equals(target.getScheme() + "://" + target.getAuthority())) {
                    continue;
                }
                String path = normalize(target.getPath());
                String folder = prefix.substring(0, prefix.length() - 1);
                boolean inside = path.equals(folder) || path.startsWith(prefix);
                String last = path.substring(path.lastIndexOf('/') + 1);
                if (inside && !last.contains(".") ) {
                    found.add(path.isEmpty() ? "/" : path);
                }
            } catch (IllegalArgumentException e) {
                // a link that is not an address: ignored
            }
        }
        return new ArrayList<>(found);
    }

    /**
     * The part of the page that holds its text: {@code <main>}, else {@code <article>}, else the whole body without its menus
     * ({@code <nav>}, {@code <header>}, {@code <footer>}, {@code <aside>}).
     */
    static String mainPart(String html) {
        for (String tag : List.of("main", "article", "body")) {
            Matcher open = Pattern.compile("<" + tag + "[\\s>]", Pattern.CASE_INSENSITIVE).matcher(html);
            if (open.find()) {
                int end = html.toLowerCase(Locale.ROOT).lastIndexOf("</" + tag + ">");
                String part = end > open.start() ? html.substring(open.start(), end) : html.substring(open.start());
                return tag.equals("body") ? part.replaceAll("(?is)<(nav|header|footer|aside)[\\s>].*?</\\1>", " ") : part;
            }
        }
        return html;
    }

    /** "1.20.1" becomes 12001, the number a versioned site writes ("1.21" is 12100); 0 when the version is not known. */
    static int versionCode(String gameVersion) {
        Matcher matcher = Pattern.compile("^1\\.(\\d+)(?:\\.(\\d+))?").matcher(gameVersion == null ? "" : gameVersion);
        if (!matcher.find()) {
            return 0;
        }
        return 10000 + Integer.parseInt(matcher.group(1)) * 100 + (matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2)));
    }

    private static final Pattern VERSIONED = Pattern.compile("<span data-versions=\"(\\d+)\">(.*?)</span>", Pattern.DOTALL);

    /**
     * Some sites write a text once for each game version ({@code <span data-versions="12001">...</span>}, one span per
     * version) and show one of them: only the spans of the server's version are kept (the newest version when the page has none
     * for it).
     */
    static String forVersion(String html, int code) {
        Matcher scan = VERSIONED.matcher(html);
        int newest = 0;
        boolean has = false;
        while (scan.find()) {
            int version = Integer.parseInt(scan.group(1));
            newest = Math.max(newest, version);
            has |= version == code;
        }
        int keep = has ? code : newest;
        return VERSIONED.matcher(html).replaceAll(match -> Matcher.quoteReplacement(Integer.parseInt(match.group(1)) == keep ? match.group(2) : ""));
    }

    /** The first heading of the page, else its {@code <title>} without the site's name after a bar or a dash, else the slug. */
    static String titleOf(String html, String slug) {
        Matcher h1 = H1.matcher(mainPart(html));
        if (h1.find()) {
            String text = HtmlToMarkup.decode(h1.group(1).replaceAll("<[^>]+>", " ")).replaceAll("\\s+", " ").strip();
            if (!text.isEmpty()) {
                return text;
            }
        }
        Matcher title = TITLE.matcher(html);
        if (title.find()) {
            String text = HtmlToMarkup.decode(title.group(1)).replaceAll("\\s+", " ").strip();
            text = text.split("\\s+[|\u2013\u2014-]\\s+", 2)[0].strip();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return slug.replace('_', ' ');
    }
}
