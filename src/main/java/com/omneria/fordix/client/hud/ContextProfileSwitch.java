package com.omneria.fordix.client.hud;

import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 * Decides when the HUD loads a saved profile because the situation changed, and when it goes back to the layout the player
 * had before. Pure Java, unit tested; the caller does the loading.
 */
public final class ContextProfileSwitch {
    public enum Type { NONE, LOAD, RESTORE }

    public record Action(Type type, int slot) {
        static final Action NONE = new Action(Type.NONE, -1);
    }

    private HudContext.Kind last = HudContext.Kind.EXPLORE;
    private boolean switched;

    /**
     * @param slotOf the profile slot chosen for a situation (by ordinal), -1 for none
     * @param usable whether a slot holds a layout
     */
    public Action update(HudContext.Kind kind, IntUnaryOperator slotOf, IntPredicate usable) {
        if (kind == last) {
            return Action.NONE;
        }
        last = kind;
        int slot = slotOf.applyAsInt(kind.ordinal());
        if (slot >= 0 && usable.test(slot)) {
            switched = true;
            return new Action(Type.LOAD, slot);
        }
        if (switched) {
            switched = false;
            return new Action(Type.RESTORE, -1);
        }
        return Action.NONE;
    }

    public boolean switched() {
        return switched;
    }

    public void reset() {
        last = HudContext.Kind.EXPLORE;
        switched = false;
    }
}
