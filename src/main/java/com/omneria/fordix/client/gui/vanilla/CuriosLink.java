package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.Fordix;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.Constructor;

/**
 * What the Curios screens say to the server, sent the way Curios' own buttons send it: its network channel and packet classes
 * are reached by name, so Fordix needs no Curios at compile time. Every call does nothing, and says whether it did, when Curios
 * is missing or has changed.
 */
final class CuriosLink {
    private static final String NETWORK = "top.theillusivec4.curios.common.network.";
    private static boolean broken;

    private CuriosLink() {
    }

    static boolean loaded() {
        return !broken && ModList.get().isLoaded("curios");
    }

    /** The accessory screen (the inventory with the bar open): Curios opens its menu on the server and the client follows. */
    static boolean openCurios(ItemStack carried) {
        return send("client.CPacketOpenCurios", new Class<?>[]{ItemStack.class}, carried);
    }

    /** Back to the plain inventory menu. */
    static boolean openVanilla(ItemStack carried) {
        return send("client.CPacketOpenVanilla", new Class<?>[]{ItemStack.class}, carried);
    }

    /** The eye on an accessory slot: shows or hides what it holds on the player's model. */
    static boolean toggleRender(String identifier, int slotIndex) {
        return send("client.CPacketToggleRender", new Class<?>[]{String.class, int.class}, identifier, slotIndex);
    }

    static boolean page(int windowId, boolean next) {
        return send("client.CPacketPage", new Class<?>[]{int.class, boolean.class}, windowId, next);
    }

    static boolean toggleCosmetics(int windowId) {
        return send("client.CPacketToggleCosmetics", new Class<?>[]{int.class}, windowId);
    }

    /** The key that opens and closes the accessory screen in Curios (its own binding, "open Curios GUI"). */
    static boolean isOpenKey(int keyCode, int scanCode) {
        if (!loaded()) {
            return false;
        }
        try {
            Object mapping = Class.forName("top.theillusivec4.curios.client.KeyRegistry").getField("openCurios").get(null);
            return mapping instanceof net.minecraft.client.KeyMapping key
                    && key.isActiveAndMatches(com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unknown) {
            return false;
        }
    }

    // ---- what the accessory menu and its slots say (all optional: a missing member reads as nothing)

    static int intOf(Object menu, String field) {
        try {
            return menu.getClass().getField(field).getInt(menu);
        } catch (ReflectiveOperationException | RuntimeException unknown) {
            return 0;
        }
    }

    static boolean flagOf(Object menu, String field) {
        try {
            return menu.getClass().getField(field).getBoolean(menu);
        } catch (ReflectiveOperationException | RuntimeException unknown) {
            return false;
        }
    }

    /** A no-argument method of a slot or menu returning a boolean, false when it does not exist. */
    static boolean test(Object target, String method) {
        Object value = call(target, method);
        return value instanceof Boolean flag && flag;
    }

    static String text(Object target, String method) {
        return call(target, method) instanceof String text ? text : "";
    }

    private static Object call(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException unknown) {
            return null;
        }
    }

    /** Scrolls the old (legacy) accessory menu to a position between 0 and 1. */
    static void scrollTo(Object menu, float position) {
        try {
            menu.getClass().getMethod("scrollTo", float.class).invoke(menu, position);
        } catch (ReflectiveOperationException | RuntimeException unknown) {
            // nothing to scroll
        }
    }

    /** The cosmetic view of the new menu flips on the client at once, and the server follows the packet. */
    static void flipCosmetics(Object menu, int windowId) {
        try {
            menu.getClass().getMethod("toggleCosmetics").invoke(menu);
        } catch (ReflectiveOperationException | RuntimeException unknown) {
            return;
        }
        toggleCosmetics(windowId);
    }

    private static boolean send(String packet, Class<?>[] types, Object... arguments) {
        if (!loaded()) {
            return false;
        }
        try {
            Object channel = Class.forName(NETWORK + "NetworkHandler").getField("INSTANCE").get(null);
            Constructor<?> constructor = Class.forName(NETWORK + packet).getConstructor(types);
            ((SimpleChannel) channel).send(PacketDistributor.SERVER.noArg(), constructor.newInstance(arguments));
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unknown) {
            Fordix.LOGGER.warn("Fordix: could not send {} to Curios", packet, unknown);
            broken = true;
            return false;
        }
    }
}
