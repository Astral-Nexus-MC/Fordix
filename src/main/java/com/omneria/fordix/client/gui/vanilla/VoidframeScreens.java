package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.guide.GuideTabs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.SmokerMenu;

/**
 * Voidframe versions of vanilla container screens. Each one subclasses the vanilla
 * screen and only swaps what gets drawn (background and labels): slots, clicks,
 * tooltips and the recipe book all stay vanilla. Arrow and gauge positions match
 * where vanilla's textures put them, so items still line up.
 */
public final class VoidframeScreens {

    private VoidframeScreens() {
    }

    /** Every chest-like 9-wide container: single and double chests, barrels, ender chests, chest minecarts. */
    public static class Chest extends ContainerScreen implements VoidframeScreen {
        public Chest(ChestMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    public static class Hopper extends HopperScreen implements VoidframeScreen {
        public Hopper(HopperMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    /** Dispensers and droppers share this screen. */
    public static class Dispenser extends DispenserScreen implements VoidframeScreen {
        public Dispenser(DispenserMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    public static class ShulkerBox extends ShulkerBoxScreen implements VoidframeScreen {
        public ShulkerBox(ShulkerBoxMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    public static class Crafting extends CraftingScreen implements VoidframeScreen {
        public Crafting(CraftingMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
            VoidframeDraw.arrow(guiGraphics, leftPos + 90, topPos + 35, 22, 15, 22);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    public static class Furnace extends FurnaceScreen implements VoidframeScreen {
        public Furnace(FurnaceMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            furnaceBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    public static class BlastFurnace extends BlastFurnaceScreen implements VoidframeScreen {
        public BlastFurnace(BlastFurnaceMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            furnaceBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    public static class Smoker extends SmokerScreen implements VoidframeScreen {
        public Smoker(SmokerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            furnaceBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    /** The survival inventory (E key). Creative mode uses its own screen, which stays vanilla. */
    public static class PlayerInventory extends InventoryScreen implements VoidframeScreen {
        private final int panelWidth;

        /**
         * The declared width includes the guide tab column, so JEI (which lays out
         * from it) keeps its item list clear of the tabs, and clicks on them don't
         * count as clicks outside the inventory. The panel itself keeps vanilla's width.
         */
        public PlayerInventory(Player player) {
            super(player);
            slotColor = FordixPalette.SLOT_HOVER;
            panelWidth = imageWidth;
            imageWidth += GuideTabs.stripWidth();
        }

        public int panelWidth() {
            return panelWidth;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, panelWidth, imageHeight);
            VoidframeDraw.well(guiGraphics, leftPos + 26, topPos + 8, 50, 70);
            VoidframeDraw.arrow(guiGraphics, leftPos + 134, topPos + 31, 13, 11, 13);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, leftPos + 51, topPos + 75, 30,
                    (float) (leftPos + 51 - mouseX), (float) (topPos + 75 - 50 - mouseY), minecraft.player);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
        }
    }

    /** Shared by the three furnace kinds: fuel gauge between the input and fuel slots, progress arrow to the result. */
    private static void furnaceBackground(GuiGraphics guiGraphics, AbstractFurnaceMenu menu,
                                          int left, int top, int width, int height) {
        VoidframeDraw.containerBackground(guiGraphics, menu, left, top, width, height);
        VoidframeDraw.titleBand(guiGraphics, left, top, width);
        int fuel = menu.isLit() ? menu.getLitProgress() + 1 : 0;
        VoidframeDraw.flame(guiGraphics, left + 57, top + 37, 12, 14, fuel);
        VoidframeDraw.arrow(guiGraphics, left + 79, top + 34, 24, 17, menu.getBurnProgress());
    }
}
