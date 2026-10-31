package com.omneria.fordix.client.gui.vanilla;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A read-only look at the Curios inventory of the player (rings, charms, bauble slots of other mods), by reflection: Fordix does
 * not depend on Curios, and when the mod is missing or its API changes the list is empty and the inventory stays as it was.
 * Nothing here writes: the slots themselves stay in the Curios screen, which a click on the column opens.
 */
public final class CuriosView {
    /** One slot of the Curios inventory: what it holds and the name of its kind ("ring", "charm"). */
    public record Cell(ItemStack stack, String identifier) {
    }

    private static final String API = "top.theillusivec4.curios.api.CuriosApi";
    private static boolean broken;
    private static Method inventoryOf;
    private static Method resolve;
    private static Method curios;
    private static Method visible;
    private static Method stacksOf;
    private static Method slotsOf;
    private static Method stackIn;
    private static Method identifierOf;
    /** The dev test puts fake cells here to draw the column without the mod. */
    private static List<Cell> forced;

    private CuriosView() {
    }

    public static void force(List<Cell> cells) {
        forced = cells;
        CuriosLink.assume(cells != null);
    }

    /** Every slot of every visible kind, in the order Curios keeps them. */
    public static List<Cell> cells() {
        if (forced != null) {
            return forced;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (broken || minecraft.player == null || !net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            return List.of();
        }
        try {
            if (inventoryOf == null) {
                Class<?> api = Class.forName(API);
                inventoryOf = api.getMethod("getCuriosInventory", LivingEntity.class);
            }
            Object lazy = inventoryOf.invoke(null, minecraft.player);
            if (resolve == null) {
                resolve = lazy.getClass().getMethod("resolve");
            }
            Optional<?> handler = (Optional<?>) resolve.invoke(lazy);
            if (handler.isEmpty()) {
                return List.of();
            }
            if (curios == null) {
                curios = handler.get().getClass().getMethod("getCurios");
            }
            Map<?, ?> kinds = (Map<?, ?>) curios.invoke(handler.get());
            List<Cell> out = new ArrayList<>();
            for (Object kind : kinds.values()) {
                if (visible == null) {
                    visible = find(kind, "isVisible");
                    stacksOf = find(kind, "getStacks");
                    identifierOf = find(kind, "getIdentifier");
                }
                if (!(Boolean) visible.invoke(kind)) {
                    continue;
                }
                Object stacks = stacksOf.invoke(kind);
                if (slotsOf == null) {
                    slotsOf = find(stacks, "getSlots");
                    stackIn = find(stacks, "getStackInSlot", int.class);
                }
                int count = (Integer) slotsOf.invoke(stacks);
                String identifier = (String) identifierOf.invoke(kind);
                for (int i = 0; i < count; i++) {
                    out.add(new Cell((ItemStack) stackIn.invoke(stacks, i), identifier));
                }
            }
            return out;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unknown) {
            broken = true;
            return List.of();
        }
    }

    /** A public method found on the interfaces too, since the objects are classes of the mod that are not public. */
    private static Method find(Object target, String name, Class<?>... parameters) throws NoSuchMethodException {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Class<?> face : type.getInterfaces()) {
                try {
                    return face.getMethod(name, parameters);
                } catch (NoSuchMethodException next) {
                    // try the next interface
                }
            }
        }
        return target.getClass().getMethod(name, parameters);
    }
}
