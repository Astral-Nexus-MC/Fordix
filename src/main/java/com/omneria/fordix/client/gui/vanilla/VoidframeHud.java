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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Voidframe versions of everyday bits of vanilla interface: the health hearts and the hunger
 * drumsticks become two angular bars facing each other across the hotbar (with the exact
 * values), and the chat input gets a panel-colored strip with an accent
 * edge. Both are drawn over/instead of vanilla's through Forge events.
 */
public final class VoidframeHud {
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 9;
    private static final int SLANT = 4;
    /** Rows inside the outline. */
    private static final int INNER_ROWS = BAR_HEIGHT - 2;
    private static final int HUNGRY = 0xFF7A8F3A;
    /** Light gold, so the saturation line reads over the orange hunger bar in any theme. */
    private static final int SATURATION = 0xFFFFE9B0;
    private static final int POISON = 0xFF8FA82E;
    private static final int WITHER = 0xFF4A4A4A;
    private static final int FREEZING = 0xFF7FD4FF;
    private static final double EASE_RATE = 9.0;

    /** Health as displayed: it eases toward the real value, so a hit leaves a fading trail. */
    private static double shownHealth = -1;
    private static long lastFrame;

    private VoidframeHud() {
    }

    /** Replaces the hearts and the drumsticks. Each bar sits where vanilla's row was and pushes what stacks above it. */
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        VoidframeGauges.onOverlayPre(event);
        boolean health = event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type();
        boolean food = event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type();
        if ((!health && !food) || !FordixConfig.VOIDFRAME_HUD.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui instanceof ForgeGui gui) || !gui.shouldDrawSurvivalElements()
                || !(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }
        // On a mount, vanilla shows the mount's health where the hunger goes: leave that to it.
        if (food && player.getVehicle() instanceof LivingEntity) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int centerX = event.getWindow().getGuiScaledWidth() / 2;
        int screenHeight = event.getWindow().getGuiScaledHeight();
        if (health) {
            int y = screenHeight - gui.leftHeight;
            gui.leftHeight += 10;
            int[] offset = HudPlacement.delta("health");
            if (!HudPlacement.hidden("health")) {
                boolean sized = HudPlacement.begin(guiGraphics, "health", false);
                drawHealthBar(guiGraphics, minecraft.font, player, centerX - 91 + offset[0], y + offset[1]);
                HudPlacement.end(guiGraphics, sized);
            }
        } else {
            int y = screenHeight - gui.rightHeight;
            gui.rightHeight += 10;
            int[] offset = HudPlacement.delta("hunger");
            if (!HudPlacement.hidden("hunger")) {
                boolean sized = HudPlacement.begin(guiGraphics, "hunger", false);
                drawHungerBar(guiGraphics, minecraft.font, player, centerX + 91 - BAR_WIDTH + offset[0], y + offset[1]);
                HudPlacement.end(guiGraphics, sized);
            }
        }
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

        frame(guiGraphics, x, y, true, hurt ? FordixPalette.TEXT_BRIGHT : FordixPalette.BEVEL_SHADOW);
        int inner = BAR_WIDTH - 2;
        int trail = Math.round(inner * (float) (shownHealth / max));
        int real = Math.round(inner * (health / max));
        if (trail > real) {
            fillRange(guiGraphics, x, y, true, real, trail, 0, INNER_ROWS, FordixPalette.TEXT);
        }
        fillRange(guiGraphics, x, y, true, 0, real, 0, INNER_ROWS, color);
        fillRange(guiGraphics, x, y, true, 0, real, 0, 1, brighten(color));

        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0F) {
            int gold = Math.round(inner * Math.min(1.0F, absorption / max));
            fillRange(guiGraphics, x, y, true, 0, gold, INNER_ROWS - 2, INNER_ROWS, FordixPalette.FLAME);
        }
        String text = Mth.ceil(health) + (absorption > 0.0F ? "+" + Mth.ceil(absorption) : "");
        guiGraphics.drawString(font, text, x + 5, y + 1, FordixPalette.TEXT_BRIGHT, true);
    }

    private static void drawHungerBar(GuiGraphics guiGraphics, Font font, Player player, int x, int y) {
        FoodData food = player.getFoodData();
        float level = Mth.clamp(food.getFoodLevel(), 0, 20);
        int color = player.hasEffect(MobEffects.HUNGER) ? HUNGRY : FordixPalette.FLAME;
        frame(guiGraphics, x, y, false, FordixPalette.BEVEL_SHADOW);
        int inner = BAR_WIDTH - 2;
        int filled = Math.round(inner * (level / 20.0F));
        // Hunger drains toward the hotbar's side, like vanilla's row does from the right.
        fillRange(guiGraphics, x, y, false, inner - filled, inner, 0, INNER_ROWS, color);
        fillRange(guiGraphics, x, y, false, inner - filled, inner, 0, 1, brighten(color));
        // Saturation: a thin light line, the reserve that's burned before hunger drops.
        int reserve = Math.round(inner * Mth.clamp(food.getSaturationLevel(), 0.0F, 20.0F) / 20.0F);
        fillRange(guiGraphics, x, y, false, inner - reserve, inner, INNER_ROWS - 1, INNER_ROWS, SATURATION);
        String text = String.valueOf((int) level);
        guiGraphics.drawString(font, text, x + BAR_WIDTH - 5 - font.width(text), y + 1, FordixPalette.TEXT_BRIGHT, true);
    }

    /** The bar's outline and empty track, both cut on the slant. */
    private static void frame(GuiGraphics guiGraphics, int x, int y, boolean cutRight, int border) {
        for (int row = 0; row < BAR_HEIGHT; row++) {
            int[] span = BarShape.rowSpan(row, BAR_HEIGHT, BAR_WIDTH, SLANT, cutRight);
            guiGraphics.fill(x + span[0], y + row, x + span[1], y + row + 1, border);
            boolean edge = row == 0 || row == BAR_HEIGHT - 1;
            if (!edge) {
                guiGraphics.fill(x + span[0] + 1, y + row, x + span[1] - 1, y + row + 1, FordixPalette.SLOT_FILL);
            }
        }
    }

    /**
     * Fills columns [from, to) on the inner rows [rowFrom, rowTo) of the bar (inside the 1px
     * outline, so row 0 is the bar's second row), clipped to the slanted shape.
     */
    private static void fillRange(GuiGraphics guiGraphics, int x, int y, boolean cutRight, int from, int to,
                                  int rowFrom, int rowTo, int color) {
        for (int row = Math.max(0, rowFrom); row < Math.min(INNER_ROWS, rowTo); row++) {
            drawRow(guiGraphics, x, y, cutRight, from, to, row + 1, color);
        }
    }

    private static void drawRow(GuiGraphics guiGraphics, int x, int y, boolean cutRight, int from, int to, int row, int color) {
        int[] span = BarShape.rowSpan(row, BAR_HEIGHT, BAR_WIDTH, SLANT, cutRight);
        int left = Math.max(x + 1 + from, x + span[0] + 1);
        int right = Math.min(x + 1 + to, x + span[1] - 1);
        if (right > left) {
            guiGraphics.fill(left, y + row, right, y + row + 1, color);
        }
    }

    private static int brighten(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 40);
        int b = Math.min(255, (color & 0xFF) + 40);
        return 0xFF000000 | r << 16 | g << 8 | b;
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
