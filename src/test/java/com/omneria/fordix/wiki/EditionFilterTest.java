package com.omneria.fordix.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EditionFilterTest {
    @Test
    void keepsTheJavaPartOfAnEditionList() {
        assertEquals("6", EditionFilter.javaOnly("JE: 6 BE: 7"));
        assertEquals("250 (en dur)", EditionFilter.javaOnly("JE: 250 (en dur) BE: 251"));
        assertEquals("Texte sans edition", EditionFilter.javaOnly("Texte sans edition"));
        assertEquals("", EditionFilter.javaOnly("BE: seulement"));
        assertEquals("Golden: 32 Iron: 250", EditionFilter.javaOnlyCell("Java Edition Golden: 32 Iron: 250 Bedrock Edition Golden: 33 Iron: 251"));
        assertEquals("In Java Edition, it burns.", EditionFilter.javaOnly("In Java Edition, it burns.").replace("In Java Edition,", "In Java Edition,"));
    }

    @Test
    void javaOnlyConversionDropsBedrockSectionsAndValues() {
        String html = "<h2>Attacking</h2><table><tr><th>Damage</th><td>JE: 6 BE: 7</td></tr></table>"
                + "<h3>Bedrock Edition</h3><p>Rien pour nous.</p><h3>Java Edition</h3><p>Pour nous.</p>";
        String markup = HtmlToMarkup.convert(html, null, true).markup();
        assertTrue(markup.contains("- Damage : 6"));
        assertFalse(markup.contains("Rien pour nous"));
        assertTrue(markup.contains("Pour nous."));
        assertTrue(HtmlToMarkup.convert(html, null, false).markup().contains("Rien pour nous"));
    }

    @Test
    void readsTheRevisionAndTheCreditLine() {
        String found = "{\"query\":{\"pages\":[{\"title\":\"Iron Sword\",\"revisions\":[{\"revid\":123456,\"timestamp\":\"2023-06-20T10:00:00Z\"}]}]}}";
        assertEquals(123456L, WikiImporter.revisionId(found));
        assertNull(WikiImporter.revisionId("{\"query\":{\"pages\":[{\"title\":\"Netherite Hoe\",\"missing\":true}]}}"));
        assertNull(WikiImporter.revisionId("pas du json"));
        WikiInfo info = new WikiInfo("minecraft", "Minecraft Wiki", "https://minecraft.wiki", "CC BY-NC-SA 3.0");
        info.gameVersion = "1.20.1";
        info.asOf = "2023-06-26";
        assertEquals("CC BY-NC-SA 3.0 \u00B7 1.20.1 \u00B7 wiki 2023-06-26", info.credit());
        assertTrue(WikiInfo.validDate("2023-06-26"));
        assertFalse(WikiInfo.validDate("26/06/2023"));
    }

    @Test
    void redirectsAreRecognizedAndFollowedByTheStore(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        String html = "<div class=\"redirectMsg\"><p>Redirect to:</p><ul class=\"redirectText\"><li><a href=\"/w/Sword\" title=\"Sword\">Sword</a></li></ul></div>";
        assertEquals("Sword", WikiImporter.redirectTarget(html));
        assertNull(WikiImporter.redirectTarget("<p>Une page normale</p>"));
        WikiStore store = new WikiStore(dir);
        store.ensure("w", "W", "", "");
        store.save("w", new WikiPage("Sword", "Sword", "Les epees.", "", 0));
        WikiPage old = new WikiPage("Iron_Sword", "Iron Sword", "", "", 0);
        old.redirectTo = "Sword";
        store.save("w", old);
        assertEquals("Les epees.", store.resolve("w", "Iron_Sword").body);
        assertEquals(1, store.index("w").size());
        assertTrue(store.search("", "epees", 5).stream().allMatch(hit -> hit.slug().equals("Sword")));
    }
}
