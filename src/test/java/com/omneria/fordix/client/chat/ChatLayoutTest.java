package com.omneria.fordix.client.chat;

import com.omneria.fordix.client.gui.layout.HudSnap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLayoutTest {
    @Test
    void partsStackFromTheBottom() {
        HudSnap.Rect input = ChatLayout.input(427, 240, 320);
        HudSnap.Rect log = ChatLayout.log(427, 240, 320);
        HudSnap.Rect tabs = ChatLayout.tabs(427, 240, 320);
        assertEquals(240 - 2, input.bottom());
        assertTrue(log.bottom() <= input.y());
        assertTrue(tabs.bottom() <= log.y());
    }

    @Test
    void blockCoversTabsToInput() {
        HudSnap.Rect block = ChatLayout.block(427, 240, 320);
        assertEquals(ChatLayout.tabs(427, 240, 320).y(), block.y());
        assertEquals(240 - 2, block.bottom());
        assertEquals(320, block.w());
    }

    @Test
    void widthStaysReadableAndOnScreen() {
        assertEquals(240, ChatLayout.width(100, 854));
        assertEquals(320, ChatLayout.width(320, 854));
        assertEquals(420, ChatLayout.width(900, 854));
        assertEquals(196, ChatLayout.width(320, 200));
    }
}
