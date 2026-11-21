package com.omneria.fordix.wiki;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class WikiSearchCacheTest {
    @Test
    void theLowerCaseScoreIsTheScore() {
        String title = "Iron Sword";
        String body = "A sword made of IRON. Iron is strong.";
        for (String query : new String[]{"iron", "iron sword", "SWORD strong", "gold", ""}) {
            assertEquals(WikiSearch.score(title, body, query),
                    WikiSearch.scoreLower(title.toLowerCase(), body.toLowerCase(), WikiSearch.queryWords(query)), query);
        }
    }

    @Test
    void thePageKeepsItsLowerCaseTextUntilTheTextChanges() {
        WikiPage page = new WikiPage("Iron_Sword", "Iron Sword", "Made of IRON", "", 0L);
        String first = page.lowerBody();
        assertEquals("made of iron", first);
        assertSame(first, page.lowerBody());
        page.body = "Now GOLD";
        assertEquals("now gold", page.lowerBody());
        assertEquals("iron sword", page.lowerTitle());
        page.title = "Gold Sword";
        assertEquals("gold sword", page.lowerTitle());
    }
}
