package com.omneria.fordix.client.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudTourTest {
    @Test
    void walksForwardThenEnds() {
        int step = 0;
        int seen = 1;
        while ((step = HudTour.next(step)) != -1) {
            seen++;
        }
        assertEquals(HudTour.STEPS, seen);
    }

    @Test
    void firstStepHasNoPrevious() {
        assertEquals(0, HudTour.previous(0));
        assertEquals(2, HudTour.previous(3));
    }

    @Test
    void onlyTheLastStepIsLast() {
        assertFalse(HudTour.last(0));
        assertTrue(HudTour.last(HudTour.STEPS - 1));
    }

    @Test
    void firstAndLastStepsHighlightNothing() {
        assertEquals(HudTour.Focus.NONE, HudTour.focus(0));
        assertEquals(HudTour.Focus.NONE, HudTour.focus(HudTour.STEPS - 1));
        assertEquals("3 / " + HudTour.STEPS, HudTour.counter(2));
    }
}
