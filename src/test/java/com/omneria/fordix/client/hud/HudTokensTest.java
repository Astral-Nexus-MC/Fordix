package com.omneria.fordix.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudTokensTest {
    @Test
    void pulseIsAOneHertzWave() {
        assertEquals(0.0F, HudTokens.pulse(0), 1e-4);
        assertEquals(1.0F, HudTokens.pulse(500), 1e-4);
        assertEquals(0.0F, HudTokens.pulse(1000), 1e-4);
    }

    @Test
    void glintRestsMostOfTheCycle() {
        assertEquals(-1.0F, HudTokens.glint(1000), 0);
        assertTrue(HudTokens.glint(2800) > 0.0F && HudTokens.glint(2800) < 1.0F);
        assertEquals(-1.0F, HudTokens.glint(3200), 0);
    }

    @Test
    void thresholds() {
        assertTrue(HudTokens.isLow(6, 20));
        assertFalse(HudTokens.isLow(7, 20));
        assertTrue(HudTokens.isCritical(3, 20));
        assertFalse(HudTokens.isCritical(4, 20));
        assertFalse(HudTokens.isCritical(0, 0));
    }

    @Test
    void mixAndAlpha() {
        assertEquals(0xFF808080, HudTokens.mix(0xFF000000, 0xFFFFFFFF, 0.5F));
        assertEquals(0x80123456, HudTokens.withAlpha(0xFF123456, 0x80));
        assertEquals(0xFF0000FF, HudTokens.mix(0xFF0000FF, 0xFFFF0000, 0.0F));
    }

    @Test
    void toastFadesInHoldsAndFadesOut() {
        assertEquals(0.0F, HudTokens.toastAlpha(0), 1e-4);
        assertEquals(0.5F, HudTokens.toastAlpha(100), 1e-4);
        assertEquals(1.0F, HudTokens.toastAlpha(1000), 1e-4);
        assertEquals(0.5F, HudTokens.toastAlpha(200 + 2200 + 150), 1e-4);
        assertEquals(0.0F, HudTokens.toastAlpha(2700), 1e-4);
        assertEquals(0.0F, HudTokens.toastAlpha(-5), 1e-4);
    }
}
