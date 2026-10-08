package com.omneria.fordix.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The outline of a long page: its headings (for a clickable table of contents) and which blocks a folded heading hides.
 * Pure Java, unit tested.
 */
public final class PageOutline {
    /** A page with at least this many headings gets a table of contents. */
    public static final int MIN_HEADINGS_FOR_CONTENTS = 4;

    private PageOutline() {
    }

    /** A heading: its place among the page's blocks, its level (1 is the biggest) and its text. */
    public record Heading(int block, int level, String text) {
    }

    public static int levelOf(PageMarkup.Block heading) {
        String action = heading.action();
        if (action == null || action.isEmpty()) {
            return 1;
        }
        try {
            return Math.max(1, Math.min(3, Integer.parseInt(action)));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static List<Heading> headings(List<PageMarkup.Block> blocks) {
        List<Heading> list = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            PageMarkup.Block block = blocks.get(i);
            if (block.type() == PageMarkup.Type.HEADING && !block.text().startsWith("§")) {
                list.add(new Heading(i, levelOf(block), block.text()));
            }
        }
        return list;
    }

    /**
     * For each block, whether a folded heading hides it: a folded heading hides what follows it up to the next heading of the
     * same or a bigger level. The folded heading itself stays visible.
     */
    public static boolean[] hidden(List<PageMarkup.Block> blocks, Set<Integer> folded) {
        boolean[] hidden = new boolean[blocks.size()];
        int foldedLevel = 0;
        for (int i = 0; i < blocks.size(); i++) {
            PageMarkup.Block block = blocks.get(i);
            if (block.type() == PageMarkup.Type.HEADING) {
                int level = levelOf(block);
                if (foldedLevel > 0 && level <= foldedLevel) {
                    foldedLevel = 0;
                }
                // A smaller heading inside a folded section goes with it; the folded heading itself stays.
                hidden[i] = foldedLevel > 0;
                if (foldedLevel == 0 && folded.contains(i)) {
                    foldedLevel = level;
                }
                continue;
            }
            hidden[i] = foldedLevel > 0;
        }
        return hidden;
    }

    /** Whether the page is long enough, in headings, to deserve a table of contents. */
    public static boolean wantsContents(List<PageMarkup.Block> blocks) {
        return headings(blocks).size() >= MIN_HEADINGS_FOR_CONTENTS;
    }
}
