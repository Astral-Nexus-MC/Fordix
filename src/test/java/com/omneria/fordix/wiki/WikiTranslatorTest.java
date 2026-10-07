package com.omneria.fordix.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class WikiTranslatorTest {
    @Test
    void readsAJsonArrayEvenInACodeFence() throws IOException {
        assertEquals(List.of("Naga", "- Elle a \"deux\" vies"), WikiTranslator.parseLines("```json\n[\"Naga\", \"- Elle a \\\"deux\\\" vies\"]\n```"));
    }

    @Test
    void refusesAnythingElse() {
        assertThrows(IOException.class, () -> WikiTranslator.parseLines("Voici la traduction"));
        assertThrows(IOException.class, () -> WikiTranslator.parseLines("[1, {\"a\": 2}]"));
    }

    @Test
    void namesTheLanguage() {
        assertEquals("French", WikiTranslator.languageName("fr"));
    }
}
