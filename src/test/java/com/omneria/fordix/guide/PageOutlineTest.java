package com.omneria.fordix.guide;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PageOutlineTest {
    private static final List<PageMarkup.Block> BLOCKS = PageMarkup.parse(
            "Intro\n\n# Drops\nTexte A\n\n## Rare\nTexte B\n\n## Commun\nTexte C\n\n# Combat\nTexte D\n\n## Phase 1\nTexte E");

    @Test
    void listsTheHeadingsWithTheirLevel() {
        List<PageOutline.Heading> headings = PageOutline.headings(BLOCKS);
        assertEquals(List.of("Drops", "Rare", "Commun", "Combat", "Phase 1"), headings.stream().map(PageOutline.Heading::text).toList());
        assertEquals(List.of(1, 2, 2, 1, 2), headings.stream().map(PageOutline.Heading::level).toList());
        assertTrue(PageOutline.wantsContents(BLOCKS));
        assertFalse(PageOutline.wantsContents(PageMarkup.parse("# A\ntexte\n# B\ntexte")));
    }

    @Test
    void aFoldedHeadingHidesItsSectionOnly() {
        // blocks: 0 Intro, 1 Drops, 2 A, 3 Rare, 4 B, 5 Commun, 6 C, 7 Combat, 8 D, 9 Phase 1, 10 E
        boolean[] folded = PageOutline.hidden(BLOCKS, Set.of(1));
        assertArrayEquals(new boolean[]{false, false, true, true, true, true, true, false, false, false, false}, folded);
        boolean[] sub = PageOutline.hidden(BLOCKS, Set.of(3));
        assertArrayEquals(new boolean[]{false, false, false, false, true, false, false, false, false, false, false}, sub);
    }
}
