package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.anim.Smoothing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Voidframe versions of two everyday bits of vanilla interface: the health hearts become a
 * bar with the exact value, and the chat input gets a panel-colored strip with an accent
 * edge. Both are drawn over/instead of vanilla's through Forge events.
 */
public final class VoidframeHud {
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 9;
    private static final int POISON = 0xFF8FA82E;
    private static final int WITHER = 0xFF4A4A4A;
    private static final int FREEZING = 0xFF7FD4FF;
    private static final double EASE_RATE = 9.0;

    /** Health as displayed: it eases toward the real value, so a hit leaves a fading trail. */
    private static double shownHealth = -1;
    private static long lastFrame;

    private VoidframeHud() {
    }

    /** Replaces the hearts. Sits where they were and pushes the armor row up like vanilla's row does. */
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type() || !FordixConfig.VOIDFRAME_HUD.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui instanceof ForgeGui gui) || !gui.shouldDrawSurvivalElements()
                || !(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int x = event.getWindow().getGuiScaledWidth() / 2 - 91;
        int y = event.getWindow().getGuiScaledHeight() - gui.leftHeight;
        gui.leftHeight += 10;
        drawHealthBar(guiGraphics, minecraft.font, player, x, y);
    }

    private static void drawHealthBar(GuiGraphics guiGraphics, Font font, Player player, int x, int y) {
        float max = Math.max(1.0F, player.getMaxHealth());
        float health = Mth.clamp(player.getHealth(), 0.0F, max);
        long now = System.nanoTime();
        double seconds = Math.min(0.1, (now - lastFrame) / 1.0e9);
        lastFrame = now;
        shownHealth = shownHealth < 0 ? health : Smoothing.step(shownHealth, health, seconds, EASE_RATE);

        int color = player.hasEffect(MobEffects.WITHER) ? WITHER
                : player.hasEffect(MobEffects.POISON) ? POISON
                : player.isFullyFrozen() ? FREEZING
                : FordixPalette.ERROR;
        boolean hurt = player.invulnerableTime > 10 && (player.invulnerableTime / 3) % 2 == 1;

        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, hurt ? FordixPalette.TEXT_BRIGHT : FordixPalette.BEVEL_SHADOW);
        guiGraphics.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, FordixPalette.SLOT_FILL);
        int inner = BAR_WIDTH - 2;
        int trail = Math.round(inner * (float) (shownHealth / max));
        int real = Math.round(inner * (health / max));
        if (trail > real) {
            guiGraphics.fill(x + 1 + real, y + 1, x + 1 + trail, y + BAR_HEIGHT - 1, FordixPalette.TEXT);
        }
        guiGraphics.fill(x + 1, y + 1, x + 1 + real, y + BAR_HEIGHT - 1, color);
        guiGraphics.fill(x + 1, y + 1, x + 1 + real, y + 2, FordixPalette.TEXT_BRIGHT & 0x55FFFFFF | 0x55000000);

        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0F) {
            int gold = Math.round(inner * Math.min(1.0F, absorption / max));
            guiGraphics.fill(x + 1, y + BAR_HEIGHT - 3, x + 1 + gold, y + BAR_HEIGHT - 1, FordixPalette.FLAME);
        }
        String text = Mth.ceil(health) + (absorption > 0.0F ? "+" + Mth.ceil(absorption) : "");
        guiGraphics.drawString(font, text, x + (BAR_WIDTH - font.width(text)) / 2, y + 1, FordixPalette.TEXT_BRIGHT, true);
    }

    /** Behind vanilla's translucent input strip: a panel tint, so the strip reads dark blue instead of black. */
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof ChatScreen && FordixConfig.VOIDFRAME_VANILLA_SCREENS.get()) {
            int width = event.getScreen().width;
            int height = event.getScreen().height;
            event.getGuiGraphics().fill(2, height - 14, width - 2, height - 2, 0xCC000000 | (FordixPalette.PANEL_FILL & 0xFFFFFF));
        }
    }

    /** Over the strip but under nothing that matters: a top rule and an accent edge, clear of the text. */
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof ChatScreen && FordixConfig.VOIDFRAME_VANILLA_SCREENS.get()) {
            int width = event.getScreen().width;
            int height = event.getScreen().height;
            GuiGraphics guiGraphics = event.getGuiGraphics();
            guiGraphics.fill(2, height - 14, width - 2, height - 13, FordixPalette.BEVEL_HIGHLIGHT);
            guiGraphics.fill(2, height - 14, 4, height - 2, FordixPalette.ACCENT);
        }
    }
}
