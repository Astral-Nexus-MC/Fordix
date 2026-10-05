package com.omneria.fordix.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class WikiAnswersTest {
    @Test
    void parsesTheServersAnswers() {
        List<WikiAnswers.Info> wikis = WikiAnswers.parseList("minecraft\tMinecraft Wiki\t12\thttps://minecraft.wiki\tCC BY-NC-SA 3.0\n");
        assertEquals(new WikiAnswers.Info("minecraft", "Minecraft Wiki", 12, "https://minecraft.wiki", "CC BY-NC-SA 3.0"), wikis.get(0));
        assertEquals(List.of(new WikiAnswers.Entry("Iron_Sword", "Iron Sword")), WikiAnswers.parseIndex("Iron_Sword\tIron Sword\n"));
        assertEquals("A sword", WikiAnswers.parseHits("minecraft\tIron_Sword\tIron Sword\tA sword\n").get(0).snippet());
        WikiAnswers.Page page = WikiAnswers.parsePage("Iron Sword\nhttps://x.test/Iron_Sword\nCC\n---\n# Titre\nTexte\n\nSuite");
        assertEquals("Iron Sword", page.title());
        assertEquals("CC", page.license());
        assertEquals("# Titre\nTexte\n\nSuite", page.body());
    }
}
