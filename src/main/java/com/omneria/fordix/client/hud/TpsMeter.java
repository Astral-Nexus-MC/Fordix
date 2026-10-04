package com.omneria.fordix.client.hud;

/**
 * The server's ticks per second as a client can tell: the server sends its game time every 20 ticks, so the game time
 * gained between two such packets over the real time between them is the tick rate. Smoothed, capped at 20.
 * Pure Java, unit tested.
 */
public final class TpsMeter {
    public static final double MAX_TPS = 20.0D;
    private static final double SMOOTHING = 0.65D;
    private static final double MIN_INTERVAL_SECONDS = 0.2D;

    private long lastGameTime = -1L;
    private long lastNanos;
    private double tps = -1.0D;

    /** A game time packet arrived. */
    public synchronized void onGameTime(long gameTime, long nanos) {
        if (lastGameTime >= 0 && gameTime > lastGameTime) {
            double seconds = (nanos - lastNanos) / 1.0e9D;
            if (seconds >= MIN_INTERVAL_SECONDS) {
                double sample = Math.min(MAX_TPS, (gameTime - lastGameTime) / seconds);
                tps = tps < 0 ? sample : tps * SMOOTHING + sample * (1.0D - SMOOTHING);
            }
        }
        lastGameTime = gameTime;
        lastNanos = nanos;
    }

    /** The current estimate, or -1 before two packets were seen. */
    public synchronized double tps() {
        return tps;
    }

    public synchronized void reset() {
        lastGameTime = -1L;
        tps = -1.0D;
    }
}
