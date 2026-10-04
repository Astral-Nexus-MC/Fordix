package com.omneria.fordix.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpsMeterTest {
    private static final long SECOND = 1_000_000_000L;

    @Test
    void unknownBeforeTwoPackets() {
        TpsMeter meter = new TpsMeter();
        assertEquals(-1.0D, meter.tps());
        meter.onGameTime(100, 0);
        assertEquals(-1.0D, meter.tps());
    }

    @Test
    void healthyServerReadsTwenty() {
        TpsMeter meter = new TpsMeter();
        meter.onGameTime(0, 0);
        meter.onGameTime(20, SECOND);
        meter.onGameTime(40, 2 * SECOND);
        assertEquals(20.0D, meter.tps(), 0.01D);
    }

    @Test
    void laggingServerReadsLower() {
        TpsMeter meter = new TpsMeter();
        meter.onGameTime(0, 0);
        // 20 ticks took two seconds: 10 TPS
        meter.onGameTime(20, 2 * SECOND);
        assertEquals(10.0D, meter.tps(), 0.01D);
        meter.onGameTime(40, 4 * SECOND);
        assertEquals(10.0D, meter.tps(), 0.01D);
    }

    @Test
    void cappedAtTwentyAndSmoothed() {
        TpsMeter meter = new TpsMeter();
        meter.onGameTime(0, 0);
        meter.onGameTime(40, SECOND); // a burst after a hiccup: 40 TPS, capped
        assertEquals(20.0D, meter.tps(), 0.01D);
        meter.onGameTime(60, 3 * SECOND); // 10 TPS sample
        assertTrue(meter.tps() < 20.0D && meter.tps() > 10.0D);
    }

    @Test
    void resetForgetsEverything() {
        TpsMeter meter = new TpsMeter();
        meter.onGameTime(0, 0);
        meter.onGameTime(20, SECOND);
        meter.reset();
        assertEquals(-1.0D, meter.tps());
    }
}
