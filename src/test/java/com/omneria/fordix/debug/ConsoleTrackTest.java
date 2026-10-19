package com.omneria.fordix.debug;

import com.omneria.fordix.console.ConsoleTrack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleTrackTest {
    @Test
    void positiveLevelLightsTheCellsAboveZero() {
        assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0}, ConsoleTrack.cells(2, 5));
    }

    @Test
    void negativeLevelLightsTheCellsBelowZero() {
        assertArrayEquals(new int[] {0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0}, ConsoleTrack.cells(-2, 5));
    }

    @Test
    void zeroLightsNothingAndLevelsAreClamped() {
        assertArrayEquals(new int[11], ConsoleTrack.cells(0, 5));
        assertEquals(5, ConsoleTrack.index(0, 5));
        assertEquals(10, ConsoleTrack.index(9, 5));
        assertEquals(0, ConsoleTrack.index(-9, 5));
    }

    @Test
    void signedShowsThePlusSign() {
        assertEquals("+3", ConsoleTrack.signed(3));
        assertEquals("-1", ConsoleTrack.signed(-1));
        assertEquals("0", ConsoleTrack.signed(0));
    }
}
