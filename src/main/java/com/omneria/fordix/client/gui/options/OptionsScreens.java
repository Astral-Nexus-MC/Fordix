package com.omneria.fordix.client.gui.options;

import com.omneria.fordix.FordixConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.ChatOptionsScreen;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.gui.screens.MouseSettingsScreen;
import net.minecraft.client.gui.screens.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * Puts the Voidframe options pages in place of the game's, when the game is about to open one. Only the game's own classes are
 * replaced (a screen of another mod, which is a subclass or another class, stays as it is), and only when it was opened from
 * another screen (the options menu), so the way back is known.
 */
public final class OptionsScreens {
    /** True while the advanced rendering screen of Sodium or Embeddium is being opened from the video page, so that it is let through. */
    private static boolean passing;

    private OptionsScreens() {
    }

    /** The video screen of Sodium, Embeddium or Rubidium: it is named after the mod and ends in "OptionsGUI" (or has "options" in its name). */
    static boolean isRenderingScreen(Screen screen) {
        String name = screen.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        String simple = screen.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        return (name.contains("sodium") || name.contains("embeddium") || name.contains("rubidium")) && simple.contains("option");
    }

    /** Opens the rendering mod's own screen from our video page. */
    public static void openAdvanced(net.minecraft.client.Minecraft minecraft, Screen advanced) {
        passing = true;
        try {
            minecraft.setScreen(advanced);
        } finally {
            passing = false;
        }
    }

    public static void onOpening(ScreenEvent.Opening event) {
        Screen next = event.getNewScreen();
        Screen from = event.getCurrentScreen();
        if (next == null || from == null || !FordixConfig.voidframeScreens()) {
            return;
        }
        Class<?> type = next.getClass();
        if (type == VideoSettingsScreen.class) {
            event.setNewScreen(new VideoScreen(from));
        } else if (!passing && isRenderingScreen(next) && !(from instanceof VideoScreen) && !isRenderingScreen(from)) {
            // The rendering mod's screen takes the place of the game's video screen: ours comes first and links to it for its own settings.
            event.setNewScreen(new VideoScreen(from, next));
        } else if (type == SoundOptionsScreen.class) {
            event.setNewScreen(new SoundScreen(from));
        } else if (KeyControlsScreen.replaces(next)) {
            event.setNewScreen(new KeyControlsScreen(from));
        } else if (type == MouseSettingsScreen.class) {
            event.setNewScreen(new MouseScreen(from));
        } else if (type == ChatOptionsScreen.class) {
            event.setNewScreen(new ChatSettingsScreen(from));
        } else if (type == AccessibilityOptionsScreen.class) {
            event.setNewScreen(new AccessibilityScreen(from));
        } else if (type == SkinCustomizationScreen.class) {
            event.setNewScreen(new SkinScreen(from));
        } else if (type == LanguageSelectScreen.class) {
            event.setNewScreen(new LanguageScreen(from));
        } else if (type == StatsScreen.class && Minecraft.getInstance().player != null) {
            event.setNewScreen(new StatsPageScreen(from, Minecraft.getInstance().player.getStats()));
        }
    }
}
