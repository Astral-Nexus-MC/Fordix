package com.omneria.fordix.client.hud;

import java.util.Map;

/**
 * Which situation the HUD is in (exploring, fighting, mining, facing a boss, riding) and how visible each information panel
 * should be in it. Pure Java, unit tested. The situation follows what the player does: a boss and a mount last as long as they
 * are there, a fight and mining linger after the last blow (so the HUD does not flicker), and the HUD changes at most once
 * every few seconds, except when a boss or a fight starts.
 */
public final class HudContext {
    public enum Kind { EXPLORE, COMBAT, MINING, BOSS, MOUNT }

    /** What happened on the current tick. */
    public record Signals(boolean boss, boolean mounted, boolean fighting, boolean mining) {
    }

    public static final long COMBAT_HOLD_MS = 8000L;
    public static final long MINING_HOLD_MS = 6000L;
    public static final long MIN_CHANGE_MS = 3000L;

    private static final Map<String, float[]> FACTORS = Map.of(
            // order: EXPLORE, COMBAT, MINING, BOSS, MOUNT
            "coordinates", new float[]{1.0F, 0.30F, 1.0F, 0.30F, 1.0F},
            "performance", new float[]{1.0F, 0.30F, 0.50F, 0.30F, 0.60F},
            "clock", new float[]{1.0F, 0.0F, 0.60F, 0.0F, 1.0F},
            "compass", new float[]{1.0F, 0.30F, 0.60F, 0.30F, 1.0F},
            "effects", new float[]{0.80F, 1.0F, 0.80F, 1.0F, 0.80F});

    private Kind current = Kind.EXPLORE;
    private long changedAt = Long.MIN_VALUE / 2;
    private long lastFight = Long.MIN_VALUE / 2;
    private long lastMine = Long.MIN_VALUE / 2;

    public Kind update(long nowMs, Signals signals) {
        if (signals.fighting()) {
            lastFight = nowMs;
        }
        if (signals.mining()) {
            lastMine = nowMs;
        }
        Kind wanted = signals.boss() ? Kind.BOSS
                : nowMs - lastFight < COMBAT_HOLD_MS ? Kind.COMBAT
                : signals.mounted() ? Kind.MOUNT
                : nowMs - lastMine < MINING_HOLD_MS ? Kind.MINING
                : Kind.EXPLORE;
        boolean urgent = wanted == Kind.BOSS || wanted == Kind.COMBAT;
        if (wanted != current && (urgent || nowMs - changedAt >= MIN_CHANGE_MS)) {
            current = wanted;
            changedAt = nowMs;
        }
        return current;
    }

    public Kind current() {
        return current;
    }

    /** The panels the situations can fade, in the order the table screen lists them. */
    public static final java.util.List<String> PANELS = java.util.List.of("coordinates", "performance", "clock", "compass", "effects");

    /** The built-in value: how much of its normal opacity a panel keeps in this situation, 0..1; panels not listed are never dimmed. */
    public static float defaultFactor(Kind kind, String element) {
        float[] row = FACTORS.get(element);
        return row == null ? 1.0F : row[kind.ordinal()];
    }

    /** The value in use: the player's table when there is one, else the built-in value. */
    public static float factor(Kind kind, String element) {
        if (HudRules.situationsForced()) {
            // The server's table: a panel it does not list is never dimmed.
            float imposed = HudRules.situationFactor(kind.ordinal(), element);
            return imposed < 0.0F ? 1.0F : imposed;
        }
        return matrix != null ? matrix.factor(kind, element) : defaultFactor(kind, element);
    }

    private static HudContextMatrix matrix;

    public static void useMatrix(HudContextMatrix table) {
        matrix = table;
    }
}
