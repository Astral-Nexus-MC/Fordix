package com.omneria.fordix.client.gui.layout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTest {

    @Test
    void stackVerticalPlacesEachItemBelowThePrevious() {
        List<Layout.Size> sizes = List.of(
                new Layout.Size(80, 20),
                new Layout.Size(80, 20),
                new Layout.Size(80, 20)
        );

        Layout.Result result = Layout.stackVertical(10, 100, 6, sizes);

        assertEquals(List.of(
                new Layout.Position(10, 100),
                new Layout.Position(10, 126),
                new Layout.Position(10, 152)
        ), result.positions());
        assertEquals(10, result.nextX());
        assertEquals(178, result.nextY());
    }

    @Test
    void stackVerticalWithNoItemsReturnsStartingCursor() {
        Layout.Result result = Layout.stackVertical(5, 7, 6, List.of());

        assertTrue(result.positions().isEmpty());
        assertEquals(5, result.nextX());
        assertEquals(7, result.nextY());
    }

    @Test
    void stackHorizontalPlacesEachItemToTheRight() {
        List<Layout.Size> sizes = List.of(
                new Layout.Size(50, 20),
                new Layout.Size(30, 20)
        );

        Layout.Result result = Layout.stackHorizontal(0, 0, 4, sizes);

        assertEquals(List.of(
                new Layout.Position(0, 0),
                new Layout.Position(54, 0)
        ), result.positions());
        assertEquals(88, result.nextX());
        assertEquals(0, result.nextY());
    }

    @Test
    void centerReturnsHalfTheRemainingSpace() {
        assertEquals(38, Layout.center(176, 100));
    }

    @Test
    void centerRoundsDownForOddRemainders() {
        assertEquals(37, Layout.center(176, 101));
    }
}
