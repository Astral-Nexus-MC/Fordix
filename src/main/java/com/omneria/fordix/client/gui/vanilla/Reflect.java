package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.Fordix;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Reads private state off vanilla screens by field type rather than by name: in a
 * production (obfuscated) game the names differ from the dev environment, but a
 * field like "the only int on StonecutterScreen" is the same field either way.
 * Only used where the type is unique on that class, and every caller has a fallback.
 */
final class Reflect {

    private Reflect() {
    }

    /** The only non-static field of {@code type} declared on {@code owner}, made accessible; null if not exactly one. */
    static Field uniqueField(Class<?> owner, Class<?> type) {
        Field found = null;
        for (Field field : owner.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType() != type) {
                continue;
            }
            if (found != null) {
                Fordix.LOGGER.warn("Fordix: more than one {} field on {}, skipping", type.getSimpleName(), owner.getSimpleName());
                return null;
            }
            found = field;
        }
        if (found == null) {
            Fordix.LOGGER.warn("Fordix: no {} field on {}", type.getSimpleName(), owner.getSimpleName());
            return null;
        }
        try {
            found.setAccessible(true);
            return found;
        } catch (RuntimeException e) {
            Fordix.LOGGER.warn("Fordix: cannot access {} field on {}", type.getSimpleName(), owner.getSimpleName(), e);
            return null;
        }
    }

    static Object get(Field field, Object instance) {
        if (field == null) {
            return null;
        }
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
