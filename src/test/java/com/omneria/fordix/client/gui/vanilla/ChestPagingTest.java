package com.omneria.fordix.client.gui.vanilla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChestPagingTest {
    @Test
    void everythingFitsOnATallScreen() {
        // 9 rows of 22 px, 56 above the grid, 178 under it: 432 px plus 16 of margin.
        assertEquals(9, ChestPaging.wideRows(500, 8, 9, 22, 56, 178, 22));
    }

    @Test
    void aShorterScreenCutsTheRowsIntoPages() {
        // 400 px: 384 - 234 = 150 px of grid, minus the 22 px strip: 5 rows per page.
        assertEquals(5, ChestPaging.wideRows(400, 8, 9, 22, 56, 178, 22));
    }

    @Test
    void aVerySmallScreenLeavesTheWideLayout() {
        assertEquals(0, ChestPaging.wideRows(300, 8, 9, 22, 56, 178, 22));
    }

    @Test
    void theCompactPanelFitsOrLosesRows() {
        assertEquals(9, ChestPaging.compactRows(400, 8, 276, 9, 18, 20));
        // 240 px screen: 224 of room, the panel is 276 + 20 for the strip: 72 missing = 4 rows cut.
        assertEquals(5, ChestPaging.compactRows(240, 8, 276, 9, 18, 20));
        // Never fewer than one row, never as many as the container has.
        assertEquals(1, ChestPaging.compactRows(100, 8, 276, 9, 18, 20));
    }

    @Test
    void pagesRoundUp() {
        assertEquals(2, ChestPaging.pages(9, 5));
        assertEquals(1, ChestPaging.pages(5, 5));
        assertEquals(3, ChestPaging.pages(9, 4));
    }
}
