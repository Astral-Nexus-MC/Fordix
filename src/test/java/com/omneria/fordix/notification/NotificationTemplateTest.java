package com.omneria.fordix.notification;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationTemplateTest {

    @Test
    void parsesStyleTitleAndMessage() {
        NotificationTemplate template = NotificationTemplate.parse("success | Bienvenue | Bon jeu !");
        assertEquals(new NotificationTemplate(NotificationStyle.SUCCESS, "Bienvenue", "Bon jeu !"), template);
    }

    @Test
    void styleIsOptional() {
        assertEquals(new NotificationTemplate(NotificationStyle.INFO, "Bienvenue", "Bon jeu !"),
                NotificationTemplate.parse("Bienvenue | Bon jeu !"));
        assertEquals(new NotificationTemplate(NotificationStyle.INFO, "Bienvenue", ""),
                NotificationTemplate.parse("Bienvenue"));
    }

    @Test
    void messageIsOptionalAfterAStyle() {
        assertEquals(new NotificationTemplate(NotificationStyle.WARNING, "Maintenance", ""),
                NotificationTemplate.parse("warning | Maintenance"));
    }

    @Test
    void aMessageMayItselfContainABar() {
        NotificationTemplate template = NotificationTemplate.parse("info | Discord | discord.gg/x | rejoins-nous");
        assertEquals("discord.gg/x | rejoins-nous", template.message());
    }

    @Test
    void blankTitlesAreRejected() {
        assertNull(NotificationTemplate.parse(""));
        assertNull(NotificationTemplate.parse("info |  | message"));
        assertNull(NotificationTemplate.parse(null));
    }

    @Test
    void fillsKnownPlaceholdersAndKeepsUnknownOnes() {
        NotificationTemplate template = NotificationTemplate.parse("Bienvenue sur {server} v{version} | Salut {player}, {unknown}");
        NotificationTemplate filled = template.filled(Map.of("server", "Astral Nexus", "version", "1.2.0", "player", "Steve"));

        assertEquals("Bienvenue sur Astral Nexus v1.2.0", filled.title());
        assertEquals("Salut Steve, {unknown}", filled.message());
    }
}
