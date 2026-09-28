package com.omneria.fordix.client.gui.vanilla;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrowShapeTest {

    @Test
    void middleRowRunsFromShaftStartToTip() {
        List<ArrowShape.Span> spans = ArrowShape.spans(22, 15, 22);

        ArrowShape.Span middle = spanAt(spans, 7);
        assertEquals(0, middle.x());
        assertEquals(22, middle.width());
    }

    @Test
    void outerRowsBelongToTheHeadOnly() {
        List<ArrowShape.Span> spans = ArrowShape.spans(22, 15, 22);

        int headStart = 22 - 8;
        assertEquals(headStart, spanAt(spans, 0).x());
        assertEquals(headStart, spanAt(spans, 14).x());
    }

    @Test
    void headNarrowsTowardTheTopAndBottom() {
        List<ArrowShape.Span> spans = ArrowShape.spans(22, 15, 22);

        assertTrue(spanAt(spans, 0).width() < spanAt(spans, 4).width());
        assertEquals(spanAt(spans, 0).width(), spanAt(spans, 14).width());
    }

    @Test
    void progressClipsEveryRowFromTheLeft() {
        List<ArrowShape.Span> spans = ArrowShape.spans(22, 15, 5);

        for (ArrowShape.Span span : spans) {
            assertTrue(span.x() + span.width() <= 5);
        }
        assertEquals(5, spanAt(spans, 7).width());
    }

    @Test
    void zeroProgressDrawsNothing() {
        assertTrue(ArrowShape.spans(22, 15, 0).isEmpty());
    }

    private static ArrowShape.Span spanAt(List<ArrowShape.Span> spans, int row) {
        return spans.stream().filter(span -> span.y() == row).findFirst().orElseThrow();
    }
}
