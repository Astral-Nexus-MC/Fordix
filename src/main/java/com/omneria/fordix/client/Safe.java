package com.omneria.fordix.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Runs one of Fordix's event handlers so that a failure in it cannot crash the game: the first failures are logged with their stack,
 * and a handler that keeps failing is switched off for the rest of the session (the game then draws that part itself). A handler
 * passes a method reference, which is not allocated for every call.
 */
public final class Safe {
    /** Failures a handler is allowed before it is switched off. */
    static final int LIMIT = 3;

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, Integer> FAILURES = new ConcurrentHashMap<>();

    private Safe() {
    }

    public static <E> void run(String name, Consumer<? super E> handler, E event) {
        Integer failed = FAILURES.get(name);
        if (failed != null && failed >= LIMIT) {
            return;
        }
        try {
            handler.accept(event);
        } catch (RuntimeException | LinkageError e) {
            int count = fail(name);
            if (count >= LIMIT) {
                LOG.error("Fordix: {} failed {} times, it is switched off until the game restarts", name, count, e);
            } else {
                LOG.error("Fordix: {} failed ({}/{})", name, count, LIMIT, e);
            }
        }
    }

    /** Counts a failure of a handler and returns how many it has had. */
    static int fail(String name) {
        return FAILURES.merge(name, 1, Integer::sum);
    }

    static boolean off(String name) {
        return FAILURES.getOrDefault(name, 0) >= LIMIT;
    }

    /** Gives every handler its chance back (the tests, and a new world). */
    public static void reset() {
        FAILURES.clear();
    }
}
