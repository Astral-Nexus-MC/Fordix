package com.omneria.fordix.pet;

/** What the wheel asks the server to do to a tamed creature (feeding and naming are the game's own clicks, done by the client). */
public enum PetAction {
    FOLLOW, STAY, INVENTORY, RECALL, RELEASE;

    public static PetAction byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : null;
    }
}
