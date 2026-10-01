package com.omneria.fordix.client.guide;

import com.omneria.fordix.Fordix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

/**
 * The Ponder scene of one item (Create 0.5.x). Reached by class name, like {@link GuideActions}, so
 * Fordix has no dependency on Create: when the classes are missing or moved, {@link #hasScene} says
 * false and the guide key falls back to Ponder's index.
 */
public final class PonderScenes {
    private static final String REGISTRY = "com.simibubi.create.foundation.ponder.PonderRegistry";
    private static final String UI = "com.simibubi.create.foundation.ponder.ui.PonderUI";
    private static final String OPENER = "com.simibubi.create.foundation.gui.ScreenOpener";

    private PonderScenes() {
    }

    /** True when Ponder has at least one scene for this item. */
    public static boolean hasScene(ItemStack stack) {
        ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        try {
            Object all = Class.forName(REGISTRY).getField("ALL").get(null);
            return all instanceof Map<?, ?> map && map.get(id) instanceof java.util.Collection<?> scenes && !scenes.isEmpty();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** Opens the item's scene; false when it could not (the caller then opens the index). */
    public static boolean open(ItemStack stack) {
        if (!hasScene(stack)) {
            return false;
        }
        try {
            Screen scene = (Screen) Class.forName(UI).getMethod("of", ItemStack.class).invoke(null, stack);
            try {
                Class.forName(OPENER).getMethod("open", Screen.class).invoke(null, scene);
            } catch (ReflectiveOperationException e) {
                Minecraft.getInstance().setScreen(scene);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Fordix.LOGGER.warn("Fordix: could not open the Ponder scene of {}", stack, e);
            return false;
        }
    }
}
