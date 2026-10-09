package com.omneria.fordix.client.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class HudChecksTest {
    @Test
    void findsElementsOnTopOfEachOther() {
        List<HudSnap.Rect> rects = List.of(new HudSnap.Rect(0, 0, 100, 20), new HudSnap.Rect(50, 0, 100, 20),
                new HudSnap.Rect(300, 0, 10, 10), new HudSnap.Rect(95, 18, 10, 10));
        List<int[]> pairs = HudChecks.overlaps(rects);
        assertEquals(1, pairs.size());
        assertEquals(0, pairs.get(0)[0]);
        assertEquals(1, pairs.get(0)[1]);
    }

    @Test
    void aSmallElementInsideABigOneCounts() {
        List<int[]> pairs = HudChecks.overlaps(List.of(new HudSnap.Rect(0, 0, 200, 100), new HudSnap.Rect(10, 10, 20, 10)));
        assertEquals(1, pairs.size());
    }

    @Test
    void touchingEdgesDoNotCount() {
        assertEquals(0, HudChecks.overlaps(List.of(new HudSnap.Rect(0, 0, 50, 50), new HudSnap.Rect(50, 0, 50, 50))).size());
    }
}
