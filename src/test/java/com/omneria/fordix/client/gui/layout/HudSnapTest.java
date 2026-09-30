package com.omneria.fordix.client.gui.layout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HudSnapTest {
    @Test
    void snapsCenterToScreenCenter() {
        HudSnap.Rect moving = new HudSnap.Rect(120, 100, 100, 10); // center 170
        HudSnap.Result result = HudSnap.snap(moving, List.of(), 340, 240, 4);
        assertEquals(0, result.dx());
        assertEquals(170, result.guideX());
        HudSnap.Rect near = new HudSnap.Rect(122, 100, 100, 10); // center 172, 2 px off
        result = HudSnap.snap(near, List.of(), 340, 240, 4);
        assertEquals(-2, result.dx());
        assertEquals(170, result.guideX());
    }

    @Test
    void doesNotSnapBeyondThreshold() {
        HudSnap.Result result = HudSnap.snap(new HudSnap.Rect(50, 50, 20, 10), List.of(), 340, 240, 4);
        assertEquals(0, result.dx());
        assertEquals(0, result.dy());
        assertNull(result.guideX());
        assertNull(result.guideY());
    }

    @Test
    void snapsToOtherElementsEdges() {
        HudSnap.Rect other = new HudSnap.Rect(20, 200, 80, 9);
        HudSnap.Rect moving = new HudSnap.Rect(24, 192, 50, 7); // left 4 off, bottom 199 -> 200 is 1 off
        HudSnap.Result result = HudSnap.snap(moving, List.of(other), 340, 240, 4);
        assertEquals(1, result.dy());
        assertEquals(200, result.guideY());
        assertEquals(-4, result.dx());
        assertEquals(20, result.guideX());
    }

    @Test
    void placeKeepsRectOnScreen() {
        HudSnap.Rect base = HudBounds.defaults("health", 400, 240);
        HudSnap.Rect placed = HudBounds.place(base, 1000, 1000, 400, 240);
        assertEquals(400 - 81, placed.x());
        assertEquals(240 - 9, placed.y());
        placed = HudBounds.place(base, -1000, -1000, 400, 240);
        assertEquals(0, placed.x());
        assertEquals(0, placed.y());
    }
}
