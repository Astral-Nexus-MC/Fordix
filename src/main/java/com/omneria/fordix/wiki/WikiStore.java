package com.omneria.fordix.wiki;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The server's in-game wikis, one folder per wiki under {@code config/fordix/wiki}: {@code wiki.json} (title, source,
 * licence) and one JSON file per page. Everything is held in memory once read; files are written as pages are imported.
 */
public final class WikiStore {
    public static final int MAX_PAGES_PER_WIKI = 6000;

    /** A page in a list: no text. */
    public record Entry(String slug, String title, String category) {
        public Entry(String slug, String title) {
            this(slug, title, "");
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path root;
    private final Map<String, WikiInfo> infos = new LinkedHashMap<>();
    private final Map<String, Map<String, WikiPage>> pages = new LinkedHashMap<>();
    private boolean loaded;

    public WikiStore(Path root) {
        this.root = root;
    }

    private synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> dirs = Files.list(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).sorted().toList()) {
                String id = dir.getFileName().toString();
                if (!WikiInfo.validId(id)) {
                    continue;
                }
                Path infoFile = dir.resolve("wiki.json");
                WikiInfo info = new WikiInfo(id, id, "", "");
                if (Files.isRegularFile(infoFile)) {
                    WikiInfo read = GSON.fromJson(Files.readString(infoFile, StandardCharsets.UTF_8), WikiInfo.class);
                    if (read != null) {
                        info = read;
                        info.id = id;
                    }
                }
                infos.put(id, info);
                Map<String, WikiPage> map = new LinkedHashMap<>();
                try (Stream<Path> files = Files.list(dir)) {
                    for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".page.json")).sorted().toList()) {
                        try {
                            WikiPage page = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), WikiPage.class);
                            if (page != null && !page.slug.isEmpty()) {
                                map.put(page.slug, page);
                            }
                        } catch (RuntimeException e) {
                            // an unreadable page file is skipped
                        }
                    }
                }
                pages.put(id, map);
            }
        } catch (IOException e) {
            // no wiki readable
        }
    }

    public synchronized List<WikiInfo> wikis() {
        load();
        return new ArrayList<>(infos.values());
    }

    public synchronized WikiInfo info(String wiki) {
        load();
        return infos.get(wiki);
    }

    public synchronized int count(String wiki) {
        load();
        return pages.getOrDefault(wiki, Map.of()).size();
    }

    /** Creates the wiki when it doesn't exist yet. */
    public synchronized WikiInfo ensure(String wiki, String title, String source, String license) {
        load();
        WikiInfo info = infos.get(wiki);
        if (info == null) {
            info = new WikiInfo(wiki, title, source, license);
            infos.put(wiki, info);
            pages.put(wiki, new LinkedHashMap<>());
            writeInfo(info);
        }
        return info;
    }

    /** Writes a wiki's details again after a change (its API address, its licence). */
    public synchronized void update(WikiInfo info) {
        load();
        if (infos.containsKey(info.id)) {
            infos.put(info.id, info);
            writeInfo(info);
        }
    }

    public synchronized void setLicense(String wiki, String license) {
        load();
        WikiInfo info = infos.get(wiki);
        if (info != null) {
            info.license = license;
            writeInfo(info);
        }
    }

    public synchronized List<Entry> index(String wiki) {
        load();
        List<Entry> entries = new ArrayList<>();
        for (WikiPage page : pages.getOrDefault(wiki, Map.of()).values()) {
            if (page.redirectTo == null || page.redirectTo.isEmpty()) {
                entries.add(new Entry(page.slug, page.title, page.category == null ? "" : page.category));
            }
        }
        entries.sort(Comparator.comparing(entry -> entry.title().toLowerCase(java.util.Locale.ROOT)));
        return entries;
    }

    public synchronized WikiPage page(String wiki, String slug) {
        load();
        return pages.getOrDefault(wiki, Map.of()).get(slug);
    }

    /** The page under this name, following redirects (an old name of a page leads to the page now called otherwise). */
    public synchronized WikiPage resolve(String wiki, String slug) {
        WikiPage page = page(wiki, slug);
        for (int hop = 0; hop < 4 && page != null && page.redirectTo != null && !page.redirectTo.isEmpty(); hop++) {
            page = page(wiki, page.redirectTo);
        }
        return page != null && page.redirectTo != null && !page.redirectTo.isEmpty() ? null : page;
    }

    /** Stores a page; false when the wiki is full or unknown. */
    public synchronized boolean save(String wiki, WikiPage page) {
        load();
        Map<String, WikiPage> map = pages.get(wiki);
        if (map == null || (!map.containsKey(page.slug) && map.size() >= MAX_PAGES_PER_WIKI)) {
            return false;
        }
        if (page.body.length() > WikiPage.MAX_BODY) {
            page.body = page.body.substring(0, WikiPage.MAX_BODY);
        }
        map.put(page.slug, page);
        try {
            Path dir = root.resolve(wiki);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(fileName(page.slug)), GSON.toJson(page), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return true;
        }
        return true;
    }

    public synchronized boolean remove(String wiki, String slug) {
        load();
        Map<String, WikiPage> map = pages.get(wiki);
        if (map == null || map.remove(slug) == null) {
            return false;
        }
        try {
            Files.deleteIfExists(root.resolve(wiki).resolve(fileName(slug)));
        } catch (IOException e) {
            // the page is gone from memory; the file goes at the next cleanup
        }
        return true;
    }

    public synchronized boolean removeWiki(String wiki) {
        load();
        if (infos.remove(wiki) == null) {
            return false;
        }
        pages.remove(wiki);
        Path dir = root.resolve(wiki);
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            // best effort
        }
        return true;
    }

    /** Pages of one wiki (or of all when {@code wiki} is empty) matching the query, best first. */
    public synchronized List<WikiSearch.Hit> search(String wiki, String query, int limit) {
        load();
        List<WikiSearch.Hit> hits = new ArrayList<>();
        for (Map.Entry<String, Map<String, WikiPage>> entry : pages.entrySet()) {
            if (!wiki.isEmpty() && !entry.getKey().equals(wiki)) {
                continue;
            }
            for (WikiPage page : entry.getValue().values()) {
                if (page.redirectTo != null && !page.redirectTo.isEmpty()) {
                    continue;
                }
                int score = WikiSearch.score(page.title, page.body, query);
                if (score > 0) {
                    hits.add(new WikiSearch.Hit(entry.getKey(), page.slug, page.title, WikiSearch.snippet(page.body, query), score));
                }
            }
        }
        return WikiSearch.sorted(hits, limit);
    }

    private void writeInfo(WikiInfo info) {
        try {
            Path dir = root.resolve(info.id);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("wiki.json"), GSON.toJson(info), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // kept in memory
        }
    }

    /** A safe file name for a slug: letters, digits and a few signs, plus a hash so two slugs never share a file. */
    static String fileName(String slug) {
        String safe = slug.replaceAll("[^A-Za-z0-9_()-]", "_");
        return (safe.length() > 80 ? safe.substring(0, 80) : safe) + "-" + Integer.toHexString(slug.hashCode()) + ".page.json";
    }
}
