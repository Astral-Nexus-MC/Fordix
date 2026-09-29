package com.omneria.fordix.client.gui.vanilla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuotoneTest {
    private static final int[] RAMP = {0xFF000000, 0xFF101010, 0xFF202020, 0xFF303030, 0xFFFFFFFF};

    @Test
    void transparentPixelsStayTransparent() {
        assertEquals(0x00808080, Duotone.recolor(0x00808080, RAMP));
    }

    @Test
    void saturatedPixelsAreKept() {
        assertEquals(0xFFE0B020, Duotone.recolor(0xFFE0B020, RAMP));
    }

    @Test
    void blackAndWhiteHitTheEndsOfTheRamp() {
        assertEquals(0xFF000000, Duotone.recolor(0xFF000000, RAMP));
        assertEquals(0xFFFFFFFF, Duotone.recolor(0xFFFFFFFF, RAMP));
    }

    @Test
    void greyBackgroundBecomesThePanelColor() {
        // 0xC6 = 198 -> luminance 0.776, next to the panel stop (0.78).
        int result = Duotone.recolor(0xFFC6C6C6, RAMP);
        assertEquals(0xFF, result >>> 24);
        int channel = result & 0xFF;
        assertEquals(0x30, channel, 2);
    }

    @Test
    void alphaIsPreserved() {
        assertEquals(0x80, Duotone.recolor(0x80808080, RAMP) >>> 24);
    }
}
