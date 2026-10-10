package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.hud.HudStyle;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.Glass;
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
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            VoidframeDraw.fillCount(guiGraphics, font, menu, menu.getRowCount() * 9, imageWidth, titleLabelY);
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
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            VoidframeDraw.fillCount(guiGraphics, font, menu, 5, imageWidth, titleLabelY);
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
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            VoidframeDraw.fillCount(guiGraphics, font, menu, 9, imageWidth, titleLabelY);
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
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            VoidframeDraw.fillCount(guiGraphics, font, menu, 27, imageWidth, titleLabelY);
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
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    public static class Furnace extends FurnaceScreen implements VoidframeScreen {
        private static final int COOK_TICKS = 200;
        public Furnace(FurnaceMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            furnaceBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight, COOK_TICKS);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    public static class BlastFurnace extends BlastFurnaceScreen implements VoidframeScreen {
        private static final int COOK_TICKS = 100;
        public BlastFurnace(BlastFurnaceMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            furnaceBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight, COOK_TICKS);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    public static class Smoker extends SmokerScreen implements VoidframeScreen {
        private static final int COOK_TICKS = 100;
        public Smoker(SmokerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            furnaceBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight, COOK_TICKS);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    /** The survival inventory (E key). Creative mode uses its own screen, which stays vanilla. */
    public static class PlayerInventory extends InventoryScreen implements VoidframeScreen {
        /**
         * The panel keeps vanilla's width, so the recipe book sits right beside it. JEI lays its item
         * list out from {@link #getXSize()}, which reports the width plus the guide tab column so
         * the list stays clear of the tabs; clicks on the tabs don't count as clicks outside.
         */
        public PlayerInventory(Player player) {
            super(player);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        public int panelWidth() {
            return imageWidth;
        }

        @Override
        public int getXSize() {
            return imageWidth + GuideTabs.stripWidth();
        }

        @Override
        protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
            boolean onTabs = mouseX >= guiLeft + imageWidth && mouseX < guiLeft + getXSize()
                    && mouseY >= guiTop && mouseY < guiTop + imageHeight;
            return !onTabs && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.well(guiGraphics, leftPos + 26, topPos + 8, 50, 70);
            VoidframeDraw.arrow(guiGraphics, leftPos + 134, topPos + 31, 13, 11, 13);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, leftPos + 51, topPos + 75, 30,
                    (float) (leftPos + 51 - mouseX), (float) (topPos + 75 - 50 - mouseY), minecraft.player);
            statsCard(guiGraphics);
        }

        /** Armor and experience as two bars, in a small card on the left of the panel (hidden when the recipe book takes that place). */
        private void statsCard(GuiGraphics guiGraphics) {
            int width = 96;
            int x = leftPos - width - 6;
            if (x < 4 || getRecipeBookComponent().isVisible() || minecraft.player == null) {
                return;
            }
            int y = topPos;
            Glass.panel(guiGraphics, x, y, width, 56);
            int armor = minecraft.player.getArmorValue();
            Glass.label(guiGraphics, font, Tr.s("inv.armor"), x + 10, y + 8);
            String armorText = armor + " / 20";
            guiGraphics.drawString(font, armorText, x + width - 8 - font.width(armorText), y + 8, FordixPalette.TEXT_BRIGHT, false);
            HudStyle.bar(guiGraphics, x + 10, y + 19, width - 20, 5, Math.min(1.0F, armor / 20.0F), 0xFF6FD6E0);
            Glass.label(guiGraphics, font, Tr.s("inv.xp"), x + 10, y + 31);
            String levelText = Tr.s("inv.level", minecraft.player.experienceLevel);
            guiGraphics.drawString(font, levelText, x + width - 8 - font.width(levelText), y + 31, 0xFF9BF0B9, false);
            HudStyle.bar(guiGraphics, x + 10, y + 42, width - 20, 5, minecraft.player.experienceProgress, 0xFF5ED68A);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
        }
    }

    /** Shared by the three furnace kinds: fuel gauge between the input and fuel slots, progress arrow to the result. */
    private static void furnaceBackground(GuiGraphics guiGraphics, AbstractFurnaceMenu menu,
                                          int left, int top, int width, int height, int cookTicks) {
        VoidframeDraw.containerBackground(guiGraphics, menu, left, top, width, height);
        VoidframeDraw.titleBand(guiGraphics, left, top, width);
        int fuel = menu.isLit() ? menu.getLitProgress() + 1 : 0;
        VoidframeDraw.flame(guiGraphics, left + 57, top + 37, 12, 14, fuel);
        VoidframeDraw.arrow(guiGraphics, left + 79, top + 34, 24, 17, menu.getBurnProgress());
        // The time left on the item being cooked, in clear text under the arrow (the arrow is 24 steps for the whole cook).
        int burn = menu.getBurnProgress();
        if (burn > 0 && burn < 24) {
            int seconds = Math.max(1, Math.round((24 - burn) / 24.0F * cookTicks / 20.0F));
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            String text = seconds + " s";
            guiGraphics.drawString(font, text, left + 91 - font.width(text) / 2, top + 53, Glass.dim(), false);
        }
    }
}
