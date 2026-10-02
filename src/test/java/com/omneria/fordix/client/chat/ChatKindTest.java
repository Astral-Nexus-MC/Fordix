package com.omneria.fordix.client.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatKindTest {
    @Test
    void translatableKeysDecideTheKind() {
        assertEquals(ChatKind.PLAYER, ChatKind.classify("chat.type.text", "<Ana> salut"));
        assertEquals(ChatKind.PLAYER, ChatKind.classify("chat.type.team.text", "x"));
        assertEquals(ChatKind.SERVER, ChatKind.classify("chat.type.announcement", "[Server] hi"));
        assertEquals(ChatKind.COMMAND, ChatKind.classify("commands.gamemode.success.self", "Set own game mode"));
        assertEquals(ChatKind.SYSTEM, ChatKind.classify("chat.type.advancement.task", "Dev has made the advancement"));
        assertEquals(ChatKind.SYSTEM, ChatKind.classify("multiplayer.player.joined", "Dev joined the game"));
    }

    @Test
    void plainLinesAreClassifiedByTheirLook() {
        assertEquals(ChatKind.PLAYER, ChatKind.classify(null, "<Ana> on se retrouve ?"));
        assertEquals(ChatKind.SERVER, ChatKind.classify(null, "[Serveur] Redemarrage dans 10 minutes"));
        assertEquals(ChatKind.SYSTEM, ChatKind.classify(null, "JourneyMap: Press [J]"));
    }

    @Test
    void plainPlayerLinesAreSplit() {
        assertArrayEquals(new String[]{"Ana", "salut <3"}, ChatKind.splitPlain("<Ana> salut <3"));
        assertNull(ChatKind.splitPlain("pas un joueur"));
    }

    @Test
    void tabsListTheirKinds() {
        assertTrue(ChatTab.ALL.matches(ChatKind.COMMAND));
        assertTrue(ChatTab.PLAYERS.matches(ChatKind.PLAYER));
        assertFalse(ChatTab.PLAYERS.matches(ChatKind.SERVER));
        assertTrue(ChatTab.SYSTEM.matches(ChatKind.COMMAND));
        assertTrue(ChatTab.SYSTEM.matches(ChatKind.SYSTEM));
        assertFalse(ChatTab.SERVER.matches(ChatKind.SYSTEM));
    }
}
