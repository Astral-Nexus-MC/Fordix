package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.Glass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.ChatOptionsScreen;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.gui.screens.MouseSettingsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The game's option sub-screens (video, sound, controls, language...) on a Voidframe glass panel with their title in the
 * accent capitals at its top-left, like the options menu: the screens themselves, their lists and their buttons are the game's.
 * A screen with a list gets a panel as tall as the screen; one with only buttons gets one that wraps them.
 */
public final class SubOptionsSkin {
    private static final Set<Class<?>> SCREENS = Set.of(VideoSettingsScreen.class, SoundOptionsScreen.class, MouseSettingsScreen.class,
            ChatOptionsScreen.class, AccessibilityOptionsScreen.class, SkinCustomizationScreen.class, ControlsScreen.class,
            KeyBindsScreen.class, LanguageSelectScreen.class, StatsScreen.class);
    private static final Field TITLE = Reflect.uniqueField(Screen.class, Component.class);
    /** The title each screen had before it was blanked, so a second layout (window resized) still knows it. */
    private static final Map<Screen, String> TITLES = new WeakHashMap<>();
    private static final int MIN_WIDTH = 360;

    private static Screen laidOut;
    private static int x;
    private static int y;
    private static int w;
    private static int h;

    private SubOptionsSkin() {
    }

    /** The panel of a screen whose content spans {@code minX..maxX} and {@code minY..maxY}, kept inside a {@code width} x {@code height} screen. */
    static int[] panelFor(int width, int height, int minX, int minY, int maxX, int maxY, boolean fullHeight) {
        int panelW = Math.min(width - 8, Math.max(MIN_WIDTH, maxX - minX + 32));
        int panelX = Math.max(4, Math.min(width - 4 - panelW, (minX + maxX) / 2 - panelW / 2));
        int top = fullHeight ? 6 : Math.max(4, minY - 34);
        int bottom = fullHeight ? height - 6 : Math.min(height - 4, maxY + 16);
        return new int[]{panelX, top, panelW, bottom - top};
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        laidOut = null;
        Screen screen = event.getScreen();
        if (!FordixConfig.VOIDFRAME_VANILLA_SCREENS.get() || !SCREENS.contains(screen.getClass())) {
            return;
        }
        if (screen instanceof StatsScreen) {
            // Its lists and tabs come with the statistics, after this event: a full-height panel, the lists handled as they draw.
            int[] panel = panelFor(screen.width, screen.height, screen.width / 2 - 170, 0, screen.width / 2 + 170, screen.height, true);
            x = panel[0];
            y = panel[1];
            w = panel[2];
            h = panel[3];
            blankTitle(screen);
            laidOut = screen;
            return;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean list = false;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractSelectionList<?> selection) {
                list = true;
                // The list tiles the game's dirt over the whole screen, which would hide the panel: it draws only its rows.
                selection.setRenderBackground(false);
                selection.setRenderTopAndBottom(false);
                int half = selection.getRowWidth() / 2 + 20;
                minX = Math.min(minX, screen.width / 2 - half);
                maxX = Math.max(maxX, screen.width / 2 + half);
            } else if (listener instanceof AbstractWidget widget && widget.visible) {
                minX = Math.min(minX, widget.getX());
                minY = Math.min(minY, widget.getY());
                maxX = Math.max(maxX, widget.getX() + widget.getWidth());
                maxY = Math.max(maxY, widget.getY() + widget.getHeight());
            }
        }
        if (minX > maxX) {
            return;
        }
        if (minY > maxY) {
            minY = 40;
            maxY = screen.height - 30;
        }
        int[] panel = panelFor(screen.width, screen.height, minX, minY, maxX, maxY, list);
        x = panel[0];
        y = panel[1];
        w = panel[2];
        h = panel[3];
        blankTitle(screen);
        laidOut = screen;
    }

    private static void blankTitle(Screen screen) {
        if (TITLE == null) {
            return;
        }
        try {
            String known = TITLES.get(screen);
            if (known == null) {
                known = ((Component) TITLE.get(screen)).getString();
                TITLES.put(screen, known);
            }
            TITLE.set(screen, Component.empty());
        } catch (IllegalAccessException | RuntimeException ignored) {
            // The game's centered title stays: harmless.
        }
    }

    /** The statistics screen keeps its lists in fields of its own: they stop tiling the dirt over the panel. */
    private static void quietLists(Screen screen) {
        for (Field field : screen.getClass().getDeclaredFields()) {
            if (AbstractSelectionList.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(screen) instanceof AbstractSelectionList<?> list) {
                        list.setRenderBackground(false);
                        list.setRenderTopAndBottom(false);
                    }
                } catch (IllegalAccessException | RuntimeException ignored) {
                    // Left as the game draws it.
                }
            }
        }
    }

    public static void onBackgroundRendered(ScreenEvent.BackgroundRendered event) {
        if (event.getScreen() != laidOut) {
            return;
        }
        if (laidOut instanceof StatsScreen) {
            quietLists(laidOut);
        }
        GuiGraphics g = event.getGuiGraphics();
        Glass.panel(g, x, y, w, h);
        String title = TITLES.getOrDefault(laidOut, "");
        Glass.title(g, Minecraft.getInstance().font, title, x + 14, y + 10);
    }
}
