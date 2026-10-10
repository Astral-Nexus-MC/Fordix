package com.omneria.fordix.client.gui.vanilla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookLayoutTest {
    @Test
    void closedBookCentersThePanelAlone() {
        int[] at = ContainerLayout.bookLayout(1000, 600, false);
        assertEquals(200, at[0]);
        assertEquals(1000, at[1]);
    }

    @Test
    void openBookSitsAgainstTheLeftEdgeOfThePanel() {
        int screen = 1000;
        int[] at = ContainerLayout.bookLayout(screen, 600, true);
        // The game puts the book at (width - 147) / 2 - 86 from the left, for the width it is given.
        int bookLeft = (at[1] - 147) / 2 - 86;
        assertEquals(at[0] - ContainerLayout.BOOK_ROOM, bookLeft);
        // Panel and book are centered together.
        int groupLeft = bookLeft;
        int groupRight = at[0] + 600;
        assertEquals(screen - groupRight, groupLeft, 1);
    }

    @Test
    void theBookNeverStartsOffScreen() {
        int[] at = ContainerLayout.bookLayout(500, 600, true);
        assertTrue((at[1] - 147) / 2 - 86 >= 4);
    }
}
