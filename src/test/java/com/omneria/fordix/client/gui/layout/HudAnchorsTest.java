package com.omneria.fordix.client.gui.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudAnchorsTest {
    private static final HudSnap.Rect BAR = new HudSnap.Rect(100, 200, 80, 10);

    @Test
    void anchoredElementFollowsTheScreenSize() {
        HudSnap.Rect small = HudAnchors.place(HudAnchors.Anchor.BOTTOM_RIGHT, -6, -6, BAR, 400, 240);
        HudSnap.Rect big = HudAnchors.place(HudAnchors.Anchor.BOTTOM_RIGHT, -6, -6, BAR, 800, 480);
        assertEquals(400 - 80 - 6, small.x());
        assertEquals(240 - 10 - 6, small.y());
        assertEquals(800 - 80 - 6, big.x());
        assertEquals(480 - 10 - 6, big.y());
    }

    @Test
    void defaultAnchorKeepsTheDefaultSpotPlusOffset() {
        HudSnap.Rect placed = HudAnchors.place(HudAnchors.Anchor.DEFAULT, 5, -3, BAR, 400, 240);
        assertEquals(105, placed.x());
        assertEquals(197, placed.y());
    }

    @Test
    void nearestPicksTheThirdHoldingTheCenter() {
        assertEquals(HudAnchors.Anchor.TOP_LEFT, HudAnchors.nearest(new HudSnap.Rect(0, 0, 20, 10), 300, 240));
        assertEquals(HudAnchors.Anchor.BOTTOM_CENTER, HudAnchors.nearest(new HudSnap.Rect(110, 220, 80, 10), 300, 240));
        assertEquals(HudAnchors.Anchor.MIDDLE_RIGHT, HudAnchors.nearest(new HudSnap.Rect(270, 110, 30, 10), 300, 240));
    }

    @Test
    void offsetForRoundTripsThroughPlace() {
        HudSnap.Rect wanted = new HudSnap.Rect(37, 90, 80, 10);
        for (HudAnchors.Anchor anchor : HudAnchors.Anchor.values()) {
            int[] offset = HudAnchors.offsetFor(anchor, wanted, BAR, 400, 240);
            HudSnap.Rect placed = HudAnchors.place(anchor, offset[0], offset[1], new HudSnap.Rect(BAR.x(), BAR.y(), 80, 10), 400, 240);
            assertEquals(wanted.x(), placed.x(), anchor.name());
            assertEquals(wanted.y(), placed.y(), anchor.name());
        }
    }

    @Test
    void presetsCoverEveryElement() {
        for (HudAnchors.Preset preset : HudAnchors.PRESETS) {
            assertEquals(HudBounds.ELEMENTS.size(), preset.entries().size(), preset.name());
        }
    }
}
