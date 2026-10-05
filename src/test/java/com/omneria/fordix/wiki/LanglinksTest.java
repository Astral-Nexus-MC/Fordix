package com.omneria.fordix.wiki;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanglinksTest {
    @Test
    void readsTitlesAndDropsSectionAnchors() {
        Map<String, String> titles = WikiImporter.langlinksOf("""
                {"query":{"pages":[
                 {"pageid":1,"title":"Diamond Sword","langlinks":[{"lang":"fr","title":"Épée#Diamant"}]},
                 {"pageid":2,"title":"Planks","langlinks":[{"lang":"fr","title":"Planches"}]},
                 {"pageid":3,"title":"Mace"}]}}
                """);
        assertEquals("Épée", titles.get("Diamond Sword"));
        assertEquals("Planches", titles.get("Planks"));
        assertEquals(2, titles.size());
    }

    @Test
    void oddAnswersGiveNothing() {
        assertTrue(WikiImporter.langlinksOf("{}").isEmpty());
        assertTrue(WikiImporter.langlinksOf("not json").isEmpty());
    }
}
