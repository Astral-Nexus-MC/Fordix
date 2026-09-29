package com.omneria.fordix.client.tooltip;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributeMathTest {

    @Test
    void additionsAddToTheBase() {
        assertEquals(7.0, AttributeMath.total(1.0, List.of(new AttributeMath.Mod(0, 6.0))), 1e-9);
    }

    @Test
    void multipliersApplyAfterAdditions() {
        double value = AttributeMath.total(2.0, List.of(
                new AttributeMath.Mod(0, 2.0),
                new AttributeMath.Mod(1, 0.5),
                new AttributeMath.Mod(2, 0.1)));
        assertEquals((2.0 + 2.0) * 1.5 * 1.1, value, 1e-9);
    }

    @Test
    void attackSpeedOfADiamondSword() {
        assertEquals(1.6, AttributeMath.total(4.0, List.of(new AttributeMath.Mod(0, -2.4))), 1e-9);
    }

    @Test
    void fractionIsClampedAndUsesTheScale() {
        assertEquals(0.5F, AttributeMath.fraction(8, 16), 1e-6);
        assertEquals(1.0F, AttributeMath.fraction(50, 16), 1e-6);
        assertEquals(0.5F, AttributeMath.fraction(10, 0), 1e-6);
    }

    @Test
    void numbersUseACommaAndNoTrailingZeros() {
        assertEquals("1,6", AttributeMath.number(1.6));
        assertEquals("7", AttributeMath.number(7.0));
        assertEquals("0,25", AttributeMath.number(0.25));
    }

    @Test
    void deltaIsSignedOrEqual() {
        assertEquals("+2", AttributeMath.delta(2.0));
        assertEquals("-0,4", AttributeMath.delta(-0.4));
        assertEquals("=", AttributeMath.delta(0.001));
    }
}
