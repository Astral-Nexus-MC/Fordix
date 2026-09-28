package com.omneria.fordix.client.gui.anim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasingTest {

    @Test
    void progressIsClampedToTheDuration() {
        assertEquals(0.0F, Easing.progress(-5, 100));
        assertEquals(0.5F, Easing.progress(50, 100));
        assertEquals(1.0F, Easing.progress(250, 100));
    }

    @Test
    void zeroDurationIsImmediatelyDone() {
        assertEquals(1.0F, Easing.progress(0, 0));
    }

    @Test
    void easeOutCubicKeepsItsEndpointsAndRunsAheadOfLinear() {
        assertEquals(0.0F, Easing.easeOutCubic(0.0F));
        assertEquals(1.0F, Easing.easeOutCubic(1.0F));
        assertTrue(Easing.easeOutCubic(0.5F) > 0.5F);
    }

    @Test
    void lerpInterpolatesBetweenBounds() {
        assertEquals(0.92F, Easing.lerp(0.92F, 1.0F, 0.0F));
        assertEquals(1.0F, Easing.lerp(0.92F, 1.0F, 1.0F));
    }

    @Test
    void withAlphaScalesOnlyTheAlphaByte() {
        assertEquals(0x803FD1C7, Easing.withAlpha(0xFF3FD1C7, 128 / 255.0F));
        assertEquals(0x003FD1C7, Easing.withAlpha(0xFF3FD1C7, 0.0F));
        assertEquals(0xFF3FD1C7, Easing.withAlpha(0xFF3FD1C7, 2.0F));
    }
}
