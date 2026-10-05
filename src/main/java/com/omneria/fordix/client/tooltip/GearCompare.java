package com.omneria.fordix.client.tooltip;

import java.util.List;

/**
 * One verdict on an item against what the player wears or holds, from the attribute differences the tooltip already
 * computes: better when it gains somewhere and loses nowhere that matters, worse for the opposite, mixed when it does both.
 * Pure Java, unit tested.
 */
public final class GearCompare {
    public enum Verdict { BETTER, WORSE, MIXED, EQUAL }

    /** One attribute: its difference and the scale it is judged on (what a full bar means). */
    public record Diff(double difference, double scale) {
    }

    private static final double SIGNIFICANT = 0.04;

    private GearCompare() {
    }

    public static Verdict verdict(List<Diff> diffs) {
        double up = 0.0;
        double down = 0.0;
        for (Diff diff : diffs) {
            double share = diff.scale() > 0 ? diff.difference() / diff.scale() : diff.difference();
            if (share > 0) {
                up += share;
            } else {
                down -= share;
            }
        }
        boolean gains = up >= SIGNIFICANT;
        boolean losses = down >= SIGNIFICANT;
        if (gains && losses) {
            return Verdict.MIXED;
        }
        return gains ? Verdict.BETTER : losses ? Verdict.WORSE : Verdict.EQUAL;
    }
}
