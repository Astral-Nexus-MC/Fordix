package com.omneria.fordix.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenRuleTest {
    private static final List<String> RULES = List.of("com.jaquadro.minecraft.storagedrawers.inventory", "net.blay09.mods.waystones.client.gui.screen.WarpPlateScreen");

    @Test
    void aPackageMatchesItsClasses() {
        assertTrue(ScreenRule.matches("com.jaquadro.minecraft.storagedrawers.inventory.DrawerScreen", RULES));
    }

    @Test
    void aClassMatchesItself() {
        assertTrue(ScreenRule.matches("net.blay09.mods.waystones.client.gui.screen.WarpPlateScreen", RULES));
    }

    @Test
    void aPrefixOfAWordIsNotAMatch() {
        assertFalse(ScreenRule.matches("com.jaquadro.minecraft.storagedrawers.inventoryExtra.Thing", RULES));
        assertFalse(ScreenRule.matches("net.blay09.mods.waystones.client.gui.screen.WarpPlateScreenOther", RULES));
    }

    @Test
    void theGamesAndFordixScreensAreNeverMatched() {
        List<String> everything = List.of("net.minecraft", "com.omneria", "net.minecraft.client", "com.omneria.fordix.client");
        assertFalse(ScreenRule.matches("net.minecraft.client.gui.screens.PauseScreen", everything));
        assertFalse(ScreenRule.matches("com.omneria.fordix.client.gui.WikiScreen", everything));
    }

    @Test
    void anEmptyListMatchesNothing() {
        assertFalse(ScreenRule.matches("com.jaquadro.minecraft.storagedrawers.inventory.DrawerScreen", List.of()));
        assertFalse(ScreenRule.matches("x.Y", List.of("", "  ")));
    }
}
