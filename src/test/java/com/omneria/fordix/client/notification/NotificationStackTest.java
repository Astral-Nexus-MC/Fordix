package com.omneria.fordix.client.notification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationStackTest {

    @Test
    void slidesInThenOutOverItsLifetime() {
        NotificationStack<String> stack = new NotificationStack<>();
        stack.add("a", 30, 0, 5000);

        assertEquals(0.0F, stack.layout(0, 200).get(0).shown());
        assertEquals(1.0F, stack.layout(NotificationStack.ENTER_MS, 200).get(0).shown());
        assertEquals(1.0F, stack.layout(4000, 200).get(0).shown());
        float leaving = stack.layout(5000 + NotificationStack.EXIT_MS / 2, 200).get(0).shown();
        assertTrue(leaving > 0.0F && leaving < 1.0F);
        assertTrue(stack.layout(5000 + NotificationStack.EXIT_MS, 200).isEmpty());
    }

    @Test
    void newestSitsAtTheBottomAndPushesOlderOnesUp() {
        NotificationStack<String> stack = new NotificationStack<>();
        stack.add("old", 30, 0, 5000);
        stack.add("new", 20, 10, 5000);

        List<NotificationStack.Placed<String>> placed = stack.layout(20, 200);

        assertEquals("new", placed.get(0).payload());
        assertEquals(180.0F, placed.get(0).y());
        assertEquals("old", placed.get(1).payload());
        assertEquals(180.0F - NotificationStack.GAP - 30, placed.get(1).y());
    }

    @Test
    void neighboursEaseDownWhenOneLeaves() {
        NotificationStack<String> stack = new NotificationStack<>();
        stack.add("old", 30, 0, 10000);
        stack.add("new", 20, 0, 1000);
        float before = stack.layout(500, 200).get(1).y();

        // "new" has fully left; "old" moves toward the bottom but not all the way in one short frame.
        List<NotificationStack.Placed<String>> placed = stack.layout(1000 + NotificationStack.EXIT_MS, 200);
        assertEquals(1, placed.size());
        float after = placed.get(0).y();
        assertTrue(after > before && after < 170.0F);

        // Given time, it settles on its resting place.
        float settled = stack.layout(5000, 200).get(0).y();
        assertEquals(170.0F, settled, 0.01F);
    }

    @Test
    void remainingTimeRunsDownToZero() {
        NotificationStack<String> stack = new NotificationStack<>();
        stack.add("a", 30, 0, 4000);

        assertEquals(1.0F, stack.layout(0, 200).get(0).remaining());
        assertEquals(0.5F, stack.layout(2000, 200).get(0).remaining(), 0.001F);
    }

    @Test
    void overflowSendsTheOldestAway() {
        NotificationStack<Integer> stack = new NotificationStack<>();
        for (int i = 0; i <= NotificationStack.MAX_VISIBLE; i++) {
            stack.add(i, 20, 0, 10000);
        }

        stack.layout(0, 200);
        List<NotificationStack.Placed<Integer>> placed = stack.layout(NotificationStack.EXIT_MS, 200);

        assertEquals(NotificationStack.MAX_VISIBLE, placed.size());
        assertTrue(placed.stream().noneMatch(p -> p.payload() == 0));
    }
}
