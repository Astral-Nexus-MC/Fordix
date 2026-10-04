package com.omneria.fordix.client.hud;

/**
 * Decides when the HUD goes to its combat layout and back: in as soon as a hostile creature is near, out after it has
 * been gone for a few checks in a row. Pure Java, unit tested.
 */
public final class CombatSwitch {
    public enum Change { NONE, ENTER, LEAVE }

    private final int calmChecksToLeave;
    private boolean fighting;
    private int calm;

    public CombatSwitch(int calmChecksToLeave) {
        this.calmChecksToLeave = calmChecksToLeave;
    }

    public Change update(boolean hostileNear) {
        if (hostileNear) {
            calm = 0;
            if (!fighting) {
                fighting = true;
                return Change.ENTER;
            }
            return Change.NONE;
        }
        if (fighting && ++calm >= calmChecksToLeave) {
            fighting = false;
            calm = 0;
            return Change.LEAVE;
        }
        return Change.NONE;
    }

    public boolean fighting() {
        return fighting;
    }
}
