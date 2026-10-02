package com.omneria.fordix.client.gui.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudSlotTest {
    @Test
    void untouchedSlotLeavesTheOverlayAlone() {
        assertFalse(new HudSlot().customized());
    }

    @Test
    void defaultSpotFollowsTheWindow() {
        HudSlot slot = new HudSlot();
        slot.setBase(new HudSnap.Rect(0, 200, 50, 20), 400, 220); // flush left, flush bottom
        assertEquals(new HudSnap.Rect(0, 300, 50, 20), slot.base(400, 320));
        assertEquals(new HudSnap.Rect(0, 200, 50, 20), slot.base(800, 220));
    }

    @Test
    void offsetFromDefaultMovesByTheOffset() {
        HudSlot slot = new HudSlot();
        slot.setBase(new HudSnap.Rect(10, 10, 40, 40), 300, 200);
        slot.offsetX = 25;
        slot.offsetY = -5;
        assertTrue(slot.customized());
        assertArrayEquals(new int[]{25, -5}, slot.delta(300, 200));
    }

    @Test
    void anchoredSlotKeepsItsCornerWhenTheWindowGrows() {
        HudSlot slot = new HudSlot();
        slot.setBase(new HudSnap.Rect(0, 0, 40, 40), 300, 200);
        slot.anchor = "bottom_right";
        assertArrayEquals(new int[]{260, 160}, slot.delta(300, 200));
        assertArrayEquals(new int[]{560, 360}, slot.delta(600, 400));
    }

    @Test
    void resetClearsThePlacementButKeepsTheMeasure() {
        HudSlot slot = new HudSlot();
        slot.setBase(new HudSnap.Rect(5, 5, 20, 20), 100, 100);
        slot.anchor = "center";
        slot.hidden = true;
        slot.reset();
        assertFalse(slot.customized());
        assertTrue(slot.measured);
    }

    @Test
    void sizeCountsAsACustomizationAndIsKeptInRange() {
        HudSlot slot = new HudSlot();
        slot.scale = 1.5F;
        assertTrue(slot.customized());
        slot.scale = 9F;
        assertEquals(3.0F, slot.scaleValue());
        slot.scale = 0F; // an older file without the field
        assertEquals(1.0F, slot.scaleValue());
        assertFalse(slot.customized());
    }

    @Test
    void biggerElementKeepsItsCornerAndStaysOnScreen() {
        HudSlot slot = new HudSlot();
        slot.setBase(new HudSnap.Rect(100, 100, 40, 20), 400, 300);
        slot.scale = 2F;
        assertEquals(new HudSnap.Rect(100, 100, 80, 40), slot.placed(slot.base(400, 300), 400, 300));
        slot.anchor = "bottom_right";
        assertEquals(new HudSnap.Rect(320, 260, 80, 40), slot.placed(slot.base(400, 300), 400, 300));
    }

    @Test
    void resetBringsTheSizeBack() {
        HudSlot slot = new HudSlot();
        slot.scale = 2F;
        slot.reset();
        assertEquals(1.0F, slot.scaleValue());
    }
}
