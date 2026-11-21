package com.omneria.fordix.client.wiki;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LruMapTest {
    @Test
    void theOldestEntryIsDroppedAndReported() {
        List<String> dropped = new ArrayList<>();
        LruMap<String, Integer> map = new LruMap<>(2, (key, value) -> dropped.add(key));
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(List.of("a"), dropped);
        assertFalse(map.containsKey("a"));
        assertEquals(2, map.size());
    }

    @Test
    void readingAnEntryKeepsItAlive() {
        LruMap<String, Integer> map = new LruMap<>(2);
        map.put("a", 1);
        map.put("b", 2);
        map.get("a");
        map.put("c", 3);
        assertTrue(map.containsKey("a"));
        assertFalse(map.containsKey("b"));
    }
}
