package com.omneria.fordix.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeTest {
    @BeforeEach
    void clean() {
        Safe.reset();
    }

    @Test
    void aFailureDoesNotEscape() {
        Safe.run("a", e -> {
            throw new IllegalStateException("boom");
        }, "event");
        assertFalse(Safe.off("a"));
    }

    @Test
    void aHandlerThatKeepsFailingIsSwitchedOff() {
        AtomicInteger calls = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            Safe.run("b", e -> {
                calls.incrementAndGet();
                throw new IllegalStateException("boom");
            }, "event");
        }
        assertEquals(Safe.LIMIT, calls.get());
        assertTrue(Safe.off("b"));
    }

    @Test
    void aHealthyHandlerRunsEveryTime() {
        AtomicInteger calls = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            Safe.run("c", e -> calls.incrementAndGet(), "event");
        }
        assertEquals(10, calls.get());
    }

    @Test
    void handlersAreSwitchedOffOneByOne() {
        for (int i = 0; i < Safe.LIMIT; i++) {
            Safe.run("d", e -> {
                throw new IllegalStateException("boom");
            }, "event");
        }
        AtomicInteger calls = new AtomicInteger();
        Safe.run("e", e -> calls.incrementAndGet(), "event");
        assertEquals(1, calls.get());
    }
}
