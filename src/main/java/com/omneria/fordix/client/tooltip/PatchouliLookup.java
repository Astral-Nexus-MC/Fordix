package com.omneria.fordix.client.tooltip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Patchouli's quick lookup (the "?" badge with the book beside an item's tooltip, hold Ctrl to open its entry) listens to the
 * tooltip event, which the Voidframe tooltip cancels: this draws the badge for it, beside our panel, in the place it would have
 * taken beside the game's tooltip. Reflection only; when Patchouli is missing or its API changes, nothing happens.
 */
final class PatchouliLookup {
    /** The badge sits 46 px left of the tooltip's left edge and 8 px under its top, as beside the game's own tooltip. */
    private static final int BEHIND_LEFT = 12;
    private static final int BELOW_TOP = 12;
    private static boolean broken;
    private static Method onTooltip;

    private PatchouliLookup() {
    }

    static void draw(GuiGraphics graphics, ItemStack stack, int panelX, int panelY) {
        if (broken || !ModList.get().isLoaded("patchouli")) {
            return;
        }
        try {
            if (onTooltip == null) {
                onTooltip = Class.forName("vazkii.patchouli.client.handler.TooltipHandler")
                        .getMethod("onTooltip", GuiGraphics.class, ItemStack.class, int.class, int.class);
            }
            onTooltip.invoke(null, graphics, stack, panelX - BEHIND_LEFT, panelY + BELOW_TOP);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unknown) {
            broken = true;
        }
    }
}
