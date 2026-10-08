package com.omneria.fordix.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.omneria.fordix.client.hud.HudContext.Kind;
import com.omneria.fordix.client.hud.HudContext.Signals;
import org.junit.jupiter.api.Test;

class HudContextTest {
    private static final Signals NONE = new Signals(false, false, false, false);

    @Test
    void fightLingersThenReturnsToExploration() {
        HudContext context = new HudContext();
        assertEquals(Kind.COMBAT, context.update(1000, new Signals(false, false, true, false)));
        assertEquals(Kind.COMBAT, context.update(8000, NONE));
        assertEquals(Kind.EXPLORE, context.update(9100, NONE));
    }

    @Test
    void bossBeatsEverythingAndEntersAtOnce() {
        HudContext context = new HudContext();
        context.update(0, new Signals(false, false, false, true));
        assertEquals(Kind.BOSS, context.update(500, new Signals(true, true, false, true)));
    }

    @Test
    void calmChangesWaitForTheMinimumDelay() {
        HudContext context = new HudContext();
        assertEquals(Kind.MINING, context.update(10_000, new Signals(false, false, false, true)));
        // Mounting right after: mounted wins over mining, but not before 3 s have passed.
        assertEquals(Kind.MINING, context.update(11_000, new Signals(false, true, false, false)));
        assertEquals(Kind.MOUNT, context.update(13_100, new Signals(false, true, false, false)));
    }

    @Test
    void factorsDimThePanelsAndLeaveOthersAlone() {
        assertEquals(0.30F, HudContext.factor(Kind.COMBAT, "coordinates"), 0);
        assertEquals(0.0F, HudContext.factor(Kind.BOSS, "clock"), 0);
        assertEquals(1.0F, HudContext.factor(Kind.COMBAT, "gear"), 0);
    }

    @Test
    void theTableOverridesTheDefaultsAndCycles() {
        HudContextMatrix table = new HudContextMatrix(null);
        assertEquals(0.30F, table.factor(Kind.COMBAT, "coordinates"), 0);
        table.cycle(Kind.COMBAT, "coordinates");
        assertEquals(0.15F, table.factor(Kind.COMBAT, "coordinates"), 0);
        table.cycle(Kind.COMBAT, "coordinates");
        assertEquals(0.0F, table.factor(Kind.COMBAT, "coordinates"), 0);
        table.cycle(Kind.COMBAT, "coordinates");
        assertEquals(1.0F, table.factor(Kind.COMBAT, "coordinates"), 0);
        assertEquals(1.0F, table.factor(Kind.EXPLORE, "coordinates"), 0);
        table.reset();
        assertEquals(0.30F, table.factor(Kind.COMBAT, "coordinates"), 0);
    }

    @Test
    void profilesCycleThroughTheSlotsAndNone() {
        HudContextMatrix table = new HudContextMatrix(null);
        assertEquals(-1, table.profile(Kind.COMBAT));
        table.cycleProfile(Kind.COMBAT, 2);
        assertEquals(0, table.profile(Kind.COMBAT));
        table.cycleProfile(Kind.COMBAT, 2);
        assertEquals(1, table.profile(Kind.COMBAT));
        table.cycleProfile(Kind.COMBAT, 2);
        assertEquals(-1, table.profile(Kind.COMBAT));
    }
}
