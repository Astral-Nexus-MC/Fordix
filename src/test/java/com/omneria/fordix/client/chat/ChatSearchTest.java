package com.omneria.fordix.client.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChatSearchTest {
    @Test
    void everyWordMustAppearInAnyOrder() {
        assertTrue(ChatSearch.matches("<Dev> Rendez-vous au spawn", "SPAWN dev"));
        assertFalse(ChatSearch.matches("<Dev> Rendez-vous au spawn", "spawn nether"));
        assertTrue(ChatSearch.matches("anything", "  "));
        assertTrue(ChatSearch.matches("anything", null));
    }
}
