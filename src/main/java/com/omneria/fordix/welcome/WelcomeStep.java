package com.omneria.fordix.welcome;

import com.omneria.fordix.client.guide.GuideEntry;

import java.util.regex.Pattern;

/**
 * One step of the welcome book's checklist, written in the server config as
 * {@code "label | condition"} or {@code "label | condition | action"}. Conditions:
 * <ul>
 *     <li>{@code advancement:<id>} - ticked when the player has that advancement;</li>
 *     <li>{@code command:<text>} - ticked when the player runs a command starting with it (no slash);</li>
 *     <li>{@code guides} - ticked when the player opens the Guides screen;</li>
 *     <li>{@code manual} - the player ticks it himself.</li>
 * </ul>
 * The optional action is a guide action ({@code command:/team join}, {@code url:https://...}) shown as a button.
 * Pure Java, unit tested.
 */
public record WelcomeStep(String key, String label, Kind kind, String argument, String action) {
    public enum Kind {
        ADVANCEMENT,
        COMMAND,
        GUIDES,
        MANUAL
    }

    private static final Pattern ADVANCEMENT_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    /** Parses a config line; null when the label or condition is missing or invalid. */
    public static WelcomeStep parse(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", 3);
        if (parts.length < 2) {
            return null;
        }
        String label = parts[0].strip();
        String condition = parts[1].strip();
        String action = parts.length > 2 ? parts[2].strip() : "";
        if (label.isEmpty() || label.length() > 80) {
            return null;
        }
        if (!action.isEmpty() && GuideEntry.parse("minecraft | x | minecraft:book | " + action) == null) {
            return null;
        }
        String key = label + "|" + condition;
        if (condition.equals("guides")) {
            return new WelcomeStep(key, label, Kind.GUIDES, "", action);
        }
        if (condition.equals("manual")) {
            return new WelcomeStep(key, label, Kind.MANUAL, "", action);
        }
        int colon = condition.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String kind = condition.substring(0, colon);
        String argument = condition.substring(colon + 1).strip();
        return switch (kind) {
            case "advancement" -> ADVANCEMENT_ID.matcher(argument).matches()
                    ? new WelcomeStep(key, label, Kind.ADVANCEMENT, argument, action) : null;
            case "command" -> {
                String command = argument.startsWith("/") ? argument.substring(1) : argument;
                yield command.isEmpty() ? null : new WelcomeStep(key, label, Kind.COMMAND, command.toLowerCase(), action);
            }
            default -> null;
        };
    }

    /** True when a command the player typed (with or without the slash) matches a COMMAND step. */
    public boolean matchesCommand(String typed) {
        if (kind != Kind.COMMAND) {
            return false;
        }
        String normalized = typed.startsWith("/") ? typed.substring(1) : typed;
        return normalized.toLowerCase().startsWith(argument);
    }
}
