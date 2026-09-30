package com.omneria.fordix.guide;

import com.omneria.fordix.client.guide.GuideEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * The little markup of server guide pages, one thing per line:
 * <ul>
 *     <li>{@code # Title} - a heading;</li>
 *     <li>{@code @button Label | action} - a button; the action is a guide action:
 *     {@code command:/spawn}, {@code url:https://...} (asks before opening), {@code ponder}, {@code patchouli:book};</li>
 *     <li>anything else - text: consecutive lines form one paragraph, a blank line starts the next.</li>
 * </ul>
 * Pure Java, unit tested.
 */
public final class PageMarkup {
    private PageMarkup() {
    }

    public enum Type {
        HEADING,
        TEXT,
        BUTTON
    }

    /** {@code action} is only set for buttons. */
    public record Block(Type type, String text, String action) {
    }

    public static List<Block> parse(String body) {
        List<Block> blocks = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        for (String raw : body.split("\r?\n", -1)) {
            String line = raw.strip();
            if (line.isEmpty()) {
                flush(blocks, paragraph);
            } else if (line.startsWith("#")) {
                flush(blocks, paragraph);
                String text = line.replaceFirst("^#+\\s*", "");
                if (!text.isEmpty()) {
                    blocks.add(new Block(Type.HEADING, text, ""));
                }
            } else if (line.startsWith("@button")) {
                flush(blocks, paragraph);
                Block button = parseButton(line.substring("@button".length()));
                if (button != null) {
                    blocks.add(button);
                }
            } else {
                if (paragraph.length() > 0) {
                    paragraph.append(' ');
                }
                paragraph.append(line);
            }
        }
        flush(blocks, paragraph);
        return blocks;
    }

    private static void flush(List<Block> blocks, StringBuilder paragraph) {
        if (paragraph.length() > 0) {
            blocks.add(new Block(Type.TEXT, paragraph.toString(), ""));
            paragraph.setLength(0);
        }
    }

    /** "Label | action", or null when the label is missing or the action isn't a valid, safe guide action. */
    private static Block parseButton(String rest) {
        int bar = rest.indexOf('|');
        if (bar < 0) {
            return null;
        }
        String label = rest.substring(0, bar).strip();
        String action = rest.substring(bar + 1).strip();
        if (label.isEmpty() || GuideEntry.parse("minecraft | " + label + " | minecraft:book | " + action) == null) {
            return null;
        }
        return new Block(Type.BUTTON, label, action);
    }
}
