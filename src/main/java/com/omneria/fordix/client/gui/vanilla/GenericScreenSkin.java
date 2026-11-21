package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.client.hud.HudRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * The Voidframe look for the screens of other mods that the server names ({@code [hud] modScreens}): a glass panel over the mod's
 * own background, a glass box under each slot, the two labels redrawn in the accent, and (elsewhere) the dim behind and the glass
 * buttons. The mod's screen stays what it is, with its menu and its widgets; only what it drew first is covered. A screen that
 * draws gauges or tabs in its background would lose them, which is why the server lists the screens one by one.
 */
public final class GenericScreenSkin {
    private static boolean resolved;
    private static Field titleX;
    private static Field titleY;
    private static Field inventoryX;
    private static Field inventoryY;
    private static Field inventoryTitle;

    private GenericScreenSkin() {
    }

    /** Whether this screen is one of those the server gives the look to. */
    public static boolean applies(Screen screen) {
        return screen != null && HudRules.anyModScreens() && !(screen instanceof VoidframeScreen) && FordixConfig.voidframeScreens()
                && HudRules.skinsScreen(screen.getClass().getName());
    }

    /** Whether the screen also gets the panel and the boxes (the entries of the list that are not written "light:"). */
    public static boolean full(Screen screen) {
        return applies(screen) && HudRules.screenMode(screen.getClass().getName()) == com.omneria.fordix.hud.ScreenRule.Mode.FULL;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            titleX = field("f_97728_");
            titleY = field("f_97729_");
            inventoryX = field("f_97730_");
            inventoryY = field("f_97731_");
            inventoryTitle = field("f_169604_");
        } catch (RuntimeException e) {
            titleX = null;
        }
    }

    private static Field field(String name) {
        Field field = ObfuscationReflectionHelper.findField(AbstractContainerScreen.class, name);
        field.setAccessible(true);
        return field;
    }

    /** After the mod drew its background, before the items: the panel and the boxes. */
    public static void onContainerBackground(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!full(screen)) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        Panel.draw(g, left, top, screen.getXSize(), screen.getYSize());
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive()) {
                VoidframeDraw.well(g, left + slot.x - 1, top + slot.y - 1, 18, 18);
            }
        }
    }

    /** After the labels, in the panel's own coordinates: the dark text of the mod is covered and written again in the accent. */
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!full(screen)) {
            return;
        }
        resolve();
        Font font = Minecraft.getInstance().font;
        GuiGraphics g = event.getGuiGraphics();
        try {
            int tx = titleX == null ? 8 : titleX.getInt(screen);
            int ty = titleY == null ? 6 : titleY.getInt(screen);
            relabel(g, font, screen, screen.getTitle(), tx, ty, true);
            // The "Inventory" label is only there on a screen that shows the player's bag.
            if (inventoryTitle != null && showsPlayerBag(screen) && inventoryTitle.get(screen) instanceof Component text) {
                relabel(g, font, screen, text, inventoryX.getInt(screen), inventoryY.getInt(screen), false);
            }
        } catch (IllegalAccessException | RuntimeException e) {
            titleX = null;
        }
    }

    private static boolean showsPlayerBag(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory) {
                return true;
            }
        }
        return false;
    }

    private static void relabel(GuiGraphics g, Font font, AbstractContainerScreen<?> screen, Component text, int x, int y, boolean title) {
        if (text.getString().isEmpty()) {
            return;
        }
        int width = Math.max(font.width(text), font.width(text.getString().toUpperCase(java.util.Locale.ROOT))) + 2;
        // The panel is drawn again inside the rectangle of the old label, so the patch has the panel's own gradient and no edge.
        // (In this version of the game a scissor is given in screen coordinates, not in those of the translated pose.)
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        g.enableScissor(left + x - 1, top + y - 1, left + x + width, top + y + 9);
        Panel.draw(g, 0, 0, screen.getXSize(), screen.getYSize());
        g.disableScissor();
        if (title) {
            VoidframeDraw.title(g, font, text, x, y);
        } else {
            VoidframeDraw.label(g, font, text, x, y);
        }
    }
}
