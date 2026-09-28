package com.omneria.fordix.client.notification;

import com.omneria.fordix.client.gui.anim.Easing;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout and timing of stacked notifications, independent of rendering: each one
 * slides in, stays for its duration, then slides out; the newest sits against the
 * anchor edge and older ones are pushed away from it (down from a top anchor, up
 * from a bottom one), easing into place when a neighbour leaves. Past
 * {@link #MAX_VISIBLE}, the oldest are sent out early. Driven by explicit timestamps,
 * so it's unit tested without a game loop.
 */
public final class NotificationStack<T> {
    public static final long ENTER_MS = 250L;
    public static final long EXIT_MS = 250L;
    public static final int GAP = 4;
    public static final int MAX_VISIBLE = 5;
    /** Time constant of the vertical easing: the gap to the target shrinks by ~63% every this many ms. */
    private static final float SETTLE_MS = 90.0F;

    private final List<Entry<T>> entries = new ArrayList<>();
    private long lastLayoutAt = -1L;

    /** A notification positioned for this frame. */
    public record Placed<T>(T payload, int height, float shown, float y, float remaining) {
    }

    private static final class Entry<T> {
        final T payload;
        final int height;
        final long createdAt;
        long leaveAt;
        float y = Float.NaN;

        Entry(T payload, int height, long createdAt, long durationMs) {
            this.payload = payload;
            this.height = height;
            this.createdAt = createdAt;
            this.leaveAt = createdAt + durationMs;
        }

        long lifetime() {
            return leaveAt - createdAt;
        }
    }

    public void add(T payload, int height, long now, long durationMs) {
        entries.add(new Entry<>(payload, height, now, Math.max(durationMs, ENTER_MS)));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Which screen edge the stack grows from. */
    public enum Anchor {
        TOP,
        BOTTOM
    }

    /**
     * Advances to {@code now} and returns every notification still on screen, newest
     * first, with {@code y} the top edge of each (y grows downward). The newest one
     * touches {@code edge}: its top edge for {@link Anchor#TOP}, its bottom edge for
     * {@link Anchor#BOTTOM}.
     */
    public List<Placed<T>> layout(long now, float edge, Anchor anchor) {
        entries.removeIf(entry -> now >= entry.leaveAt + EXIT_MS);
        sendOverflowAway(now);

        float settle = lastLayoutAt < 0 ? 1.0F : 1.0F - (float) Math.exp(-(now - lastLayoutAt) / SETTLE_MS);
        lastLayoutAt = now;

        List<Placed<T>> placed = new ArrayList<>(entries.size());
        float cursor = edge;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry<T> entry = entries.get(i);
            float target = anchor == Anchor.TOP ? cursor : cursor - entry.height;
            cursor = anchor == Anchor.TOP ? cursor + entry.height + GAP : target - GAP;
            entry.y = Float.isNaN(entry.y) ? target : Easing.lerp(entry.y, target, settle);

            float enter = Easing.easeOutCubic(Easing.progress(now - entry.createdAt, ENTER_MS));
            float exit = Easing.easeOutCubic(Easing.progress(now - entry.leaveAt, EXIT_MS));
            float shown = enter * (1.0F - exit);
            float remaining = 1.0F - Easing.progress(now - entry.createdAt, entry.lifetime());
            placed.add(new Placed<>(entry.payload, entry.height, shown, entry.y, remaining));
        }
        return placed;
    }

    /** Anything beyond the newest MAX_VISIBLE that hasn't started leaving is told to leave now. */
    private void sendOverflowAway(long now) {
        int staying = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry<T> entry = entries.get(i);
            if (now >= entry.leaveAt) {
                continue;
            }
            staying++;
            if (staying > MAX_VISIBLE) {
                entry.leaveAt = now;
            }
        }
    }

    /** Test hook: how many notifications are still tracked, including ones sliding out. */
    int size() {
        return entries.size();
    }
}
