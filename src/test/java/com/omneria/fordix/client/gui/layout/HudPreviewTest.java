package com.omneria.fordix.client.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HudPreviewTest {
    @Test
    void sizesStartAtTheGameMinimum() {
        assertEquals(320, HudPreview.SIZES[0][0]);
        assertEquals(240, HudPreview.SIZES[0][1]);
    }

    @Test
    void stepWrapsBothWays() {
        assertEquals(1, HudPreview.step(0, 1));
        assertEquals(HudPreview.SIZES.length - 1, HudPreview.step(0, -1));
        assertEquals(0, HudPreview.step(HudPreview.SIZES.length - 1, 1));
    }

    @Test
    void scaleFitsTheTighterSide() {
        assertEquals(0.5F, HudPreview.scale(480, 400, 960, 540), 0.0001F);
        assertEquals(0.0F, HudPreview.scale(0, 100, 320, 240), 0.0F);
    }

    @Test
    void outsideSeesEveryEdge() {
        assertFalse(HudPreview.outside(new HudSnap.Rect(0, 0, 100, 50), 320, 240));
        assertTrue(HudPreview.outside(new HudSnap.Rect(250, 10, 100, 50), 320, 240));
        assertTrue(HudPreview.outside(new HudSnap.Rect(-1, 10, 100, 50), 320, 240));
        assertEquals(List.of(1), HudPreview.outsideAll(List.of(new HudSnap.Rect(0, 0, 10, 10), new HudSnap.Rect(315, 0, 10, 10)), 320, 240));
    }
}
