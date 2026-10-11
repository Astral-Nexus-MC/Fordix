package com.omneria.fordix.client.gui.layout;

/** The arithmetic of a setting slider: a mouse position to a value on a step, and back. Pure Java, unit tested. */
public final class SliderMath {
    private SliderMath() {
    }

    /** Where {@code value} sits between {@code min} and {@code max}, 0..1. */
    public static double fraction(double value, double min, double max) {
        if (max <= min) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    /** The value at {@code fraction} (0..1) of the way, snapped to the nearest {@code step} counted from {@code min}. */
    public static double valueAt(double fraction, double min, double max, double step) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        double raw = min + clamped * (max - min);
        if (step <= 0.0) {
            return raw;
        }
        double snapped = min + Math.round((raw - min) / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    /** The fraction of a track starting at {@code left} and {@code width} wide that a mouse at {@code mouseX} points at. */
    public static double fractionAt(double mouseX, int left, int width) {
        return width <= 0 ? 0.0 : (mouseX - left) / width;
    }

    /** One keyboard step to the left ({@code -1}) or right ({@code 1}), kept inside the range. */
    public static double nudge(double value, int direction, double min, double max, double step) {
        double moved = value + direction * (step > 0.0 ? step : (max - min) / 20.0);
        return Math.max(min, Math.min(max, moved));
    }
}
