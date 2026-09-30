package com.omneria.fordix.guide;

import com.omneria.fordix.notification.AdminStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageMarkupTest {
    @Test
    void parsesHeadingsParagraphsAndButtons() {
        List<PageMarkup.Block> blocks = PageMarkup.parse("""
                # Regles
                Pas d'insultes.
                Soyez courtois.

                ## Commandes
                @button Spawn | command:/spawn
                @button Wiki | url:https://wiki.exemple.fr
                """);
        assertEquals(5, blocks.size());
        assertEquals(new PageMarkup.Block(PageMarkup.Type.HEADING, "Regles", ""), blocks.get(0));
        assertEquals(new PageMarkup.Block(PageMarkup.Type.TEXT, "Pas d'insultes. Soyez courtois.", ""), blocks.get(1));
        assertEquals("Commandes", blocks.get(2).text());
        assertEquals(new PageMarkup.Block(PageMarkup.Type.BUTTON, "Spawn", "command:/spawn"), blocks.get(3));
        assertEquals("url:https://wiki.exemple.fr", blocks.get(4).action());
    }

    @Test
    void dropsButtonsWithABadOrUnsafeAction() {
        List<PageMarkup.Block> blocks = PageMarkup.parse("@button Fichier | url:file:///etc/passwd\n@button | command:/x\n@button Rien\n@button Ok | command:/ok");
        assertEquals(1, blocks.size());
        assertEquals("Ok", blocks.get(0).text());
    }

    @Test
    void emptyBodyGivesNoBlocks() {
        assertTrue(PageMarkup.parse("").isEmpty());
        assertTrue(PageMarkup.parse("\n\n  \n").isEmpty());
    }

    @Test
    void storeKeepsPagesAndEnforcesItsLimit() {
        AdminStore store = new AdminStore();
        assertTrue(store.savePage("regles", "Regles", "# Regles", "admin", 1000));
        assertTrue(store.savePage("regles", "Regles v2", "# Regles", "admin", 2000));
        assertEquals(2000, store.pages.get("regles").updatedAt);
        AdminStore back = AdminStore.fromJson(store.toJson());
        assertEquals("Regles v2", back.pages.get("regles").title);
        for (int i = 0; i < AdminStore.MAX_PAGES; i++) {
            store.savePage("p" + i, "t", "b", "a", 1);
        }
        assertFalse(store.savePage("extra", "t", "b", "a", 1));
        assertTrue(store.deletePage("regles"));
        assertFalse(store.deletePage("regles"));
    }
}
