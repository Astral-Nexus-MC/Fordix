package com.omneria.fordix.client.hud;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

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

    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.RECORD_OVERLAY.type() || !FordixConfig.voidframeHud() || message == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - shownAt;
        float alpha = HudTokens.toastAlpha(elapsed);
        if (alpha <= 0.0F) {
            message = null;
            return;
        }
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        GuiGraphics g = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int textWidth = font.width(message);
        int panelWidth = textWidth + 24;
        int x = (width - panelWidth) / 2;
        int y = height - 72 - Math.round((1.0F - alpha) * 4.0F);
        int bodyAlpha = Math.round(200 * alpha);
        HudStyle.panel(g, x, y, panelWidth, 18, FordixPalette.ACCENT & 0xFFFFFF, bodyAlpha);
        int textAlpha = Math.max(4, Math.round(255 * alpha));
        g.drawString(font, message, x + 12, y + 5, (textAlpha << 24) | 0xFFFFFF, false);
    }
}
