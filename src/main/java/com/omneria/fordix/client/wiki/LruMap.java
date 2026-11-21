package com.omneria.fordix.client.wiki;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** A map that keeps the most recently used entries only; what it drops is handed to a callback (a texture to release). Pure. */
public final class LruMap<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    private final transient BiConsumer<K, V> onEvict;

    public LruMap(int capacity) {
        this(capacity, (key, value) -> {
        });
    }

    public LruMap(int capacity, BiConsumer<K, V> onEvict) {
        super(16, 0.75F, true);
        this.capacity = capacity;
        this.onEvict = onEvict;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (size() > capacity) {
            onEvict.accept(eldest.getKey(), eldest.getValue());
            return true;
        }
        return false;
    }
}
