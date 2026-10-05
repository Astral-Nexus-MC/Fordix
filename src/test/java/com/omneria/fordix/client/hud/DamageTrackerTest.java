package com.omneria.fordix.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DamageTrackerTest {
    @Test
    void reportsTheChangeFromOneTickToTheNext() {
        DamageTracker tracker = new DamageTracker();
        assertEquals(0.0F, tracker.update(1, 20.0F), 0.001F);
        assertEquals(-6.0F, tracker.update(1, 14.0F), 0.001F);
        assertEquals(0.0F, tracker.update(1, 14.0F), 0.001F);
        assertEquals(2.0F, tracker.update(1, 16.0F), 0.001F);
    }

    @Test
    void forgetsCreaturesThatLeft() {
        DamageTracker tracker = new DamageTracker();
        tracker.update(1, 10.0F);
        tracker.update(2, 10.0F);
        tracker.retain(Set.of(2));
        assertEquals(0.0F, tracker.update(1, 4.0F), 0.001F);
        assertEquals(-3.0F, tracker.update(2, 7.0F), 0.001F);
    }

    @Test
    void formatsNumbers() {
        assertEquals("5", DamageTracker.format(-5.0F));
        assertEquals("2.5", DamageTracker.format(-2.5F));
        assertEquals("12", DamageTracker.format(12.4F));
        assertEquals("3", DamageTracker.format(2.98F));
    }
}
