package com.omneria.fordix.client.gui.anim;

/**
 * Time-based animation curves. Pure math on elapsed milliseconds - no Minecraft
 * dependency - so animations are unit tested without a render loop.
 */
public final class Easing {

    private Easing() {
    }

    /** Linear progress from 0 at {@code elapsedMs = 0} to 1 at {@code durationMs}, clamped. */
    public static float progress(long elapsedMs, long durationMs) {
        if (durationMs <= 0 || elapsedMs >= durationMs) {
            return 1.0F;
        }
        if (elapsedMs <= 0) {
            return 0.0F;
        }
        return (float) elapsedMs / durationMs;
    }

    /** Cubic ease-out: fast start, gentle landing. Takes and returns a 0..1 progress. */
    public static float easeOutCubic(float t) {
        float inverse = 1.0F - t;
        return 1.0F - inverse * inverse * inverse;
    }

    /** Linear interpolation between {@code from} and {@code to}. */
    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /** Scales the alpha byte of an ARGB color by {@code factor} (0..1). */
    public static int withAlpha(int argb, float factor) {
        int alpha = (argb >>> 24) & 0xFF;
        int scaled = Math.round(alpha * Math.max(0.0F, Math.min(1.0F, factor)));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }
}
