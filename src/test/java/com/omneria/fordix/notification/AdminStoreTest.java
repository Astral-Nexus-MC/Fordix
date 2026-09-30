package com.omneria.fordix.notification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminStoreTest {
    private static final long MINUTE = 60_000L;

    @Test
    void oneShotFiresOnceThenDisappears() {
        AdminStore store = new AdminStore();
        store.schedule("info", 5, "Redemarrage", "", "tous", "", 1_000, 0, "admin");
        assertTrue(store.due(999).isEmpty());
        assertEquals(1, store.due(1_000).size());
        assertTrue(store.due(2_000).isEmpty());
        assertTrue(store.schedules.isEmpty());
    }

    @Test
    void repeatingMovesToTheNextFutureSlotAndSkipsMissedOnes() {
        AdminStore store = new AdminStore();
        AdminStore.Scheduled entry = store.schedule("info", 5, "Vote", "", "tous", "", 0, 10 * MINUTE, "admin");
        assertEquals(1, store.due(25 * MINUTE).size());
        assertEquals(30 * MINUTE, entry.nextAt);
        assertTrue(store.due(29 * MINUTE).isEmpty());
        assertEquals(1, store.due(30 * MINUTE).size());
        assertEquals(40 * MINUTE, entry.nextAt);
        assertEquals(1, store.schedules.size());
    }

    @Test
    void idsKeepGrowingAndDeleteWorks() {
        AdminStore store = new AdminStore();
        int first = store.schedule("info", 5, "a", "", "tous", "", 10, 0, "x").id;
        int second = store.schedule("info", 5, "b", "", "tous", "", 10, 0, "x").id;
        assertEquals(first + 1, second);
        assertTrue(store.deleteSchedule(first));
        assertFalse(store.deleteSchedule(first));
    }

    @Test
    void jsonRoundTripKeepsEverything() {
        AdminStore store = new AdminStore();
        store.schedule("warning", 8, "Titre", "Message", "equipe", "rouge", 5 * MINUTE, 0, "admin");
        store.saveTemplate("annonce", "success", 15, "Bravo", "a tous");
        AdminStore back = AdminStore.fromJson(store.toJson());
        assertEquals(1, back.schedules.size());
        assertEquals("rouge", back.schedules.get(0).value);
        assertEquals(15, back.templates.get("annonce").seconds);
        assertEquals(store.nextId, back.nextId);
    }

    @Test
    void unreadableJsonGivesAnEmptyStore() {
        assertTrue(AdminStore.fromJson("{ not json").schedules.isEmpty());
        assertTrue(AdminStore.fromJson("").templates.isEmpty());
        assertTrue(AdminStore.fromJson("null").schedules.isEmpty());
    }

    @Test
    void limitsAreEnforced() {
        AdminStore store = new AdminStore();
        for (int i = 0; i < AdminStore.MAX_SCHEDULES; i++) {
            store.schedule("info", 5, "t", "", "tous", "", 1, 0, "x");
        }
        assertNull(store.schedule("info", 5, "t", "", "tous", "", 1, 0, "x"));
        for (int i = 0; i < AdminStore.MAX_TEMPLATES; i++) {
            assertTrue(store.saveTemplate("n" + i, "info", 5, "t", ""));
        }
        assertFalse(store.saveTemplate("extra", "info", 5, "t", ""));
        assertTrue(store.saveTemplate("n0", "info", 9, "t", ""));
    }

    @Test
    void describeReadsNaturally() {
        AdminStore store = new AdminStore();
        AdminStore.Scheduled once = store.schedule("info", 5, "Redemarrage", "", "tous", "", 10 * MINUTE, 0, "a");
        AdminStore.Scheduled every = store.schedule("info", 5, "Vote", "", "equipe", "rouge", 0, 120 * MINUTE, "a");
        assertEquals("#1 dans 10 min : Redemarrage (tous)", once.describe(0));
        assertEquals("#2 toutes les 2 h : Vote (equipe rouge)", every.describe(0));
        assertEquals(List.of(once, every), store.schedules);
    }

    @Test
    void welcomeBookOverrideSurvivesJsonAndResets() {
        AdminStore store = new AdminStore();
        assertNull(store.welcomeSteps);
        assertTrue(store.setWelcomeBook(List.of("Lire | manual"), "give %player% minecraft:diamond"));
        AdminStore loaded = AdminStore.fromJson(store.toJson());
        assertEquals(List.of("Lire | manual"), loaded.welcomeSteps);
        assertEquals("give %player% minecraft:diamond", loaded.welcomeReward);
        loaded.resetWelcomeBook();
        assertNull(loaded.welcomeSteps);
        assertNull(loaded.welcomeReward);
        assertFalse(store.setWelcomeBook(java.util.Collections.nCopies(AdminStore.MAX_BOOK_STEPS + 1, "a | manual"), ""));
    }
}
