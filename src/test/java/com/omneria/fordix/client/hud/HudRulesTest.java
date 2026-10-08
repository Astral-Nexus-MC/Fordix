package com.omneria.fordix.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HudRulesTest {
    @AfterEach
    void reset() {
        HudRules.clear();
    }

    @Test
    void withoutRulesThePlayerDecides() {
        assertTrue(HudRules.hidden("health", true));
        assertFalse(HudRules.hidden("health", false));
    }

    @Test
    void requiredCanNotBeHiddenAndForbiddenIsAlwaysHidden() {
        HudRules.set(List.of("health", "minecraft:hotbar"), List.of("fordix:coordinates", "health"), "  FXH1-x ");
        assertFalse(HudRules.hidden("health", true));
        assertFalse(HudRules.hidden("minecraft:hotbar", true));
        assertTrue(HudRules.hidden("fordix:coordinates", false));
        assertFalse(HudRules.hidden("armor", false));
        assertTrue(HudRules.locked("fordix:coordinates"));
        assertFalse(HudRules.locked("armor"));
        assertEquals("FXH1-x", HudRules.proposed());
    }
}
