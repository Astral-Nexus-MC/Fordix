package com.omneria.fordix.client.tooltip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RarityPaletteTest {

    @Test
    void vanillaTiersMapToTheScale() {
        assertEquals(RarityPalette.COMMON, RarityPalette.colorOf("COMMON"));
        assertEquals(RarityPalette.RARE, RarityPalette.colorOf("RARE"));
        assertEquals(RarityPalette.EPIC, RarityPalette.colorOf("EPIC"));
        assertEquals(RarityPalette.UNCOMMON, RarityPalette.colorOf("UNCOMMON"));
    }

    @Test
    void anyLegendaryNameIsOrange() {
        assertEquals(RarityPalette.LEGENDARY, RarityPalette.colorOf("LEGENDARY"));
        assertEquals(RarityPalette.LEGENDARY, RarityPalette.colorOf("legendary"));
        assertEquals(RarityPalette.LEGENDARY, RarityPalette.colorOf("Legendaire"));
    }

    @Test
    void unknownNamesHaveNoColor() {
        assertNull(RarityPalette.colorOf("MYTHIC"));
    }

    @Test
    void labelsAreInFrench() {
        assertEquals("Commun", RarityPalette.labelOf("COMMON"));
        assertEquals("Épique", RarityPalette.labelOf("EPIC"));
        assertEquals("Légendaire", RarityPalette.labelOf("LEGENDARY"));
        assertEquals("Mythic", RarityPalette.labelOf("MYTHIC"));
    }
}
