package com.omneria.fordix.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class SiteImporterTest {
    @Test
    void foldersAndSlugs() {
        assertEquals("/wiki/", SiteImporter.folderOf("/wiki/"));
        assertEquals("/wiki/", SiteImporter.folderOf("/wiki/index.html"));
        assertEquals("/", SiteImporter.folderOf(""));
        assertEquals("/wiki", SiteImporter.normalize("/wiki/"));
        assertEquals("Home", SiteImporter.slugOf("/wiki", "/wiki/"));
        assertEquals("buildings_builder", SiteImporter.slugOf("/wiki/buildings/builder", "/wiki/"));
    }

    @Test
    void followsOnlyPagesUnderTheFolder() {
        String html = "<nav><a href=\"/wiki/buildings/builder/\">B</a><a href=\"workers/cook#x\">C</a><a href=\"/other/page\">O</a>"
                + "<a href=\"https://github.com/x\">G</a><a href=\"/wiki/_astro/a.png\">I</a><a href=\"#top\">T</a></nav>";
        assertEquals(List.of("/wiki/buildings/builder", "/wiki/workers/cook"),
                SiteImporter.links(html, URI.create("https://minecolonies.com/wiki/"), "https://minecolonies.com", "/wiki/"));
    }

    @Test
    void titleAndMainPart() {
        String html = "<html><head><title>Builder | MineColonies Wiki</title></head><body><nav>menu</nav><main><h1>Builder's Hut</h1><p>Text</p></main></body></html>";
        assertEquals("Builder's Hut", SiteImporter.titleOf(html, "x"));
        assertEquals("<main><h1>Builder's Hut</h1><p>Text</p>", SiteImporter.mainPart(html).replace("</main>", ""));
        assertEquals("Builder", SiteImporter.titleOf("<title>Builder | MineColonies Wiki</title><body>no heading</body>", "x"));
    }

    @Test
    void keepsOnlyTheServersVersion() {
        String html = "Build the <span data-versions=\"12101\">Hut</span><span data-versions=\"12001\">Cabin</span><span data-versions=\"11800\">Shed</span> first.";
        assertEquals(12001, SiteImporter.versionCode("1.20.1"));
        assertEquals(12100, SiteImporter.versionCode("1.21"));
        assertEquals("Build the Cabin first.", SiteImporter.forVersion(html, 12001));
        assertEquals("Build the Hut first.", SiteImporter.forVersion(html, 0));
    }

    @Test
    void bodyWithoutMenus() {
        String html = "<body><nav><a href=\"/x\">Menu</a></nav><div class=\"page-content\"><h1>Hut</h1></div><footer>f</footer></body>";
        assertEquals(false, SiteImporter.mainPart(html).contains("Menu"));
        assertEquals(true, SiteImporter.mainPart(html).contains("<h1>Hut</h1>"));
    }
}
