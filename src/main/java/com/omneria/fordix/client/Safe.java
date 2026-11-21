package com.omneria.fordix.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Runs one of Fordix's event handlers so that a failure in it cannot crash the game: the first failures are logged with their stack,
 * and a handler that keeps failing is switched off for the rest of the session (the game then draws that part itself). A handler
 * passes a method reference, which is not allocated for every call.
 */
public final class Safe {
    /** Failures a handler is allowed before it is switched off. */
    static final int LIMIT = 3;

    /** Where failures are written: the mod's logger once the game is up (a plain stream in the tests, which have no game). */
    public static volatile BiConsumer<String, Throwable> reporter = (message, error) -> System.err.println(message + ": " + error);

    private static final Map<String, Integer> FAILURES = new ConcurrentHashMap<>();

    private Safe() {
    }

    public static <E> void run(String name, Consumer<? super E> handler, E event) {
        if (off(name)) {
            return;
        }
        try {
            handler.accept(event);
        } catch (RuntimeException | LinkageError e) {
            int count = FAILURES.merge(name, 1, Integer::sum);
            String tail = count >= LIMIT ? " times, it is switched off until the game restarts" : " time(s)";
            reporter.accept("Fordix: " + name + " failed " + count + tail, e);
        }
    }

    static boolean off(String name) {
        return FAILURES.getOrDefault(name, 0) >= LIMIT;
    }

    /** Gives every handler its chance back (the tests). */
    public static void reset() {
        FAILURES.clear();
    }
}
