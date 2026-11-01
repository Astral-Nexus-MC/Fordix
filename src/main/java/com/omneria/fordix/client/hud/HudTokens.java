package com.omneria.fordix.client.hud;

/**
 * The numbers of the Voidframe 2 HUD language, without any Minecraft dependency (unit tested): the three opacity levels,
 * the motion durations and the thresholds of the vital bars.
 */
public final class HudTokens {
    /** Fade lengths in milliseconds: a state change, and a whole context change. */
    public static final long FADE_MS = 200L;
    /** Health fractions: below LOW the bar changes color, below CRITICAL it pulses and the screen warns. */
    public static final float LOW = 0.30F;
    public static final float CRITICAL = 0.15F;
    private static final long PULSE_MS = 1000L;
    private static final long GLINT_MS = 3200L;

    private HudTokens() {
    }

    /** 0..1 smooth wave at 1 Hz, the only continuous motion the HUD allows (critical state). */
    public static float pulse(long timeMs) {
        double phase = (timeMs % PULSE_MS) / (double) PULSE_MS;
        return (float) (0.5D - 0.5D * Math.cos(phase * Math.PI * 2.0D));
    }

    /** Position 0..1 of the light sweeping across a bar, or -1 while it rests (most of the cycle). */
    public static float glint(long timeMs) {
        long cycle = timeMs % GLINT_MS;
        long start = 2400L;
        return cycle < start ? -1.0F : (cycle - start) / (float) (GLINT_MS - start);
    }

    /** How much hunger a food would give now: its nutrition, up to what is missing from the 20 points of the bar (0 when full or not food). */
    public static int foodGain(int foodLevel, int nutrition) {
        return Math.max(0, Math.min(20 - Math.max(0, foodLevel), nutrition));
    }

    public static boolean isLow(float health, float max) {
        return max > 0.0F && health / max <= LOW;
    }

    public static boolean isCritical(float health, float max) {
        return max > 0.0F && health / max <= CRITICAL;
    }

    public static final long TOAST_IN_MS = 200L;
    public static final long TOAST_HOLD_MS = 2200L;
    public static final long TOAST_OUT_MS = 300L;

    /** Opacity 0..1 of a notification {@code elapsedMs} after it appeared: fades in, holds, fades out; 0 once over. */
    public static float toastAlpha(long elapsedMs) {
        if (elapsedMs < 0L || elapsedMs >= TOAST_IN_MS + TOAST_HOLD_MS + TOAST_OUT_MS) {
            return 0.0F;
        }
        if (elapsedMs < TOAST_IN_MS) {
            return elapsedMs / (float) TOAST_IN_MS;
        }
        long out = elapsedMs - TOAST_IN_MS - TOAST_HOLD_MS;
        return out <= 0L ? 1.0F : 1.0F - out / (float) TOAST_OUT_MS;
    }

    /** Mixes two ARGB colors, {@code t} 0..1 from {@code from} to {@code to}. */
    public static int mix(int from, int to, float t) {
        float k = Math.max(0.0F, Math.min(1.0F, t));
        int out = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int a = (from >> shift) & 0xFF;
            int b = (to >> shift) & 0xFF;
            out |= Math.round(a + (b - a) * k) << shift;
        }
        return out;
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}
