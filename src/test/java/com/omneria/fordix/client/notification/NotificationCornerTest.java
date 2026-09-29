package com.omneria.fordix.client.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationCornerTest {

    @Test
    void autoAvoidsJourneyMapsMinimap() {
        assertEquals(NotificationCorner.TOP_LEFT, NotificationCorner.resolve("auto", true));
        assertEquals(NotificationCorner.TOP_RIGHT, NotificationCorner.resolve("auto", false));
    }

    @Test
    void anExplicitCornerWinsOverAuto() {
        assertEquals(NotificationCorner.TOP_RIGHT, NotificationCorner.resolve("top_right", true));
        assertEquals(NotificationCorner.BOTTOM_LEFT, NotificationCorner.resolve("bottom_left", false));
    }

    @Test
    void unknownValuesBehaveLikeAuto() {
        assertEquals(NotificationCorner.TOP_LEFT, NotificationCorner.resolve("middle", true));
        assertEquals(NotificationCorner.TOP_RIGHT, NotificationCorner.resolve(null, false));
    }

    @Test
    void everyOptionButAutoNamesACorner() {
        for (String option : NotificationCorner.OPTIONS) {
            if (!option.equals(NotificationCorner.AUTO)) {
                assertEquals(option, NotificationCorner.resolve(option, true).name().toLowerCase());
            }
        }
        assertTrue(NotificationCorner.BOTTOM_RIGHT.isRight());
        assertFalse(NotificationCorner.BOTTOM_RIGHT.isTop());
    }
}
