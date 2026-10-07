package com.omneria.fordix.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WikiRootTest {
    @Test
    void folderOfAWiki() {
        assertEquals("/tfwiki", WikiImporter.wikiRoot("/tfwiki/index.php/Main_Page"));
        assertEquals("", WikiImporter.wikiRoot("/wiki/Naga"));
        assertEquals("", WikiImporter.wikiRoot("/w/Naga"));
        assertEquals("/a", WikiImporter.wikiRoot("/a/wiki/Naga"));
    }

    @Test
    void pageNameInIndexPhpPath() {
        assertEquals("Main_Page", HtmlToMarkup.pageOf("/tfwiki/index.php/Main_Page"));
        assertEquals("Naga", HtmlToMarkup.pageOf("/tfwiki/index.php?title=Naga"));
    }
}
