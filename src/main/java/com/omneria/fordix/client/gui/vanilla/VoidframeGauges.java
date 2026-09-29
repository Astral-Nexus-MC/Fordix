package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * The rest of the Voidframe HUD, in the same chevron shape as the health and hunger bars:
 * a thin experience line with a level badge, an armor bar above the health, an air bar above the
 * hunger (only while it matters) and boss bars at the top. Each replaces vanilla's element by
 * cancelling its overlay; when the game state doesn't fit what's expected, vanilla's is kept.
 */
public final class VoidframeGauges {
    private static final int SLANT = 3;
    private static final int GAUGE_WIDTH = 52;
    private static final int GAUGE_HEIGHT = 7;
    private static final int XP_TOP = 0xFF7DFFB0;
    private static final int XP_BOTTOM = 0xFF2BB56A;
    private static final int ARMOR_TOP = 0xFFC6D3DC;
    private static final int ARMOR_BOTTOM = 0xFF6F8290;
    private static final int AIR_TOP = 0xFF9BE0FF;
    private static final int AIR_BOTTOM = 0xFF3A8FBF;
    private static final Field BOSS_EVENTS = Reflect.uniqueField(BossHealthOverlay.class, Map.class);

    private VoidframeGauges() {
    }

    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!FordixConfig.VOIDFRAME_HUD.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui instanceof ForgeGui gui) || !(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }
        var overlay = event.getOverlay();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        if (overlay == VanillaGuiOverlay.EXPERIENCE_BAR.type()) {
            if (gui.shouldDrawSurvivalElements() && minecraft.player != null && minecraft.player.jumpableVehicle() == null
                    && minecraft.gameMode != null && minecraft.gameMode.hasExperience()) {
                event.setCanceled(true);
                drawExperience(guiGraphics, minecraft.font, player, width, height);
            }
        } else if (overlay == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
            if (gui.shouldDrawSurvivalElements()) {
                event.setCanceled(true);
                int top = height - gui.leftHeight;
                gui.leftHeight += 10;
                if (player.getArmorValue() > 0) {
                    drawArmor(guiGraphics, minecraft.font, player, width / 2 - 91, top + 1);
                }
            }
        } else if (overlay == VanillaGuiOverlay.AIR_LEVEL.type()) {
            int air = player.getAirSupply();
            int max = player.getMaxAirSupply();
            if (gui.shouldDrawSurvivalElements() && (player.isEyeInFluid(FluidTags.WATER) || air < max)) {
                event.setCanceled(true);
                int top = height - gui.rightHeight;
                gui.rightHeight += 10;
                drawAir(guiGraphics, minecraft.font, air, max, width / 2 + 91 - GAUGE_WIDTH, top + 1);
            }
        } else if (overlay == VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) {
            if (Reflect.get(BOSS_EVENTS, minecraft.gui.getBossOverlay()) instanceof Map<?, ?> events) {
                event.setCanceled(true);
                drawBosses(guiGraphics, minecraft.font, events, width, height);
            }
        }
    }

    /** A thin line just above the hotbar, and the level in a badge between health and hunger. */
    private static void drawExperience(GuiGraphics guiGraphics, Font font, Player player, int width, int height) {
        int x = width / 2 - 91;
        Chevron.draw(guiGraphics, x, height - 25, 182, 3, true, 1, player.experienceProgress, XP_TOP, XP_BOTTOM, false);
        if (player.experienceLevel > 0) {
            String level = String.valueOf(player.experienceLevel);
            int badgeWidth = Math.max(22, font.width(level) + 14);
            int badgeX = width / 2 - badgeWidth / 2;
            int badgeY = height - 40;
            int badgeHeight = 11;
            for (int row = 0; row < badgeHeight; row++) {
                int inset = 4 - Math.round(4.0F * row / (badgeHeight - 1));
                int leftInset = Math.round(4.0F * (badgeHeight - 1 - row) / (badgeHeight - 1));
                int rightInset = Math.round(4.0F * row / (badgeHeight - 1));
                boolean edge = row == 0 || row == badgeHeight - 1;
                guiGraphics.fill(badgeX + leftInset, badgeY + row, badgeX + badgeWidth - rightInset, badgeY + row + 1,
                        edge ? XP_TOP : FordixPalette.BEVEL_SHADOW);
                if (!edge) {
                    guiGraphics.fill(badgeX + leftInset + 1, badgeY + row, badgeX + badgeWidth - rightInset - 1, badgeY + row + 1,
                            FordixPalette.SLOT_FILL);
                }
            }
            guiGraphics.drawString(font, level, width / 2 - font.width(level) / 2, badgeY + 2, XP_TOP, true);
        }
    }

    private static void drawArmor(GuiGraphics guiGraphics, Font font, Player player, int x, int y) {
        int armor = player.getArmorValue();
        Chevron.draw(guiGraphics, x, y, GAUGE_WIDTH, GAUGE_HEIGHT, true, SLANT, armor / 20.0F, ARMOR_TOP, ARMOR_BOTTOM, false);
        guiGraphics.drawString(font, String.valueOf(armor), x + GAUGE_WIDTH + 4, y, ARMOR_TOP, true);
    }

    private static void drawAir(GuiGraphics guiGraphics, Font font, int air, int max, int x, int y) {
        float fraction = Mth.clamp(air / (float) max, 0.0F, 1.0F);
        Chevron.draw(guiGraphics, x, y, GAUGE_WIDTH, GAUGE_HEIGHT, false, SLANT, fraction, AIR_TOP, AIR_BOTTOM, true);
        String seconds = Math.max(0, Mth.ceil(air / 20.0F)) + "s";
        guiGraphics.drawString(font, seconds, x - 4 - font.width(seconds), y, AIR_TOP, true);
    }

    private static void drawBosses(GuiGraphics guiGraphics, Font font, Map<?, ?> events, int width, int height) {
        int y = 14;
        for (Object value : events.values()) {
            if (!(value instanceof LerpingBossEvent boss)) {
                continue;
            }
            int x = width / 2 - 91;
            int[] colors = bossColors(boss.getColor());
            Chevron.draw(guiGraphics, x, y, 182, 8, true, 3, boss.getProgress(), colors[0], colors[1], false);
            int notches = switch (boss.getOverlay()) {
                case NOTCHED_6 -> 6;
                case NOTCHED_10 -> 10;
                case NOTCHED_12 -> 12;
                case NOTCHED_20 -> 20;
                default -> 0;
            };
            for (int i = 1; i < notches; i++) {
                int notchX = x + 1 + Math.round(180.0F * i / notches);
                guiGraphics.fill(notchX, y + 1, notchX + 1, y + 7, 0x66000000);
            }
            guiGraphics.drawCenteredString(font, boss.getName(), width / 2, y - 10, 0xFFFFFFFF);
            y += 19;
            if (y >= height / 3) {
                break;
            }
        }
    }

    private static int[] bossColors(BossEvent.BossBarColor color) {
        return switch (color) {
            case PINK -> new int[]{0xFFFF7AC0, 0xFFC03A82};
            case BLUE -> new int[]{0xFF7AB8FF, 0xFF2F62C0};
            case RED -> new int[]{0xFFFF7468, 0xFFA82F29};
            case GREEN -> new int[]{0xFF7DFFB0, 0xFF2BB56A};
            case YELLOW -> new int[]{0xFFFFE58A, 0xFFC79A2A};
            case PURPLE -> new int[]{0xFFC9A8FF, 0xFF7A4FD0};
            case WHITE -> new int[]{0xFFFFFFFF, 0xFF9FB3C0};
        };
    }
}
