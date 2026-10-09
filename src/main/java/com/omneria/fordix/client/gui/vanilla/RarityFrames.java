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
 * A frame in the item's rarity color around every uncommon-or-better item, in containers and on
 * the hotbar, so rare loot stands out without opening its tooltip. Drawn over the item with a
 * 1px outline and a faint tint, which leave the item itself readable.
 */
public final class RarityFrames {
    private RarityFrames() {
    }

    /** Containers: after slots and labels, already translated to the container's top-left corner. */
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!FordixConfig.VOIDFRAME_VANILLA_SCREENS.get()) {
            return;
        }
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive() && ItemRarity.isNotable(slot.getItem())) {
                frame(guiGraphics, slot.x - 1, slot.y - 1, 18, ItemRarity.color(slot.getItem()));
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

    private static void frame(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        int outline = (0xCC << 24) | (color & 0xFFFFFF);
        int tint = (0x28 << 24) | (color & 0xFFFFFF);
        guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, tint);
        guiGraphics.fill(x, y, x + size, y + 1, outline);
        guiGraphics.fill(x, y + size - 1, x + size, y + size, outline);
        guiGraphics.fill(x, y + 1, x + 1, y + size - 1, outline);
        guiGraphics.fill(x + size - 1, y + 1, x + size, y + size - 1, outline);
    }
}
