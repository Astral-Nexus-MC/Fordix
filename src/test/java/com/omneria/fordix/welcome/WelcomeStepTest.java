package com.omneria.fordix.welcome;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WelcomeStepTest {
    @Test
    void parsesEveryCondition() {
        assertEquals(WelcomeStep.Kind.GUIDES, WelcomeStep.parse("Ouvrir les guides | guides").kind());
        assertEquals(WelcomeStep.Kind.MANUAL, WelcomeStep.parse("Lire les regles | manual").kind());
        WelcomeStep advancement = WelcomeStep.parse("Miner de la pierre | advancement:minecraft:story/mine_stone");
        assertEquals(WelcomeStep.Kind.ADVANCEMENT, advancement.kind());
        assertEquals("minecraft:story/mine_stone", advancement.argument());
        WelcomeStep command = WelcomeStep.parse("Rejoindre une equipe | command:/team join | command:/team join");
        assertEquals(WelcomeStep.Kind.COMMAND, command.kind());
        assertEquals("team join", command.argument());
        assertEquals("command:/team join", command.action());
    }

    @Test
    void rejectsBadLines() {
        assertNull(WelcomeStep.parse(null));
        assertNull(WelcomeStep.parse("pas de condition"));
        assertNull(WelcomeStep.parse(" | guides"));
        assertNull(WelcomeStep.parse("Truc | advancement:Pas Valide"));
        assertNull(WelcomeStep.parse("Truc | command:"));
        assertNull(WelcomeStep.parse("Truc | inconnu:x"));
        assertNull(WelcomeStep.parse("Truc | manual | url:file:///etc/passwd"));
    }

    @Test
    void commandMatchingIgnoresSlashAndCase() {
        WelcomeStep step = WelcomeStep.parse("Equipe | command:team join");
        assertTrue(step.matchesCommand("team join rouge"));
        assertTrue(step.matchesCommand("/Team Join rouge"));
        assertFalse(step.matchesCommand("team leave"));
        assertFalse(WelcomeStep.parse("Guides | guides").matchesCommand("guides"));
    }
}
