package com.omneria.fordix.client.hud;

import com.omneria.fordix.Fordix;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The action-bar message (the line above the hotbar that commands and plugins write) as a glass plate that fades in and out.
 * The messages are read from the chat events that flag them as overlay messages; the game's own overlay is only replaced while
 * one of them is showing, so what the game writes there itself (the name of the disc being played) stays as it was.
 */
public final class ActionBarHud {
    private static Component message;
    private static long shownAt;

    private ActionBarHud() {
    }

    public static void onChat(ClientChatReceivedEvent event) {
        if (event instanceof ClientChatReceivedEvent.System system && system.isOverlay()) {
            message = event.getMessage();
            shownAt = System.currentTimeMillis();
        }
    }

    private static boolean resolved;
    private static Field gameMessage;
    private static Field gameTime;

    /**
     * The messages the game writes itself (the hint to dismount a mount or a boat, the name of the disc being played) sit in private
     * fields of its HUD class: a Component, then the int of the time left, then the flag of the rainbow color (whatever their visibility). Found by that shape
     * (the class layout is the same in the development and the production game); when it is not found, the game draws them.
     */
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
            for (int i = 0; i + 2 < fields.size(); i++) {
                Field a = fields.get(i);
                if (a.getType() == Component.class
                        && fields.get(i + 1).getType() == int.class && fields.get(i + 2).getType() == boolean.class) {
                    gameMessage = a;
                    gameTime = fields.get(i + 1);
                    gameMessage.setAccessible(true);
                    gameTime.setAccessible(true);
                    return;
                }
            }
            StringBuilder layout = new StringBuilder();
            for (Field field : fields) {
                layout.append(field.getType().getSimpleName()).append(' ').append(field.getName()).append(", ");
            }
            Fordix.LOGGER.warn("Fordix: the game's overlay message fields were not found, they stay vanilla ({})", layout);
        } catch (RuntimeException e) {
            gameMessage = null;
            Fordix.LOGGER.warn("Fordix: cannot read the game's overlay message fields: {}", e.toString());
        }
    }

    /** The message the game is showing itself and how opaque it is (0 to 1), or null. */
    private static Object[] gameOwn(float partialTick) {
        resolve();
        if (gameMessage == null) {
            return null;
        }
        try {
            Gui gui = Minecraft.getInstance().gui;
            Component text = gui == null ? null : (Component) gameMessage.get(gui);
            int time = gui == null ? 0 : gameTime.getInt(gui);
            if (text == null || time <= 0) {
                return null;
            }
            float left = time - partialTick;
            float alpha = left > 20.0F ? 1.0F : Math.max(0.0F, left / 20.0F);
            return alpha <= 0.0F ? null : new Object[]{text, alpha};
        } catch (IllegalAccessException | RuntimeException e) {
            return null;
        }
    }

    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.RECORD_OVERLAY.type() || !FordixConfig.voidframeHud()) {
            return;
        }
        Component shown = null;
        float alpha = 0.0F;
        if (message != null) {
            float fade = HudTokens.toastAlpha(System.currentTimeMillis() - shownAt);
            if (fade <= 0.0F) {
                message = null;
            } else {
                shown = message;
                alpha = fade;
            }
        }
        if (shown == null) {
            Object[] own = gameOwn(event.getPartialTick());
            if (own == null) {
                return;
            }
            shown = (Component) own[0];
            alpha = (Float) own[1];
        }
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        GuiGraphics g = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int textWidth = font.width(shown);
        int panelWidth = textWidth + 24;
        int x = (width - panelWidth) / 2;
        int y = height - 72 - Math.round((1.0F - alpha) * 4.0F);
        int bodyAlpha = Math.round(200 * alpha);
        HudStyle.panel(g, x, y, panelWidth, 18, FordixPalette.ACCENT & 0xFFFFFF, bodyAlpha);
        int textAlpha = Math.max(4, Math.round(255 * alpha));
        g.drawString(font, shown, x + 12, y + 5, (textAlpha << 24) | 0xFFFFFF, false);
    }
}
