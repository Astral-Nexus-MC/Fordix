package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixAdvancementsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * Swaps vanilla container screens for their Voidframe versions as they open. The
 * replacement reuses the menu the vanilla screen was built with, so the server-side
 * container is untouched. Classes are matched exactly: a subclass means another mod
 * customised that screen, and it's left alone - which also keeps our own screens,
 * themselves vanilla subclasses, from being replaced again.
 */
public final class VanillaScreens {

    private VanillaScreens() {
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!FordixConfig.VOIDFRAME_VANILLA_SCREENS.get()) {
            return;
        }
        Screen replacement = replacementFor(event.getNewScreen());
        if (replacement != null) {
            event.setNewScreen(replacement);
        }
    }

    private static Screen replacementFor(Screen screen) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (screen == null || player == null) {
            return null;
        }
        Inventory inventory = player.getInventory();
        Class<?> type = screen.getClass();
        if (type == ContainerScreen.class) {
            ContainerScreen vanilla = (ContainerScreen) screen;
            return new VoidframeScreens.Chest(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == HopperScreen.class) {
            HopperScreen vanilla = (HopperScreen) screen;
            return new VoidframeScreens.Hopper(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == DispenserScreen.class) {
            DispenserScreen vanilla = (DispenserScreen) screen;
            return new VoidframeScreens.Dispenser(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == ShulkerBoxScreen.class) {
            ShulkerBoxScreen vanilla = (ShulkerBoxScreen) screen;
            return new VoidframeScreens.ShulkerBox(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == CraftingScreen.class) {
            CraftingScreen vanilla = (CraftingScreen) screen;
            return new VoidframeScreens.Crafting(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == FurnaceScreen.class) {
            FurnaceScreen vanilla = (FurnaceScreen) screen;
            return new VoidframeScreens.Furnace(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == BlastFurnaceScreen.class) {
            BlastFurnaceScreen vanilla = (BlastFurnaceScreen) screen;
            return new VoidframeScreens.BlastFurnace(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == SmokerScreen.class) {
            SmokerScreen vanilla = (SmokerScreen) screen;
            return new VoidframeScreens.Smoker(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == InventoryScreen.class) {
            return new VoidframeScreens.PlayerInventory(player);
        }
        if (type == CreativeModeInventoryScreen.class && player.connection != null) {
            return new VoidframeStations.Creative(player, player.connection.enabledFeatures(),
                    Minecraft.getInstance().options.operatorItemsTab().get());
        }
        if (type == LoomScreen.class) {
            LoomScreen vanilla = (LoomScreen) screen;
            return new VoidframeStations.Loom(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == CartographyTableScreen.class) {
            CartographyTableScreen vanilla = (CartographyTableScreen) screen;
            return new VoidframeStations.Cartography(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == AdvancementsScreen.class) {
            return new FordixAdvancementsScreen(player.connection.getAdvancements());
        }
        if (type == GrindstoneScreen.class) {
            GrindstoneScreen vanilla = (GrindstoneScreen) screen;
            return new VoidframeStations.Grindstone(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == AnvilScreen.class) {
            AnvilScreen vanilla = (AnvilScreen) screen;
            return new VoidframeStations.Anvil(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == BrewingStandScreen.class) {
            BrewingStandScreen vanilla = (BrewingStandScreen) screen;
            return new VoidframeStations.BrewingStand(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == BeaconScreen.class) {
            BeaconScreen vanilla = (BeaconScreen) screen;
            return new VoidframeStations.Beacon(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == EnchantmentScreen.class) {
            EnchantmentScreen vanilla = (EnchantmentScreen) screen;
            return new VoidframeStations.Enchanting(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == MerchantScreen.class) {
            MerchantScreen vanilla = (MerchantScreen) screen;
            return new VoidframeStations.Merchant(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == StonecutterScreen.class) {
            StonecutterScreen vanilla = (StonecutterScreen) screen;
            return new VoidframeStations.Stonecutter(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == SmithingScreen.class) {
            SmithingScreen vanilla = (SmithingScreen) screen;
            return new VoidframeStations.Smithing(vanilla.getMenu(), inventory, vanilla.getTitle());
        }
        if (type == HorseInventoryScreen.class) {
            HorseInventoryScreen vanilla = (HorseInventoryScreen) screen;
            // The horse only lives in a private field; without it, leave the vanilla screen in place.
            if (Reflect.get(VoidframeStations.Horse.HORSE, vanilla) instanceof AbstractHorse horse) {
                return new VoidframeStations.Horse(vanilla.getMenu(), inventory, horse);
            }
        }
        return null;
    }
}
