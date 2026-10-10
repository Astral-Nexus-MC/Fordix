package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * A Voidframe 2 backdrop behind the game's own menus. Without a world behind them (options opened from the title screen, the
 * connection and loading screens) the game tiles its dirt texture; a nearly opaque dark gradient replaces it. With a world
 * behind, the world stays visible and only gets a thin navy tint. The title screen keeps its panorama.
 */
public final class ScreenBackdrop {
    private ScreenBackdrop() {
    }

    public static void onBackgroundRendered(ScreenEvent.BackgroundRendered event) {
        Screen screen = event.getScreen();
        if (!FordixConfig.VOIDFRAME_VANILLA_SCREENS.get() || screen instanceof TitleScreen || screen instanceof VoidframeScreen) {
            return;
        }
        String name = screen.getClass().getName();
        if (!name.startsWith("net.minecraft.client.gui.screens.") && !name.startsWith("net.minecraftforge.client.gui.")) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        if (Minecraft.getInstance().level == null) {
            g.fillGradient(0, 0, screen.width, screen.height, 0xF00A1118, 0xF8030609);
        } else {
            g.fill(0, 0, screen.width, screen.height, 0x38050A12);
        }
    }
}
