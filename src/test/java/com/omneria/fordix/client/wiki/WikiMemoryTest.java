package com.omneria.fordix.client.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WikiMemoryTest {
    @Test
    void favouritesAndRecentsSurviveAReload(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("memory.json");
        WikiMemory memory = new WikiMemory(file);
        assertTrue(memory.toggleFavorite("twilightforest", "Naga", "Naga"));
        memory.visited("twilightforest", "Lich", "Lich");
        memory.visited("create", "Cogwheel", "Cogwheel");
        memory.visited("twilightforest", "Lich", "Lich");
        WikiMemory again = new WikiMemory(file);
        assertTrue(again.isFavorite("twilightforest", "Naga"));
        assertEquals("Lich", again.recent().get(0).slug());
        assertEquals(2, again.recent().size());
        assertFalse(again.toggleFavorite("twilightforest", "Naga", "Naga"));
        assertTrue(Files.exists(file));
    }

    @Test
    void recentsAreBounded() {
        WikiMemory memory = new WikiMemory(null);
        for (int i = 0; i < 30; i++) {
            memory.visited("w", "p" + i, "P" + i);
        }
        assertEquals(WikiMemory.MAX_RECENT, memory.recent().size());
        assertEquals("p29", memory.recent().get(0).slug());
    }
}
