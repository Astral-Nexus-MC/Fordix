package com.omneria.fordix.client.gui.vanilla;

/**
 * The look and the movement of the rarity backgrounds, as numbers: the tint of a slot by rank, the passing of the shine,
 * the breathing, the sparkles and the turning frame of the legendary. No Minecraft here (the drawing is in
 * {@link RarityFrames}), so it is tested on its own. Ranks: 0 common, 1 uncommon, 2 rare, 3 epic, 4 legendary.
 */
public final class RarityMotion {
    public static final int EPIC = 3;
    public static final int LEGENDARY = 4;
    private static final double EPIC_PERIOD = 5.0D;
    private static final double LEGENDARY_PERIOD = 2.6D;
    private static final double BREATH_PERIOD = 2.4D;
    private static final double FRAME_PERIOD = 3.2D;
    private static final double TWINKLE_PERIOD = 1.8D;
    /** The shine only passes in the last part of its cycle, the rest of the time the slot is still. */
    private static final double SWEEP_START = 0.64D;

    private RarityMotion() {
    }

    /** Each slot starts its cycles at its own moment, so a bag of items does not blink as one block. */
    public static double delayOf(int slotIndex) {
        return (slotIndex * 0.37D) % 3.0D;
    }

    /** Whether the rank has a shine passing over the slot now and then. */
    public static boolean hasSweep(int rank) {
        return rank >= EPIC;
    }

    /** How far the shine is across the slot (0 to 1), or -1 when it is not passing. */
    public static double sweep(int rank, double seconds, double delay) {
        if (!hasSweep(rank)) {
            return -1.0D;
        }
        double period = rank >= LEGENDARY ? LEGENDARY_PERIOD : EPIC_PERIOD;
        double phase = (((seconds + delay) % period) + period) % period / period;
        return phase < SWEEP_START ? -1.0D : (phase - SWEEP_START) / (1.0D - SWEEP_START);
    }

    /** The shine of a pointer passing over the slot: one sweep over {@code 0.7} seconds, then nothing. */
    public static double hoverSweep(double secondsSinceEnter) {
        return secondsSinceEnter < 0 || secondsSinceEnter > 0.7D ? -1.0D : secondsSinceEnter / 0.7D;
    }

    /** How much the legendary's glow from the bottom shows (0 to 0.95). */
    public static double breath(double seconds, double delay) {
        return (0.5D - 0.5D * Math.cos(2 * Math.PI * (seconds + delay) / BREATH_PERIOD)) * 0.95D;
    }

    /** Where the bright arcs of the legendary's frame are, as a turn (0 to 1). */
    public static double frameTurn(double seconds, double delay) {
        return (((seconds + delay) % FRAME_PERIOD) + FRAME_PERIOD) % FRAME_PERIOD / FRAME_PERIOD;
    }

    /** The brightness (0 to 1) of sparkle number {@code index}: on, dim, on again, off. */
    public static double twinkle(double seconds, double delay, int index) {
        double p = ((((seconds + delay + index * 0.45D) % TWINKLE_PERIOD) + TWINKLE_PERIOD) % TWINKLE_PERIOD) / TWINKLE_PERIOD;
        if (p < 0.2D) {
            return p / 0.2D;
        }
        if (p < 0.45D) {
            return 1.0D - (p - 0.2D) / 0.25D * 0.8D;
        }
        if (p < 0.7D) {
            return 0.2D + (p - 0.45D) / 0.25D * 0.8D;
        }
        return 1.0D - (p - 0.7D) / 0.3D;
    }

    /** The white flash of an item that just arrived: 0.55 at once, gone after 0.55 seconds. */
    public static double flash(double secondsSinceArrival) {
        if (secondsSinceArrival < 0 || secondsSinceArrival > 0.55D) {
            return 0.0D;
        }
        return 0.55D * (1.0D - secondsSinceArrival / 0.55D);
    }

    /** The tint of the slot's background, top and bottom, as ARGB: the share of the rank's color rises with the rank. */
    public static int[] tint(int rank, int color) {
        if (rank <= 0) {
            return new int[]{0, 0};
        }
        if (rank >= LEGENDARY) {
            return new int[]{mix(0xFF1A0F04, color, 0.24F), mix(0xFF3A1D02, color, 0.78F)};
        }
        return new int[]{mix(0xFF05080C, color, rank * 0.04F + 0.03F), mix(0xFF0A1218, color, rank * 0.15F + 0.10F)};
    }

    /** {@code from} moved toward {@code to} by {@code t}, opaque. */
    static int mix(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 255) * (1 - t) + ((to >> 16) & 255) * t);
        int g = Math.round(((from >> 8) & 255) * (1 - t) + ((to >> 8) & 255) * t);
        int b = Math.round((from & 255) * (1 - t) + (to & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
