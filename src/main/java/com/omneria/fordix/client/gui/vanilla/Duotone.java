package com.omneria.fordix.client.gui.vanilla;

/**
 * Recolors the grey pixels of a vanilla GUI texture with a theme ramp, keeping alpha and
 * leaving saturated pixels (gold advancement frames, green toggles...) untouched. Plain
 * Java on ARGB ints, unit tested; {@link TextureRecolor} applies it to real textures.
 */
public final class Duotone {
    /** Luminance of the ramp's stops, darkest to lightest: shadow, slot, button, panel, highlight. */
    private static final float[] STOPS = {0.0F, 0.30F, 0.55F, 0.78F, 1.0F};
    private static final float MAX_SATURATION = 0.25F;

    private Duotone() {
    }

    /** Recolors one ARGB pixel; {@code ramp} holds five opaque ARGB colors, darkest first. */
    public static int recolor(int argb, int[] ramp) {
        int alpha = argb >>> 24;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        if (alpha == 0 || (max - min) / 255.0F > MAX_SATURATION) {
            return argb;
        }
        float luminance = (0.299F * red + 0.587F * green + 0.114F * blue) / 255.0F;
        int segment = 0;
        while (segment < STOPS.length - 2 && luminance > STOPS[segment + 1]) {
            segment++;
        }
        float t = Math.max(0.0F, Math.min(1.0F, (luminance - STOPS[segment]) / (STOPS[segment + 1] - STOPS[segment])));
        int from = ramp[segment];
        int to = ramp[segment + 1];
        return alpha << 24 | mix(from, to, t, 16) << 16 | mix(from, to, t, 8) << 8 | mix(from, to, t, 0);
    }

    private static int mix(int from, int to, float t, int shift) {
        int a = (from >> shift) & 0xFF;
        int b = (to >> shift) & 0xFF;
        return Math.round(a + (b - a) * t);
    }
}
