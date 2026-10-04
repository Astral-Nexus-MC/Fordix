package com.omneria.fordix.client.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChatCoordsTest {
    @Test
    void findsPlainAndLabeledCoordinates() {
        List<ChatCoords.Found> plain = ChatCoords.find("rdv a 120 64 -300 ok ?");
        assertEquals(1, plain.size());
        assertEquals(new ChatCoords.Found(6, 17, 120, 64, -300), plain.get(0));
        List<ChatCoords.Found> labeled = ChatCoords.find("Base : X: 10, Y: 70, Z: 20");
        assertEquals(1, labeled.size());
        assertEquals(10, labeled.get(0).x());
        assertEquals(20, labeled.get(0).z());
    }

    @Test
    void ignoresCountsTimesAndDecimals() {
        assertTrue(ChatCoords.find("j'ai 3 4 5 items").isEmpty());
        assertTrue(ChatCoords.find("il est 12:30:45").isEmpty());
        assertTrue(ChatCoords.find("version 1.20.1").isEmpty());
    }

    @Test
    void acceptsDecimalsFromTheClipboardFormat() {
        List<ChatCoords.Found> found = ChatCoords.find("19.5 -60.0 -22.5");
        assertEquals(1, found.size());
        assertEquals(19, found.get(0).x());
        assertEquals(-22, found.get(0).z());
    }
}
