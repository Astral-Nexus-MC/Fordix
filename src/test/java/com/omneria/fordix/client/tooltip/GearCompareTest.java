package com.omneria.fordix.client.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class GearCompareTest {
    @Test
    void judgesTheDifferencesTogether() {
        assertEquals(GearCompare.Verdict.BETTER, GearCompare.verdict(List.of(new GearCompare.Diff(2, 16), new GearCompare.Diff(0, 4))));
        assertEquals(GearCompare.Verdict.WORSE, GearCompare.verdict(List.of(new GearCompare.Diff(-3, 20))));
        assertEquals(GearCompare.Verdict.MIXED, GearCompare.verdict(List.of(new GearCompare.Diff(4, 16), new GearCompare.Diff(-1, 4))));
        assertEquals(GearCompare.Verdict.EQUAL, GearCompare.verdict(List.of(new GearCompare.Diff(0.1, 16))));
        assertEquals(GearCompare.Verdict.EQUAL, GearCompare.verdict(List.of()));
    }
}
