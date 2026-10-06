package com.omneria.fordix.client.guide;

import com.omneria.fordix.Tr;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.FordixClient;
import com.omneria.fordix.client.gui.ItemSheetScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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

    private static boolean isPonder(GuideEntry entry) {
        return entry != null && entry.action() == GuideEntry.Action.PONDER;
    }

    /** "Maj + G": the shortcut as the player has it set. */
    public static String keyLabel() {
        return Tr.s("guides.hint.shift") + " + " + FordixClient.OPEN_GUIDE.getTranslatedKeyMessage().getString().toUpperCase();
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!FordixConfig.GUIDE_HINTS.get() || FordixClient.OPEN_GUIDE.isUnbound()) {
            return;
        }
        if (event.getItemStack().isEmpty()) {
            return;
        }
        List<Component> lines = event.getToolTip();
        String text = PonderScenes.hasScene(event.getItemStack()) && isPonder(entryFor(event.getItemStack()))
                ? Tr.s("guides.hint.ponder") : Tr.s("guides.hint.sheet");
        lines.add(Component.literal("[" + keyLabel() + "] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(text).withStyle(ChatFormatting.GRAY)));
    }

    /** Shift + the guide key over an item in a container screen. */
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || com.omneria.fordix.client.gui.vanilla.InventoryTools.typing()
                || !FordixClient.pressed(FordixClient.OPEN_GUIDE, event.getKeyCode(), event.getScanCode())) {
            return;
        }
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null) {
            return;
        }
        if (slot.getItem().isEmpty()) {
            return;
        }
        event.setCanceled(true);
        if (isPonder(entryFor(slot.getItem())) && PonderScenes.open(slot.getItem())) {
            return;
        }
        Minecraft.getInstance().setScreen(new ItemSheetScreen(screen, slot.getItem()));
    }
}
