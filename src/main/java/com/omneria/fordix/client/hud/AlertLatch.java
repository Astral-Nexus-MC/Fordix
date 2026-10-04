package com.omneria.fordix.client.hud;

import java.util.HashSet;
import java.util.Set;

/** Fires an alert once when its condition turns true, and again only after it went false. Pure Java, unit tested. */
public final class AlertLatch {
    private final Set<String> raised = new HashSet<>();

    /** True exactly when {@code active} just became true for this key. */
    public boolean rise(String key, boolean active) {
        if (!active) {
            raised.remove(key);
            return false;
        }
        return raised.add(key);
    }

    public void clear() {
        raised.clear();
    }
}
