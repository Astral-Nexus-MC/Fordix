package com.omneria.fordix.client.hud;

import java.util.Locale;

/** Small formatting and threshold rules of the information panels. Pure Java, unit tested. */
public final class HudFormat {
    private HudFormat() {
    }

    /** The game clock "HH:MM" for a day time in ticks: tick 0 is 06:00, a day lasts 24000 ticks. */
    public static String gameClock(long dayTime) {
        long tick = Math.floorMod(dayTime, 24000L);
        long hour = (tick / 1000L + 6L) % 24L;
        long minute = tick % 1000L * 60L / 1000L;
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    /** True from dusk to dawn (the hours monsters spawn and beds work). */
    public static boolean isNight(long dayTime) {
        long tick = Math.floorMod(dayTime, 24000L);
        return tick >= 13000L && tick < 23000L;
    }

    /** Share of durability left, 0 to 1. */
    public static float durabilityLeft(int damage, int maxDamage) {
        if (maxDamage <= 0) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (maxDamage - damage) / (float) maxDamage));
    }

    /** 0 good, 1 warning, 2 bad: what a share left (durability, health) deserves as a color. */
    public static int level(float left) {
        return left > 0.5F ? 0 : left > 0.2F ? 1 : 2;
    }

    /** "850 m" below a kilometer, "1.2 km" above. */
    public static String distance(double blocks) {
        if (blocks < 1000.0D) {
            return Math.round(blocks) + " m";
        }
        return String.format(Locale.ROOT, "%.1f km", blocks / 1000.0D);
    }

    /**
     * Which of the eight arrows (0 up, 1 up-right, 2 right... clockwise) points to a target (dx, dz) from a player
     * looking along {@code yaw} (Minecraft: 0 faces south, 90 west).
     */
    public static int arrowIndex(float yaw, double dx, double dz) {
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double diff = ((target - yaw) % 360.0D + 540.0D) % 360.0D - 180.0D;
        return Math.floorMod((int) Math.round(diff / 45.0D), 8);
    }
}
