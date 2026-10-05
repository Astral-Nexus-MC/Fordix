package com.omneria.fordix.client.gui.vanilla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SortPlanTest {
    private static SortPlan.Entry e(String id, int count, int max) {
        return new SortPlan.Entry(id, count, max, "1|" + id);
    }

    @Test
    void mergesIdenticalStacksThenPacksAndOrders() {
        List<SortPlan.Entry> slots = Arrays.asList(null, e("stone", 40, 64), e("apple", 3, 64), null, e("stone", 40, 64), e("sword", 1, 1));
        List<SortPlan.Entry> after = SortPlan.apply(slots, SortPlan.plan(slots));
        assertEquals(6, after.size());
        assertEquals("apple", after.get(0).same());
        assertEquals(3, after.get(0).count());
        assertEquals("stone", after.get(1).same());
        assertEquals(64, after.get(1).count());
        assertEquals("stone", after.get(2).same());
        assertEquals(16, after.get(2).count());
        assertEquals("sword", after.get(3).same());
        assertNull(after.get(4));
        assertNull(after.get(5));
    }

    @Test
    void doesNotMergeStacksThatDoNotStack() {
        List<SortPlan.Entry> slots = Arrays.asList(e("sword", 1, 1), e("sword", 1, 1));
        assertTrue(SortPlan.plan(slots).isEmpty());
    }

    @Test
    void itemsCountIsKeptWhateverTheOrder() {
        List<SortPlan.Entry> slots = new ArrayList<>();
        String[] ids = {"d", "b", "a", "c", "b", "a", "a"};
        for (String id : ids) {
            slots.add(e(id, 20, 64));
        }
        slots.add(null);
        List<SortPlan.Entry> after = SortPlan.apply(slots, SortPlan.plan(slots));
        int before = slots.stream().filter(x -> x != null).mapToInt(SortPlan.Entry::count).sum();
        int total = after.stream().filter(x -> x != null).mapToInt(SortPlan.Entry::count).sum();
        assertEquals(before, total);
        String last = "";
        for (SortPlan.Entry entry : after) {
            if (entry == null) {
                break;
            }
            assertTrue(entry.same().compareTo(last) >= 0);
            last = entry.same();
        }
    }
}
