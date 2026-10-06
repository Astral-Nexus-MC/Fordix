package com.omneria.fordix.client.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WheelGeometryTest {
    @Test
    void hubIsNoSector() {
        assertEquals(-1, WheelGeometry.sectorAt(3, 3, 7, 40));
    }

    @Test
    void topIsSectorZeroAndItGoesClockwise() {
        assertEquals(0, WheelGeometry.sectorAt(0, -100, 8, 40));
        assertEquals(2, WheelGeometry.sectorAt(100, 0, 8, 40));
        assertEquals(4, WheelGeometry.sectorAt(0, 100, 8, 40));
        assertEquals(6, WheelGeometry.sectorAt(-100, 0, 8, 40));
    }

    @Test
    void justLeftOfTopWrapsToTheLastSector() {
        assertEquals(0, WheelGeometry.sectorAt(-1, -100, 7, 40));
        assertEquals(6, WheelGeometry.sectorAt(-60, -100, 7, 40));
    }

    @Test
    void middleOfSectors() {
        assertEquals(0.0, WheelGeometry.middleOf(0, 4));
        assertEquals(Math.PI, WheelGeometry.middleOf(2, 4), 1e-9);
    }
}
