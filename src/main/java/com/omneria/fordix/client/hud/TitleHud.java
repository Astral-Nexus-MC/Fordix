package com.omneria.fordix.client.hud;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The big title and subtitle (the {@code /title} command) in Voidframe 2: a dark band across the screen, the title large with a
 * soft shadow, an accent rule, and the subtitle in capitals under it. The game keeps these in private fields of its HUD class and
 * publishes no event, so they are read by type and declaration order (the same class layout in the development and the production
 * game): the countdown int, then the title and subtitle components, then the three fade lengths. When the layout does not match,
 * nothing is read and the game's own title is drawn.
 */
public final class TitleHud {
    private static boolean resolved;
    private static Field countdown;
    private static Field title;
    private static Field subtitle;
    private static Field fadeIn;
    private static Field stay;
    private static Field fadeOut;

    private TitleHud() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            List<Field> fields = new ArrayList<>();
            for (Field field : Gui.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
            for (int i = 1; i + 4 < fields.size(); i++) {
                Field before = fields.get(i - 1);
                Field a = fields.get(i);
                Field b = fields.get(i + 1);
                Field c = fields.get(i + 2);
                Field d = fields.get(i + 3);
                Field e = fields.get(i + 4);
                if (before.getType() == int.class && a.getType() == Component.class && b.getType() == Component.class
                        && c.getType() == int.class && d.getType() == int.class && e.getType() == int.class
                        && Modifier.isProtected(a.getModifiers())) {
                    countdown = before;
                    title = a;
                    subtitle = b;
                    fadeIn = c;
                    stay = d;
                    fadeOut = e;
                    for (Field field : List.of(countdown, title, subtitle, fadeIn, stay, fadeOut)) {
                        field.setAccessible(true);
                    }
                    return;
                }
            }
            Fordix.LOGGER.warn("Fordix: the game's title fields were not found, titles stay vanilla");
        } catch (RuntimeException e) {
            countdown = null;
            Fordix.LOGGER.warn("Fordix: cannot read the game's title fields: {}", e.toString());
        }
    }

    /**
     * Whether the game's title fields were found. The in-game test calls it, so a game version that changes the class layout fails
     * the test instead of silently bringing the vanilla title back.
     */
    public static boolean selfCheck() {
        resolved = false;
        resolve();
        return countdown != null;
    }

    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.TITLE_TEXT.type() || !FordixConfig.voidframeHud()) {
            return;
        }
        resolve();
        if (countdown == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Gui gui = minecraft.gui;
        if (gui == null) {
            return;
        }
        try {
            Component main = (Component) title.get(gui);
            Component sub = (Component) subtitle.get(gui);
            int time = countdown.getInt(gui);
            if ((main == null && sub == null) || time <= 0) {
                return;
            }
            int in = fadeIn.getInt(gui);
            int hold = stay.getInt(gui);
            int out = fadeOut.getInt(gui);
            float remaining = time - event.getPartialTick();
            float alpha;
            if (time > out + hold) {
                alpha = (in + hold + out - remaining) / Math.max(1, in);
            } else if (time <= out) {
                alpha = remaining / Math.max(1, out);
            } else {
                alpha = 1.0F;
            }
            alpha = Math.max(0.0F, Math.min(1.0F, alpha));
            if (alpha <= 0.0F) {
                event.setCanceled(true);
                return;
            }
            event.setCanceled(true);
            draw(event.getGuiGraphics(), minecraft.font, main, sub, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(), alpha);
        } catch (IllegalAccessException | RuntimeException e) {
            countdown = null;
        }
    }

    private static void draw(GuiGraphics g, Font font, Component main, Component sub, int width, int height, float alpha) {
        int centerX = width / 2;
        float titleScale = 3.0F;
        int top = height / 2 - 40;
        int bandHeight = 74;
        int a = Math.max(1, Math.round(alpha * 255));
        // The band: dark in the middle, transparent at both ends of the screen.
        int steps = 24;
        for (int i = 0; i < steps; i++) {
            float edge = 1.0F - Math.abs(i - (steps - 1) / 2.0F) / (steps / 2.0F);
            int from = i * width / steps;
            int to = (i + 1) * width / steps;
            g.fill(from, top, to, top + bandHeight, HudTokens.withAlpha(0x03060A, Math.round(150 * alpha * Math.min(1.0F, edge * 1.6F))));
        }
        if (main != null) {
            String text = main.getString();
            g.pose().pushPose();
            g.pose().translate(centerX, top + 10, 0);
            g.pose().scale(titleScale, titleScale, 1.0F);
            g.drawString(font, main, -font.width(text) / 2, 0, (a << 24) | 0xFFFFFF, true);
            g.pose().popPose();
        }
        if (sub != null) {
            String text = sub.getString().toUpperCase(java.util.Locale.ROOT);
            Glass.rule(g, centerX - 90, top + 44, 180, HudTokens.withAlpha(FordixPalette.ACCENT, a));
            g.drawString(font, text, centerX - font.width(text) / 2, top + 52,
                    (a << 24) | (HudTokens.mix(FordixPalette.ACCENT, 0xFFFFFFFF, 0.35F) & 0xFFFFFF), false);
        }
    }
}
