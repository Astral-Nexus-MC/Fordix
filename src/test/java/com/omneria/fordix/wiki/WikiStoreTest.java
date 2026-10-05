package com.omneria.fordix.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WikiStoreTest {
    @TempDir
    Path dir;

    @Test
    void pagesSurviveAReloadAndAreListedByTitle() {
        WikiStore store = new WikiStore(dir);
        store.ensure("twilightforest", "Twilight Forest", "https://example.org", "CC BY-SA");
        assertTrue(store.save("twilightforest", new WikiPage("Naga", "Naga", "Un grand serpent.", "https://example.org/wiki/Naga", 1)));
        assertTrue(store.save("twilightforest", new WikiPage("Lich", "Lich", "Un mage mort.", "", 2)));
        WikiStore reloaded = new WikiStore(dir);
        assertEquals(1, reloaded.wikis().size());
        assertEquals("CC BY-SA", reloaded.info("twilightforest").license);
        assertEquals(List.of(new WikiStore.Entry("Lich", "Lich"), new WikiStore.Entry("Naga", "Naga")), reloaded.index("twilightforest"));
        assertEquals("Un grand serpent.", reloaded.page("twilightforest", "Naga").body);
        assertNull(reloaded.page("twilightforest", "Inconnu"));
    }

    @Test
    void searchRanksTitlesFirstAndFiltersOnEveryWord() {
        WikiStore store = new WikiStore(dir);
        store.ensure("a", "A", "", "");
        store.ensure("b", "B", "", "");
        store.save("a", new WikiPage("Naga", "Naga", "Un serpent geant qui garde la tour.", "", 0));
        store.save("a", new WikiPage("Tour", "Tour", "La tour du naga est haute.", "", 0));
        store.save("b", new WikiPage("Autre", "Autre", "Rien a voir.", "", 0));
        List<WikiSearch.Hit> hits = store.search("", "naga", 10);
        assertEquals(2, hits.size());
        assertEquals("Naga", hits.get(0).title());
        assertTrue(hits.get(0).snippet().length() > 0);
        assertEquals(1, store.search("a", "naga serpent", 10).size());
        assertTrue(store.search("b", "naga", 10).isEmpty());
    }

    @Test
    void removingAPageOrAWiki() {
        WikiStore store = new WikiStore(dir);
        store.ensure("w", "W", "", "");
        store.save("w", new WikiPage("P", "P", "x", "", 0));
        assertTrue(store.remove("w", "P"));
        assertFalse(store.remove("w", "P"));
        assertEquals(0, new WikiStore(dir).count("w"));
        assertNotNull(store.info("w"));
        assertTrue(store.removeWiki("w"));
        assertTrue(new WikiStore(dir).wikis().isEmpty());
    }

    @Test
    void wikiIdsAreRestricted() {
        assertTrue(WikiInfo.validId("twilightforest"));
        assertFalse(WikiInfo.validId("../etc"));
        assertFalse(WikiInfo.validId("Upper"));
        assertFalse(WikiInfo.validId(""));
    }
}
