package com.omneria.fordix.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A short notice in the Voidframe 2 glass style, above the hotbar: a title and a detail, fading in, holding for two seconds
 * and fading away. Used when a HUD profile loads, when the situation is locked, and for the server's HUD messages. A new one
 * replaces the one on screen.
 */
public final class HudToast {
    private static String title = "";
    private static String detail = "";
    private static int accent = 0x3FD1C7;
    private static long shownAt = Long.MIN_VALUE / 2;

    private HudToast() {
    }

    public static void show(String noticeTitle, String noticeDetail, int accentRgb) {
        title = noticeTitle;
        detail = noticeDetail == null ? "" : noticeDetail;
        accent = accentRgb & 0xFFFFFF;
        shownAt = System.currentTimeMillis();
    }

    /** Draws the notice, if there is one; call once a frame from a HUD event. */
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        long elapsed = System.currentTimeMillis() - shownAt;
        float alpha = HudTokens.toastAlpha(elapsed);
        if (alpha <= 0.01F) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int width = Math.max(font.width(title), font.width(detail)) + 28;
        int height = detail.isEmpty() ? 20 : 31;
        int x = (screenWidth - width) / 2;
        // Rises a few pixels while it fades in.
        int lift = Math.round((1.0F - Math.min(1.0F, elapsed / (float) HudTokens.TOAST_IN_MS)) * 6.0F);
        int y = screenHeight - 82 + lift;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        try {
            HudStyle.panel(graphics, x, y, width, height, accent, 0xE6);
            graphics.drawString(font, title, x + 12, y + 6, 0xFFFFFFFF, true);
            if (!detail.isEmpty()) {
                graphics.drawString(font, detail, x + 12, y + 18, 0xFF000000 | accent, true);
            }
            graphics.flush();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
