package com.omneria.fordix.client.gui.anim;

/**
 * Frame-rate independent easing toward a moving target (smooth scrolling and panning).
 * Plain Java, unit tested.
 */
public final class Smoothing {
    private Smoothing() {
    }

    /**
     * Moves {@code current} toward {@code target} over {@code seconds}: the remaining distance
     * shrinks by the same fraction per unit of time however often this is called.
     * {@code rate} is how fast it converges (about 1/rate seconds to cover most of the way).
     */
    public static double step(double current, double target, double seconds, double rate) {
        double fraction = 1.0 - Math.exp(-Math.max(0.0, seconds) * rate);
        return current + (target - current) * fraction;
    }

    /** The range a viewport start may take so the content stays in view; content smaller than the view is centered. */
    public static double[] scrollRange(double contentStart, double contentEnd, double viewSize, double padding) {
        double low = contentStart - padding;
        double high = contentEnd + padding - viewSize;
        if (high < low) {
            double middle = (low + high) / 2.0;
            return new double[]{middle, middle};
        }
        return new double[]{low, high};
    }
}
