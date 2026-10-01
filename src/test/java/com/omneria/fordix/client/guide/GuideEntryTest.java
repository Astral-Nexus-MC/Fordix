package com.omneria.fordix.client.guide;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuideEntryTest {

    @Test
    void parsesPonder() {
        assertEquals(new GuideEntry("create", "Create", "create:wrench", GuideEntry.Action.PONDER, ""),
                GuideEntry.parse("create | Create | create:wrench | ponder"));
    }

    @Test
    void parsesAWebLinkKeepingItsColons() {
        GuideEntry entry = GuideEntry.parse("twilightforest | Twilight Forest | minecraft:book | url:https://example.org/wiki");
        assertEquals(GuideEntry.Action.URL, entry.action());
        assertEquals("https://example.org/wiki", entry.target());
    }

    @Test
    void parsesPatchouliAndCommands() {
        assertEquals("modid:guide", GuideEntry.parse("modid | Guide | minecraft:book | patchouli:modid:guide").target());
        GuideEntry command = GuideEntry.parse("minecraft | Aide | minecraft:book | command:/help");
        assertEquals(GuideEntry.Action.COMMAND, command.action());
        assertEquals("help", command.target());
    }

    @Test
    void rejectsNonWebLinks() {
        assertNull(GuideEntry.parse("x | X | minecraft:book | url:file:///etc/passwd"));
        assertNull(GuideEntry.parse("x | X | minecraft:book | url:javascript:alert(1)"));
    }

    @Test
    void rejectsIncompleteOrUnknownLines() {
        assertNull(GuideEntry.parse("create | Create | create:wrench"));
        assertNull(GuideEntry.parse("create |  | create:wrench | ponder"));
        assertNull(GuideEntry.parse("create | Create | create:wrench | teleport:spawn"));
        assertNull(GuideEntry.parse("create | Create | create:wrench | url:"));
        assertNull(GuideEntry.parse(null));
    }

    @Test
    void wikiLinkNamesTheItemPage() {
        GuideEntry entry = GuideEntry.parse("minecraft | Wiki | minecraft:book | url:https://minecraft.wiki/w/{objet}");
        assertEquals("https://minecraft.wiki/w/Iron_Sword", GuideEntry.resolveUrl(entry.target(), "iron_sword"));
        assertEquals("https://minecraft.wiki/w/Minecraft_Wiki", GuideEntry.resolveUrl(entry.target(), null));
        assertEquals("Oak_Log", GuideEntry.wikiPage("oak_log"));
        assertEquals("https://exemple.fr/", GuideEntry.resolveUrl("https://exemple.fr/", "iron_sword"));
    }
}
