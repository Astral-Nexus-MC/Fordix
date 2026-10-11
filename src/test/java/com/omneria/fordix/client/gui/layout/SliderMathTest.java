package com.omneria.fordix.client.gui.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SliderMathTest {

    @Test
    void theFractionIsWhereTheValueSitsInTheRange() {
        assertEquals(0.5, SliderMath.fraction(17, 2, 32), 1e-9);
        assertEquals(0.0, SliderMath.fraction(0, 2, 32), 0.0);
        assertEquals(1.0, SliderMath.fraction(99, 2, 32), 0.0);
        assertEquals(0.0, SliderMath.fraction(5, 5, 5), 0.0);
    }

    @Test
    void aValueSnapsToTheNearestStepCountedFromTheMinimum() {
        assertEquals(170.0, SliderMath.valueAt(0.62, 10, 260, 10), 1e-9);
        assertEquals(10.0, SliderMath.valueAt(-1, 10, 260, 10), 0.0);
        assertEquals(260.0, SliderMath.valueAt(2, 10, 260, 10), 0.0);
        assertEquals(13.0, SliderMath.valueAt(0.37, 2, 32, 1), 1e-9);
    }

    @Test
    void aSmoothSliderKeepsTheRawValue() {
        assertEquals(0.25, SliderMath.valueAt(0.25, 0, 1, 0), 1e-9);
    }

    @Test
    void theMouseMapsOntoTheTrack() {
        assertEquals(0.25, SliderMath.fractionAt(125, 100, 100), 1e-9);
        assertEquals(0.0, SliderMath.fractionAt(5, 5, 0), 0.0);
    }

    @Test
    void theKeyboardMovesOneStepAndStopsAtTheEnds() {
        assertEquals(11.0, SliderMath.nudge(10, 1, 2, 32, 1), 1e-9);
        assertEquals(2.0, SliderMath.nudge(2, -1, 2, 32, 1), 0.0);
        assertEquals(32.0, SliderMath.nudge(32, 1, 2, 32, 1), 0.0);
        assertEquals(0.55, SliderMath.nudge(0.5, 1, 0, 1, 0), 1e-9);
    }
}
