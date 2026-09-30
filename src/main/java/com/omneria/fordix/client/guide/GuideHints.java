package com.omneria.fordix.client.guide;

import com.omneria.fordix.client.FordixClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * A guide one key away from any item of a mod that has one: the tooltip says "Maj + G: open the
 * guide" and pressing it over the item in a container screen runs the mod's guide action
 * (Ponder, a Patchouli book, its wiki...). Only mods with an installed guide entry get the line.
 */
public final class GuideHints {
    private GuideHints() {
    }

    /** The first installed guide entry of the item's mod, or null. */
    public static GuideEntry entryFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return null;
        }
        for (GuideEntry entry : GuideTabs.installedEntries()) {
            if (entry.modId().equals(id.getNamespace())) {
                return entry;
            }
        }
        return null;
    }

    private static String keyLabel() {
        return "Maj + " + FordixClient.OPEN_GUIDE.getTranslatedKeyMessage().getString().toUpperCase();
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        GuideEntry entry = entryFor(event.getItemStack());
        if (entry == null) {
            return;
        }
        List<Component> lines = event.getToolTip();
        lines.add(Component.literal("[" + keyLabel() + "] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("Ouvrir le guide : " + entry.label()).withStyle(ChatFormatting.GRAY)));
    }

    /** Shift + the guide key over an item in a container screen. */
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !Screen.hasShiftDown()
                || !FordixClient.OPEN_GUIDE.matches(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null) {
            return;
        }
        GuideEntry entry = entryFor(slot.getItem());
        if (entry != null) {
            event.setCanceled(true);
            GuideActions.run(entry);
        }
    }
}
