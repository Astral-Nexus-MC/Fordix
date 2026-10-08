package com.omneria.fordix.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.omneria.fordix.client.hud.ContextProfileSwitch.Type;
import com.omneria.fordix.client.hud.HudContext.Kind;
import org.junit.jupiter.api.Test;

class ContextProfileSwitchTest {
    private static final int[] SLOTS = {-1, 1, -1, 2, -1};

    @Test
    void loadsOnEntryAndRestoresOnExit() {
        ContextProfileSwitch sw = new ContextProfileSwitch();
        assertEquals(Type.NONE, sw.update(Kind.EXPLORE, i -> SLOTS[i], s -> true).type());
        var enter = sw.update(Kind.COMBAT, i -> SLOTS[i], s -> true);
        assertEquals(Type.LOAD, enter.type());
        assertEquals(1, enter.slot());
        assertEquals(Type.NONE, sw.update(Kind.COMBAT, i -> SLOTS[i], s -> true).type());
        assertEquals(2, sw.update(Kind.BOSS, i -> SLOTS[i], s -> true).slot());
        assertEquals(Type.RESTORE, sw.update(Kind.EXPLORE, i -> SLOTS[i], s -> true).type());
        assertEquals(Type.NONE, sw.update(Kind.MINING, i -> SLOTS[i], s -> true).type());
    }

    @Test
    void anEmptySlotIsIgnored() {
        ContextProfileSwitch sw = new ContextProfileSwitch();
        assertEquals(Type.NONE, sw.update(Kind.COMBAT, i -> SLOTS[i], s -> false).type());
    }
}
