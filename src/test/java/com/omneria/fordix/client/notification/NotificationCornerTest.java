package com.omneria.fordix.client.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationCornerTest {

    @Test
    void autoIsTopRight() {
        assertEquals(NotificationCorner.TOP_RIGHT, NotificationCorner.resolve("auto"));
    }

    @Test
    void anExplicitCornerWinsOverAuto() {
        assertEquals(NotificationCorner.TOP_LEFT, NotificationCorner.resolve("top_left"));
        assertEquals(NotificationCorner.BOTTOM_LEFT, NotificationCorner.resolve("bottom_left"));
    }

    @Test
    void unknownValuesBehaveLikeAuto() {
        assertEquals(NotificationCorner.TOP_RIGHT, NotificationCorner.resolve("middle"));
        assertEquals(NotificationCorner.TOP_RIGHT, NotificationCorner.resolve(null));
    }

    @Test
    void everyOptionButAutoNamesACorner() {
        for (String option : NotificationCorner.OPTIONS) {
            if (!option.equals(NotificationCorner.AUTO)) {
                assertEquals(option, NotificationCorner.resolve(option).name().toLowerCase());
            }
        }
        assertTrue(NotificationCorner.BOTTOM_RIGHT.isRight());
        assertFalse(NotificationCorner.BOTTOM_RIGHT.isTop());
    }
}
