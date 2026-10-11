package com.omneria.fordix.client.hud;

import com.omneria.fordix.Fordix;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.vanilla.Reflect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The F3 text in Voidframe 2: the game's two columns of lines on glass panels, the name of each figure dimmed and its value bright.
 * The game's debug overlay has two protected methods that return the lines of the left and the right column; they are found by
 * their signature, and told apart by what they say (the right column starts with the Java version, the left one with the game's).
 * When anything is not as expected, the game draws its own F3.
 */
public final class DebugHud {
    /** The game's debug overlay, held by its HUD class: the only field of that type. */
    private static final java.lang.reflect.Field OVERLAY = Reflect.uniqueField(Gui.class, DebugScreenOverlay.class);
    private static boolean resolved;
    private static List<Method> sources = List.of();

    private DebugHud() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        List<Method> found = new ArrayList<>();
        try {
            for (Method method : DebugScreenOverlay.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0 && method.getReturnType() == List.class) {
                    method.setAccessible(true);
                    found.add(method);
                }
            }
        } catch (RuntimeException e) {
            found.clear();
        }
        if (found.size() != 2) {
            Fordix.LOGGER.warn("Fordix: the game's F3 lines were not found ({} candidates), F3 stays vanilla", found.size());
            return;
        }
        sources = found;
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Method method, DebugScreenOverlay overlay) throws ReflectiveOperationException {
        Object value = method.invoke(overlay);
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object line : list) {
                out.add(line == null ? "" : line.toString());
            }
        }
        return out;
    }

    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.DEBUG_TEXT.type() || event.isCanceled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.renderDebug || minecraft.gui == null) {
            return;
        }
        resolve();
        if (sources.isEmpty()) {
            return;
        }
        if (!(Reflect.get(OVERLAY, minecraft.gui) instanceof DebugScreenOverlay overlay)) {
            return;
        }
        try {
            List<String> left = null;
            List<String> right = null;
            for (Method method : sources) {
                List<String> lines = lines(method, overlay);
                boolean system = false;
                for (String line : lines) {
                    if (line != null && line.startsWith("Java:")) {
                        system = true;
                        break;
                    }
                }
                if (system) {
                    right = lines;
                } else {
                    left = lines;
                }
            }
            if (left == null || right == null) {
                sources = List.of();
                return;
            }
            event.setCanceled(true);
            int width = event.getWindow().getGuiScaledWidth();
            Font font = minecraft.font;
            drawColumn(event.getGuiGraphics(), font, left, 2, false, width);
            drawColumn(event.getGuiGraphics(), font, right, width - 2, true, width);
        } catch (ReflectiveOperationException | RuntimeException e) {
            sources = List.of();
            Fordix.LOGGER.warn("Fordix: cannot read the game's F3 lines, F3 stays vanilla: {}", e.toString());
        }
    }

    private static void drawColumn(GuiGraphics g, Font font, List<String> lines, int edge, boolean rightAligned, int screenWidth) {
        int widest = 0;
        int count = 0;
        for (String line : lines) {
            if (!line.isEmpty()) {
                widest = Math.max(widest, font.width(line));
                count++;
            }
        }
        if (count == 0) {
            return;
        }
        int padding = 6;
        int boxWidth = widest + padding * 2 + 2;
        int boxHeight = lines.size() * 9 + padding * 2 - 1;
        int x = rightAligned ? edge - boxWidth : edge;
        int y = 2;
        g.fill(x, y, x + boxWidth, y + boxHeight, 0xC4070B10);
        g.fill(x, y, x + boxWidth, y + 1, 0x24FFFFFF);
        int accentX = rightAligned ? x + boxWidth - 2 : x;
        g.fill(accentX, y, accentX + 2, y + boxHeight, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
        int ty = y + padding;
        for (String line : lines) {
            if (!line.isEmpty()) {
                int tx = rightAligned ? x + boxWidth - padding - 2 - font.width(line) : x + padding + 2;
                int colon = line.indexOf(": ");
                if (colon > 0 && colon < 28) {
                    // "FPS: 144": the name dimmed, the value in the bright tone (colour codes inside the value stay).
                    g.drawString(font, line.substring(0, colon + 1), tx, ty, Glass.dim(), false);
                    g.drawString(font, line.substring(colon + 1), tx + font.width(line.substring(0, colon + 1)), ty, FordixPalette.TEXT_BRIGHT, false);
                } else {
                    g.drawString(font, line, tx, ty, FordixPalette.TEXT_BRIGHT, false);
                }
            }
            ty += 9;
        }
    }
}
