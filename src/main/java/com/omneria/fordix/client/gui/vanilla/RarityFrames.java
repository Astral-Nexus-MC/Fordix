package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.tooltip.ItemRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * A line in the item's rarity color under every uncommon-or-better item, in containers and on
 * the hotbar, so rare loot stands out without opening its tooltip. Drawn over the item's lower edge only,
 * which leaves the item itself readable (Voidframe 2: a glow line, no longer a full frame).
 */
public final class RarityFrames {
    private RarityFrames() {
    }

    /** Containers: after slots and labels, already translated to the container's top-left corner. */
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!FordixConfig.voidframeScreens()) {
            return;
        }
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive() && ItemRarity.isNotable(slot.getItem())) {
                int[] box = screen instanceof WideLayout wideScreen ? wideScreen.boxOf(slot) : new int[]{1, 18};
                frame(guiGraphics, slot.x - box[0], slot.y - box[0], box[1], ItemRarity.color(slot.getItem()));
            }
        }
    }

    /** Hotbar: the nine slots sit at fixed offsets from the screen's bottom center. */
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type() || !FordixConfig.voidframeHud()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }
        int centerX = event.getWindow().getGuiScaledWidth() / 2;
        int bottom = event.getWindow().getGuiScaledHeight();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (ItemRarity.isNotable(stack)) {
                frame(event.getGuiGraphics(), centerX - 90 + i * 20 + 1, bottom - 19, 18, ItemRarity.color(stack));
            }
        }
    }

    /** A glowing line under the item with a soft wash rising from it: the rarity reads without covering the item. */
    private static void frame(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        int rgb = color & 0xFFFFFF;
        guiGraphics.fillGradient(x + 1, y + size - 8, x + size - 1, y + size - 3, rgb, (0x38 << 24) | rgb);
        guiGraphics.fill(x + 1, y + size - 4, x + size - 1, y + size - 3, (0x70 << 24) | rgb);
        guiGraphics.fill(x + 2, y + size - 3, x + size - 2, y + size - 1, (0xFF << 24) | rgb);
    }
}
