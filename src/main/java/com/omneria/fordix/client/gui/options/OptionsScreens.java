package com.omneria.fordix.client.gui.options;

import com.omneria.fordix.FordixConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * Puts the Voidframe options pages in place of the game's, when the game is about to open one. Only the game's own classes are
 * replaced (a screen of another mod, which is a subclass or another class, stays as it is), and only when it was opened from
 * another screen (the options menu), so the way back is known.
 */
public final class OptionsScreens {
    private OptionsScreens() {
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
        } else if (type == SoundOptionsScreen.class) {
            event.setNewScreen(new SoundScreen(from));
        }
    }
}
