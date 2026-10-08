package com.omneria.fordix.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageCleanerTest {
    @Test
    void dropsEditingNotesAndFileCaptions() {
        String body = "@image a.png | Creeper.png: Infobox image for Minecraft entity 'Creeper'\n\n- Spawn : Light level of 0 [Java Edition only]\n\nIt explodes.[needs testing]\n\n@image b.png | A creeper near a house";
        String clean = PageCleaner.clean(body, true);
        assertFalse(clean.contains("Infobox image"));
        assertTrue(clean.contains("@image a.png"));
        assertTrue(clean.contains("A creeper near a house"));
        assertFalse(clean.contains("[Java Edition only]"));
        assertFalse(clean.contains("needs testing"));
        assertTrue(clean.contains("It explodes."));
    }

    @Test
    void mergesRowsThatOnlyDifferByTheirName() {
        StringBuilder body = new StringBuilder("# Spawning\n\n- Spawn area | Weight | Chance\n\n");
        for (String biome : List.of("Bamboo Jungle", "Grove", "Meadow", "Taiga", "Desert", "Forest", "Swamp", "Beach")) {
            body.append("- ").append(biome).append(" | 100 | 19.42%\n\n");
        }
        body.append("- Plains | 100 | 19.23%\n");
        String clean = PageCleaner.clean(body.toString(), false);
        assertTrue(clean.contains("- Bamboo Jungle, Grove, Meadow, Taiga, Desert, Forest +2 | 100 | 19.42%"), clean);
        assertTrue(clean.contains("- Plains | 100 | 19.23%"));
    }

    @Test
    void dropsATableThatRepeatsAnotherOfTheSameSection() {
        String body = "# Drops\n\n- Item | Chance\n\n- Gunpowder | 66%\n\n- Music Disc | 100%\n\n- Creeper Head | 100%\n\ntext between\n\n"
                + "- Item | Chance\n\n- Gunpowder | 2⁄3\n\n- Music Disc | 1\n\n- Creeper Head | 1\n\n- Killed : 1\n";
        String clean = PageCleaner.clean(body, false);
        assertEquals(1, clean.split("Gunpowder", -1).length - 1, clean);
        assertTrue(clean.contains("text between"));
    }

    @Test
    void dropsSoundsAndTheOtherEdition() {
        String body = "Intro.\n\n# Behavior\n\nIt walks.\n\nBedrock Edition:\n\nBedrock text.\n\nJava Edition:\n\nJava text.\n\n# Sounds\n\n- Hiss | 1\n\n# Bedrock Edition\n\nOnly there.\n\n# Trivia\n\nFun.";
        String clean = PageCleaner.clean(body, true);
        assertTrue(clean.contains("It walks."));
        assertTrue(clean.contains("Java text."));
        assertFalse(clean.contains("Bedrock text."));
        assertFalse(clean.contains("Hiss"));
        assertFalse(clean.contains("Only there."));
        assertFalse(clean.contains("Fun."));
    }

    @Test
    void cutsRowsAndLongPages() {
        StringBuilder table = new StringBuilder("# Loot\n\n- A | B\n\n");
        for (int i = 0; i < 30; i++) {
            table.append("- Item").append(i).append(" | ").append(i).append(" | x").append(i).append("\n\n");
        }
        assertTrue(PageCleaner.clean(table.toString(), false).contains("… +"));
        StringBuilder big = new StringBuilder("Intro text.\n\n");
        for (int s = 0; s < 40; s++) {
            big.append("# Section ").append(s).append("\n\n").append("x".repeat(1000)).append("\n\n");
        }
        String clean = PageCleaner.clean(big.toString(), false);
        assertTrue(clean.length() < PageCleaner.MAX_CHARS + 1100);
        assertTrue(clean.startsWith("Intro text."));
    }

    @Test
    void dropsTheOtherEditionRowsOfATable() {
        String body = "# Loot\n\n- Item | Chest | Chance\n\n- Java Edition\n\n- Sword | Village | 22%\n\n- Bedrock Edition\n\n- Sword | Village | 23%\n\n- Pickaxe | Stronghold | 12%\n";
        String clean = PageCleaner.clean(body, true);
        assertTrue(clean.contains("Sword | Village | 22%"));
        assertFalse(clean.contains("23%"));
        assertFalse(clean.contains("Pickaxe"));
    }

    @Test
    void keepsTheJavaPartOfAnInfoboxLine() {
        String clean = PageCleaner.clean("- Hitbox : In Java Edition: Height: 1.7 In Bedrock Edition: Height: 1.8", true);
        assertEquals("- Hitbox : Height: 1.7", clean);
    }
}
