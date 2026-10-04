package com.omneria.fordix.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudFormatTest {
    @Test
    void gameClockStartsAtSix() {
        assertEquals("06:00", HudFormat.gameClock(0));
        assertEquals("12:00", HudFormat.gameClock(6000));
        assertEquals("18:00", HudFormat.gameClock(12000));
        assertEquals("00:00", HudFormat.gameClock(18000));
        assertEquals("06:30", HudFormat.gameClock(24000 + 500));
    }

    @Test
    void nightRunsFromDuskToDawn() {
        assertFalse(HudFormat.isNight(6000));
        assertTrue(HudFormat.isNight(14000));
        assertFalse(HudFormat.isNight(23500));
    }

    @Test
    void durabilityShareAndColor() {
        assertEquals(0.75F, HudFormat.durabilityLeft(25, 100), 0.001F);
        assertEquals(1.0F, HudFormat.durabilityLeft(0, 0), 0.001F);
        assertEquals(0, HudFormat.level(0.9F));
        assertEquals(1, HudFormat.level(0.3F));
        assertEquals(2, HudFormat.level(0.1F));
    }

    @Test
    void distanceSwitchesToKilometers() {
        assertEquals("120 m", HudFormat.distance(120.2));
        assertEquals("1.5 km", HudFormat.distance(1500));
    }

    @Test
    void arrowsFollowTheLookingDirection() {
        // Looking south (yaw 0): a target to the south is ahead, to the west on the right, north behind, east on the left.
        assertEquals(0, HudFormat.arrowIndex(0, 0, 10));
        assertEquals(2, HudFormat.arrowIndex(0, -10, 0));
        assertEquals(4, HudFormat.arrowIndex(0, 0, -10));
        assertEquals(6, HudFormat.arrowIndex(0, 10, 0));
        assertEquals(1, HudFormat.arrowIndex(0, -10, 10));
        // Turned to the west (yaw 90) the west target is ahead.
        assertEquals(0, HudFormat.arrowIndex(90, -10, 0));
    }
}
