package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Item durability as a small chevron bar under the item, in the wear color vanilla computes
 * (green to red). Registered for every item that can be damaged, modded ones included; it's
 * drawn over the spot where vanilla's own bar goes.
 */
public final class VoidframeDurability implements IItemDecorator {
    private static final VoidframeDurability INSTANCE = new VoidframeDurability();

    private VoidframeDurability() {
    }

    public static void onRegister(RegisterItemDecorationsEvent event) {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item.canBeDepleted()) {
                event.register(item, INSTANCE);
            }
        }
    }

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if (!FordixConfig.voidframeHud() || !stack.isBarVisible()) {
            return false;
        }
        int color = 0xFF000000 | stack.getBarColor();
        Chevron.draw(guiGraphics, xOffset + 1, yOffset + 12, 15, 3, true, 1,
                stack.getBarWidth() / 13.0F, Chevron.brighten(color, 40), color, false);
        return true;
    }
}
