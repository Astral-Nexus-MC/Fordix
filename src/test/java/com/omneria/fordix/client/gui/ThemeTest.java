package com.omneria.fordix.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeTest {

    @Test
    void defaultThemeIsListedFirst() {
        assertEquals(Theme.DEFAULT, Theme.NAMES.get(0));
        assertTrue(Theme.NAMES.contains("ember"));
    }

    @Test
    void unknownNameFallsBackToTheDefault() {
        assertEquals(Theme.byName(Theme.DEFAULT), Theme.byName("does-not-exist"));
    }

    @Test
    void parseHexAcceptsWithOrWithoutHashAndMakesItOpaque() {
        assertEquals(0xFF3FD1C7, Theme.parseHex("#3FD1C7"));
        assertEquals(0xFF3FD1C7, Theme.parseHex("3fd1c7"));
    }

    @Test
    void parseHexRejectsMalformedValues() {
        assertNull(Theme.parseHex(""));
        assertNull(Theme.parseHex("#FFF"));
        assertNull(Theme.parseHex("#GGGGGG"));
        assertNull(Theme.parseHex(null));
    }

    @Test
    void withAccentOnlyChangesTheAccent() {
        Theme base = Theme.byName("verdant");
        Theme recolored = base.withAccent(0xFF123456);
        assertEquals(0xFF123456, recolored.accent());
        assertEquals(base.panelFill(), recolored.panelFill());
        assertEquals(base.text(), recolored.text());
    }
}
