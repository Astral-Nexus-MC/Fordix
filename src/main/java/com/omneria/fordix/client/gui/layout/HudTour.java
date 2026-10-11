package com.omneria.fordix.client.gui.layout;

/** The steps of the HUD editor's guided tour and what each one points at. Pure Java, unit tested. */
public final class HudTour {
    /** What the dimmed screen leaves lit for a step. */
    public enum Focus { NONE, ELEMENT, LIST, PROPERTIES, TOOLS, PREVIEW }

    private static final Focus[] FOCUS = {
            Focus.NONE, Focus.ELEMENT, Focus.LIST, Focus.PROPERTIES, Focus.TOOLS, Focus.PREVIEW, Focus.NONE};

    public static final int STEPS = FOCUS.length;

    private HudTour() {
    }

    public static Focus focus(int step) {
        return FOCUS[Math.max(0, Math.min(STEPS - 1, step))];
    }

    /** The step after this one, or -1 when the tour is over. */
    public static int next(int step) {
        return step + 1 >= STEPS ? -1 : step + 1;
    }

    /** The step before this one; the first step stays first. */
    public static int previous(int step) {
        return Math.max(0, step - 1);
    }

    public static boolean last(int step) {
        return step >= STEPS - 1;
    }

    public static String counter(int step) {
        return (step + 1) + " / " + STEPS;
    }
}
