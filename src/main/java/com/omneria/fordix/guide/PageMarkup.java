package com.omneria.fordix.guide;

import com.omneria.fordix.client.guide.GuideEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * The little markup of server guide pages, one thing per line:
 * <ul>
 *     <li>{@code # Title} - a heading ({@code ##} and {@code ###} for smaller ones);</li>
 *     <li>{@code - text} - a bullet;</li>
 *     <li>{@code @image name | caption} - a picture from the server's {@code config/fordix/images} folder;</li>
 *     <li>{@code @link page | Label} - a link to another page of the same in-game wiki;</li>
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
        BUTTON,
        BULLET,
        IMAGE,
        LINK
    }

    /**
     * {@code action} is the action of a button, the level (1 to 3) of a heading, the file name of an image or the page
     * of a link; {@code text} is the label, the caption of an image or the text of a bullet.
     */
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
                    int level = 0;
                    while (level < line.length() && line.charAt(level) == '#') {
                        level++;
                    }
                    blocks.add(new Block(Type.HEADING, text, level <= 1 ? "" : Integer.toString(Math.min(3, level))));
                }
            } else if (line.startsWith("- ") && line.length() > 2) {
                flush(blocks, paragraph);
                blocks.add(new Block(Type.BULLET, line.substring(2).strip(), ""));
            } else if (line.startsWith("@image ")) {
                flush(blocks, paragraph);
                int bar = line.indexOf('|');
                String name = (bar < 0 ? line.substring(7) : line.substring(7, bar)).strip();
                if (isImageName(name)) {
                    blocks.add(new Block(Type.IMAGE, bar < 0 ? "" : line.substring(bar + 1).strip(), name));
                }
            } else if (line.startsWith("@link ")) {
                flush(blocks, paragraph);
                int bar = line.indexOf('|');
                String page = (bar < 0 ? line.substring(6) : line.substring(6, bar)).strip();
                String label = bar < 0 ? page : line.substring(bar + 1).strip();
                if (!page.isEmpty() && page.length() <= 120) {
                    blocks.add(new Block(Type.LINK, label.isEmpty() ? page : label, page));
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

    /** A plain file name (letters, digits, dot, dash, underscore) of a picture: nothing that could leave the images folder. */
    public static boolean isImageName(String name) {
        return name.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,100}\\.(png|jpg|jpeg|gif)") && !name.contains("..");
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
