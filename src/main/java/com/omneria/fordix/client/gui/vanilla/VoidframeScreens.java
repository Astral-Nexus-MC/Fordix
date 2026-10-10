package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.hud.HudStyle;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.guide.GuideTabs;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.inventory.AbstractContainerMenu;
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

    // ---------- the wide layout (mockup), shared by the chest-like containers ----------

    /**
     * Whether a container with a recipe book gets the wide layout: the panel must fit, with the open book beside it while it is
     * open (when it does not fit, the screen goes back to the compact layout, where the book fits as it always did).
     */
    private static boolean wantsWide(net.minecraft.client.gui.screens.Screen screen, int width, int height,
                                     net.minecraft.world.inventory.RecipeBookType book) {
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        boolean open = player != null && player.getRecipeBook().isOpen(book);
        return ContainerLayout.fits(screen, width + (open ? ContainerLayout.BOOK_ROOM : 0), height)
                && ContainerLayout.ready(((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) screen).getMenu());
    }

    private static final int WIDE_W = 218;
    private static final int GRID_Y = 56;

    private static int chestDivider(int rows) {
        return GRID_Y + ContainerLayout.PITCH * rows + 10;
    }

    private static int chestHeight(int rows) {
        return chestDivider(rows) + 168;
    }

    /** Places a chest-like container in the wide layout: its slots in a grid, then the player's bag and hotbar. */
    private static void placeChestLike(AbstractContainerMenu menu, int count, int columns, int gridX, int rows) {
        int divider = chestDivider(rows);
        ContainerLayout.grid(menu, count, columns, gridX, GRID_Y);
        ContainerLayout.playerBlock(menu, 10, divider + 26, divider + 26 + 76);
    }

    private static void wideChestBackground(GuiGraphics g, AbstractContainerMenu menu, WideLayout screen, int left, int top, int rows,
                                            net.minecraft.client.gui.Font font) {
        VoidframeDraw.wideBackground(g, menu, screen, left, top, WIDE_W, chestHeight(rows));
        int divider = chestDivider(rows);
        Glass.rule(g, left + 10, top + divider, WIDE_W - 20);
        g.drawString(font, Tr.s("container.hint"), left + 12, top + divider + 26 + 76 + 38, Glass.dim(), false);
    }

    private static void wideChestLabels(GuiGraphics g, net.minecraft.client.gui.Font font, Component title, Component inventory,
                                        AbstractContainerMenu menu, int slots, int rows) {
        VoidframeDraw.title(g, font, title, 14, 9);
        VoidframeDraw.fillCount(g, font, menu, slots, WIDE_W + 6, 9);
        VoidframeDraw.label(g, font, inventory, 12, chestDivider(rows) + 10);
    }

    /** Every chest-like 9-wide container: single and double chests, barrels, ender chests, chest minecarts. */
    public static class Chest extends ContainerScreen implements VoidframeScreen, WideLayout {
        private final int rows;
        private boolean wide;

        public Chest(ChestMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
            rows = menu.getRowCount();
        }

        @Override
        public boolean wide() {
            return wide;
        }

        @Override
        public int[] searchRect() {
            return wide ? new int[]{154, 28, 54, 20} : null;
        }

        @Override
        public int[] sortRect() {
            return wide ? new int[]{106, 28, 44, 20} : null;
        }

        @Override
        protected void init() {
            wide = ContainerLayout.fits(this, WIDE_W, chestHeight(rows)) && ContainerLayout.ready(menu);
            imageWidth = wide ? WIDE_W : 176;
            imageHeight = wide ? chestHeight(rows) : 114 + rows * 18;
            super.init();
            if (wide) {
                placeChestLike(menu, rows * 9, 9, 10, rows);
            } else {
                ContainerLayout.restore(menu);
            }
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            if (wide) {
                wideChestBackground(guiGraphics, menu, this, leftPos, topPos, rows, font);
                return;
            }
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            if (wide) {
                wideChestLabels(guiGraphics, font, title, playerInventoryTitle, menu, rows * 9, rows);
                return;
            }
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            VoidframeDraw.fillCount(guiGraphics, font, menu, menu.getRowCount() * 9, imageWidth, titleLabelY);
        }
    }

    public static class Hopper extends HopperScreen implements VoidframeScreen, WideLayout {
        private boolean wide;

        public Hopper(HopperMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        public boolean wide() {
            return wide;
        }

        @Override
        protected void init() {
            wide = ContainerLayout.fits(this, WIDE_W, chestHeight(1)) && ContainerLayout.ready(menu);
            imageWidth = wide ? WIDE_W : 176;
            imageHeight = wide ? chestHeight(1) : 133;
            super.init();
            if (wide) {
                placeChestLike(menu, 5, 5, (WIDE_W - 5 * ContainerLayout.PITCH) / 2, 1);
            } else {
                ContainerLayout.restore(menu);
            }
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            if (wide) {
                wideChestBackground(guiGraphics, menu, this, leftPos, topPos, 1, font);
                return;
            }
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            if (wide) {
                wideChestLabels(guiGraphics, font, title, playerInventoryTitle, menu, 5, 1);
                return;
            }
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            VoidframeDraw.fillCount(guiGraphics, font, menu, 5, imageWidth, titleLabelY);
        }
    }

    /** Dispensers and droppers share this screen. */
    public static class Dispenser extends DispenserScreen implements VoidframeScreen, WideLayout {
        private boolean wide;

        public Dispenser(DispenserMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        public boolean wide() {
            return wide;
        }

        @Override
        protected void init() {
            wide = ContainerLayout.fits(this, WIDE_W, chestHeight(3)) && ContainerLayout.ready(menu);
            imageWidth = wide ? WIDE_W : 176;
            imageHeight = wide ? chestHeight(3) : 166;
            super.init();
            if (wide) {
                placeChestLike(menu, 9, 3, (WIDE_W - 3 * ContainerLayout.PITCH) / 2, 3);
            } else {
                ContainerLayout.restore(menu);
            }
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            if (wide) {
                wideChestBackground(guiGraphics, menu, this, leftPos, topPos, 3, font);
                return;
            }
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            if (wide) {
                wideChestLabels(guiGraphics, font, title, playerInventoryTitle, menu, 9, 3);
                return;
            }
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            VoidframeDraw.fillCount(guiGraphics, font, menu, 9, imageWidth, titleLabelY);
        }
    }

    public static class ShulkerBox extends ShulkerBoxScreen implements VoidframeScreen, WideLayout {
        private boolean wide;

        public ShulkerBox(ShulkerBoxMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        public boolean wide() {
            return wide;
        }

        @Override
        public int[] searchRect() {
            return wide ? new int[]{154, 28, 54, 20} : null;
        }

        @Override
        public int[] sortRect() {
            return wide ? new int[]{106, 28, 44, 20} : null;
        }

        @Override
        protected void init() {
            wide = ContainerLayout.fits(this, WIDE_W, chestHeight(3)) && ContainerLayout.ready(menu);
            imageWidth = wide ? WIDE_W : 176;
            imageHeight = wide ? chestHeight(3) : 167;
            super.init();
            if (wide) {
                placeChestLike(menu, 27, 9, 10, 3);
            } else {
                ContainerLayout.restore(menu);
            }
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            if (wide) {
                wideChestBackground(guiGraphics, menu, this, leftPos, topPos, 3, font);
                return;
            }
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            if (wide) {
                wideChestLabels(guiGraphics, font, title, playerInventoryTitle, menu, 27, 3);
                return;
            }
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            VoidframeDraw.fillCount(guiGraphics, font, menu, 27, imageWidth, titleLabelY);
        }
    }

    // ---------- crafting table ----------

    private static final int CRAFT_H = 244;

    public static class Crafting extends CraftingScreen implements VoidframeScreen, WideLayout {
        private boolean wide;

        public Crafting(CraftingMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        public boolean wide() {
            return wide;
        }

        @Override
        public int[] boxOf(net.minecraft.world.inventory.Slot slot) {
            if (!wide) {
                return new int[]{1, 18};
            }
            return slot instanceof net.minecraft.world.inventory.ResultSlot ? new int[]{5, 26} : new int[]{2, 20};
        }

        private boolean lastBook;

        /** The wide panel with the recipe book open: the book to its left, both centered, the buttons following the panel. */
        private void placeWide() {
            net.minecraft.client.gui.screens.recipebook.RecipeBookComponent book = getRecipeBookComponent();
            boolean open = book.isVisible();
            int[] at = ContainerLayout.bookLayout(this.width, imageWidth, open);
            leftPos = at[0];
            if (open != lastBook) {
                lastBook = open;
                book.init(at[1], this.height, minecraft, false, menu);
            }
            for (net.minecraft.client.gui.components.events.GuiEventListener child : children()) {
                if (child instanceof net.minecraft.client.gui.components.ImageButton button) {
                    button.setX(leftPos + 176);
                    button.setY(topPos + 40);
                }
            }
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // The recipe book opening or closing can change whether the wide layout still has room: lay out again.
            if (wantsWide(this, WIDE_W, CRAFT_H, net.minecraft.world.inventory.RecipeBookType.CRAFTING) != wide) {
                rebuildWidgets();
            }
            if (wide) {
                placeWide();
            }
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void init() {
            wide = wantsWide(this, WIDE_W, CRAFT_H, net.minecraft.world.inventory.RecipeBookType.CRAFTING);
            imageWidth = wide ? WIDE_W : 176;
            imageHeight = wide ? CRAFT_H : 166;
            super.init();
            if (wide) {
                int p = ContainerLayout.PITCH;
                for (net.minecraft.world.inventory.Slot slot : menu.slots) {
                    if (slot instanceof net.minecraft.world.inventory.ResultSlot) {
                        ContainerLayout.put(slot, 132, 62, ContainerLayout.BIG);
                    }
                }
                for (int i = 1; i <= 9; i++) {
                    ContainerLayout.put(menu.getSlot(i), 16 + (i - 1) % 3 * p, 40 + (i - 1) / 3 * p, ContainerLayout.BOX);
                }
                ContainerLayout.playerBlock(menu, 10, 138, 214);
                // The recipe book toggle sits at the right of the grid.
                for (net.minecraft.client.gui.components.events.GuiEventListener child : children()) {
                    if (child instanceof net.minecraft.client.gui.components.ImageButton button) {
                        button.setX(leftPos + 176);
                        button.setY(topPos + 40);
                    }
                }
            } else {
                ContainerLayout.restore(menu);
            }
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            if (wide) {
                VoidframeDraw.wideBackground(guiGraphics, menu, this, leftPos, topPos, imageWidth, imageHeight);
                Glass.rule(guiGraphics, leftPos + 10, topPos + 112, WIDE_W - 20);
                VoidframeDraw.arrow(guiGraphics, leftPos + 96, topPos + 66, 28, 14, 28);
                return;
            }
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
            VoidframeDraw.arrow(guiGraphics, leftPos + 90, topPos + 35, 22, 15, 22);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            if (wide) {
                VoidframeDraw.title(guiGraphics, font, title, 14, 9);
                VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, 12, 122);
                return;
            }
            VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    // ---------- the survival inventory ----------

    private static final int INV_W = 600;
    private static final int INV_H = 388;

    /** The survival inventory (E key). Creative mode uses its own screen, which stays vanilla. */
    public static class PlayerInventory extends InventoryScreen implements VoidframeScreen, WideLayout {
        private boolean wide;

        /**
         * JEI lays its item list out from {@link #getXSize()}, which reports the width plus the guide tab column so
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
        public boolean wide() {
            return wide;
        }

        @Override
        public int[] boxOf(net.minecraft.world.inventory.Slot slot) {
            if (!wide) {
                return new int[]{1, 18};
            }
            boolean armorOrHand = slot.container instanceof Inventory && slot.getContainerSlot() >= 36 && slot.getContainerSlot() <= 40;
            return slot instanceof net.minecraft.world.inventory.ResultSlot || armorOrHand ? new int[]{5, 26} : new int[]{2, 20};
        }

        @Override
        public int[] searchRect() {
            return wide ? new int[]{432, 262, 140, 22} : null;
        }

        @Override
        public int[] sortRect() {
            return wide ? new int[]{432, 290, 66, 22} : null;
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

        private boolean lastBook;

        /** The wide panel with the recipe book open: the book to its left, both centered, the buttons following the panel. */
        private void placeWide() {
            net.minecraft.client.gui.screens.recipebook.RecipeBookComponent book = getRecipeBookComponent();
            boolean open = book.isVisible();
            int[] at = ContainerLayout.bookLayout(this.width, imageWidth, open);
            leftPos = at[0];
            if (open != lastBook) {
                lastBook = open;
                book.init(at[1], this.height, minecraft, false, menu);
            }
            for (net.minecraft.client.gui.components.events.GuiEventListener child : children()) {
                if (child instanceof net.minecraft.client.gui.components.ImageButton button) {
                    button.setX(leftPos + 506);
                    button.setY(topPos + 290);
                    button.setWidth(66);
                    button.setHeight(22);
                }
            }
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // The recipe book opening or closing can change whether the wide layout still has room: lay out again.
            if (wantsWide(this, INV_W, INV_H, net.minecraft.world.inventory.RecipeBookType.CRAFTING) != wide) {
                rebuildWidgets();
            }
            if (wide) {
                placeWide();
            }
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void init() {
            wide = wantsWide(this, INV_W, INV_H, net.minecraft.world.inventory.RecipeBookType.CRAFTING);
            imageWidth = wide ? INV_W : 176;
            imageHeight = wide ? INV_H : 166;
            super.init();
            if (!wide) {
                ContainerLayout.restore(menu);
                return;
            }
            int p = ContainerLayout.PITCH;
            for (net.minecraft.world.inventory.Slot slot : menu.slots) {
                if (slot instanceof net.minecraft.world.inventory.ResultSlot) {
                    ContainerLayout.put(slot, 344, 285, ContainerLayout.BIG);
                } else if (slot.container instanceof Inventory) {
                    int index = slot.getContainerSlot();
                    if (index >= 36 && index <= 39) {
                        // Head, chest, legs, feet down the left column, with room for the name of each piece.
                        ContainerLayout.put(slot, 18, 50 + (39 - index) * 32, ContainerLayout.BIG);
                    } else if (index == 40) {
                        ContainerLayout.put(slot, 18, 186, ContainerLayout.BIG);
                    }
                }
            }
            for (int i = 1; i <= 4; i++) {
                ContainerLayout.put(menu.getSlot(i), 258 + (i - 1) % 2 * p, 276 + (i - 1) / 2 * p, ContainerLayout.BOX);
            }
            ContainerLayout.playerBlock(menu, 18, 264, 352);
            for (net.minecraft.client.gui.components.events.GuiEventListener child : children()) {
                if (child instanceof net.minecraft.client.gui.components.ImageButton button) {
                    button.setX(leftPos + 506);
                    button.setY(topPos + 290);
                    button.setWidth(66);
                    button.setHeight(22);
                }
            }
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            if (wide) {
                wideBackground(guiGraphics, mouseX, mouseY);
                return;
            }
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.well(guiGraphics, leftPos + 26, topPos + 8, 50, 70);
            VoidframeDraw.arrow(guiGraphics, leftPos + 134, topPos + 31, 13, 11, 13);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, leftPos + 51, topPos + 75, 30,
                    (float) (leftPos + 51 - mouseX), (float) (topPos + 75 - 50 - mouseY), minecraft.player);
            statsCard(guiGraphics);
        }

        private void wideBackground(GuiGraphics g, int mouseX, int mouseY) {
            VoidframeDraw.wideBackground(g, menu, this, leftPos, topPos, imageWidth, imageHeight);
            // The player in a deep well with a soft light from below.
            VoidframeDraw.well(g, leftPos + 156, topPos + 46, 148, 168);
            g.fillGradient(leftPos + 157, topPos + 140, leftPos + 303, topPos + 213, 0x00000000,
                    com.omneria.fordix.client.hud.HudTokens.withAlpha(FordixPalette.ACCENT, 0x28));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, leftPos + 230, topPos + 204, 56,
                    (float) (leftPos + 230 - mouseX), (float) (topPos + 204 - 84 - mouseY), minecraft.player);
            Glass.card(g, leftPos + 246, topPos + 248, 164, 124);
            Glass.card(g, leftPos + 420, topPos + 248, 164, 124);
            VoidframeDraw.arrow(g, leftPos + 312, topPos + 292, 24, 12, 24);
            if (minecraft.player != null) {
                int selected = minecraft.player.getInventory().selected;
                int bx = leftPos + 18 + selected * ContainerLayout.PITCH - 1;
                int by = topPos + 352 - 1;
                int line = 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF);
                g.fill(bx, by, bx + 22, by + 1, line);
                g.fill(bx, by + 21, bx + 22, by + 22, line);
                g.fill(bx, by + 1, bx + 1, by + 21, line);
                g.fill(bx + 21, by + 1, bx + 22, by + 21, line);
                bigStats(g);
            }
        }

        /** The experience under the player and the attributes of the game (the ones other mods add come as more rows). */
        private void bigStats(GuiGraphics g) {
            net.minecraft.world.entity.player.Player player = minecraft.player;
            int levelX = leftPos + 156;
            int levelY = topPos + 222;
            g.drawString(font, Tr.s("inv.level", player.experienceLevel).toUpperCase(java.util.Locale.ROOT), levelX, levelY, 0xFF9BF0B9, false);
            String percent = Math.round(player.experienceProgress * 100) + " %";
            g.drawString(font, percent, levelX + 148 - font.width(percent), levelY, Glass.dim(), false);
            HudStyle.bar(g, levelX, levelY + 12, 148, 5, player.experienceProgress, 0xFF5ED68A);

            int x = leftPos + 322;
            int y = topPos + 46;
            Glass.card(g, x, y, 262, 190);
            Object[][] rows = {
                    {"inv.attr.health", net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 40.0D, 0xFFE5534B, 0},
                    {"inv.armor", net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 20.0D, 0xFF6FE0E6, 0},
                    {"inv.attr.damage", net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 20.0D, 0xFFB45FE0, 1},
                    {"inv.attr.attack_speed", net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, 4.0D, 0xFFE8A64A, 1},
                    {"inv.attr.movement", net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.3D, 0xFF5ED68A, 2}};
            for (int i = 0; i < rows.length; i++) {
                Object[] row = rows[i];
                double value = player.getAttributeValue((net.minecraft.world.entity.ai.attributes.Attribute) row[1]);
                int decimals = (Integer) row[4];
                String text = decimals == 0 ? Integer.toString((int) Math.round(value)) : String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
                int rowY = y + 12 + i * 35;
                g.drawString(font, Tr.s((String) row[0]), x + 12, rowY, FordixPalette.TEXT, false);
                g.drawString(font, text, x + 250 - font.width(text), rowY, FordixPalette.TEXT_BRIGHT, false);
                HudStyle.bar(g, x + 12, rowY + 13, 238, 6, (float) Math.min(1.0D, value / (Double) row[2]), (Integer) row[3]);
            }
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
            if (!wide) {
                VoidframeDraw.title(guiGraphics, font, title, titleLabelX, titleLabelY);
                return;
            }
            VoidframeDraw.title(guiGraphics, font, Component.translatable("fordix.inv.title"), 14, 9);
            if (minecraft.player != null) {
                String right = (minecraft.player.getName().getString() + " \u00B7 " + Tr.s("inv.level", minecraft.player.experienceLevel))
                        .toUpperCase(java.util.Locale.ROOT);
                guiGraphics.drawString(font, right, INV_W - 14 - font.width(right), 9, Glass.dim(), false);
            }
            VoidframeDraw.label(guiGraphics, font, Component.translatable("fordix.inv.equipment"), 18, 34);
            VoidframeDraw.label(guiGraphics, font, Component.translatable("fordix.inv.craft"), 258, 256);
            VoidframeDraw.label(guiGraphics, font, Component.translatable("fordix.inv.bag"), 18, 250);
            VoidframeDraw.label(guiGraphics, font, Component.translatable("fordix.inv.hotbar"), 18, 338);
            // The name of each worn piece beside its square (what the square is for while it is empty).
            if (minecraft.player != null) {
                String[] keys = {"head", "chest", "legs", "feet"};
                net.minecraft.world.entity.EquipmentSlot[] parts = {net.minecraft.world.entity.EquipmentSlot.HEAD,
                        net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                        net.minecraft.world.entity.EquipmentSlot.FEET};
                for (int i = 0; i < 4; i++) {
                    drawWorn(guiGraphics, minecraft.player.getItemBySlot(parts[i]), "inv.slot." + keys[i], 50 + i * 32);
                }
                drawWorn(guiGraphics, minecraft.player.getOffhandItem(), "inv.slot.offhand", 186);
            }
        }

        private void drawWorn(GuiGraphics g, net.minecraft.world.item.ItemStack stack, String emptyKey, int boxY) {
            boolean empty = stack.isEmpty();
            String text = font.plainSubstrByWidth(empty ? Tr.s(emptyKey) : stack.getHoverName().getString(), 92);
            g.drawString(font, text, 52, boxY + 9, empty ? Glass.dim() : FordixPalette.TEXT, false);
        }
    }

    // ---------- furnaces ----------

    private static final int FURNACE_H = 252;

    /** Places a furnace-like menu (input, fuel, result, then the player's blocks) in the wide layout. */
    private static void placeFurnace(AbstractContainerMenu menu) {
        ContainerLayout.put(menu.getSlot(0), 20, 52, ContainerLayout.BOX);
        ContainerLayout.put(menu.getSlot(1), 20, 96, ContainerLayout.BOX);
        ContainerLayout.put(menu.getSlot(2), 136, 66, ContainerLayout.BIG);
        ContainerLayout.playerBlock(menu, 10, 144, 218);
    }

    private static int[] furnaceBox(boolean wide, net.minecraft.world.inventory.Slot slot) {
        if (!wide) {
            return new int[]{1, 18};
        }
        return slot instanceof net.minecraft.world.inventory.FurnaceResultSlot ? new int[]{5, 26} : new int[]{2, 20};
    }

    private static void wideFurnaceRecipeButton(Object screen, int left, int top, java.util.List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children) {
        for (net.minecraft.client.gui.components.events.GuiEventListener child : children) {
            if (child instanceof net.minecraft.client.gui.components.ImageButton button) {
                button.setX(left + 176);
                button.setY(top + 52);
            }
        }
    }

    public static class Furnace extends FurnaceScreen implements VoidframeScreen, WideLayout {
        private static final int COOK_TICKS = 200;
        private boolean wide;

        public Furnace(FurnaceMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        public boolean wide() {
            return wide;
        }

        @Override
        public int[] boxOf(net.minecraft.world.inventory.Slot slot) {
            return furnaceBox(wide, slot);
        }

        private boolean lastBook;

        /** The wide panel with the recipe book open: the book to its left, both centered, the buttons following the panel. */
        private void placeWide() {
            net.minecraft.client.gui.screens.recipebook.RecipeBookComponent book = getRecipeBookComponent();
            boolean open = book.isVisible();
            int[] at = ContainerLayout.bookLayout(this.width, imageWidth, open);
            leftPos = at[0];
            if (open != lastBook) {
                lastBook = open;
                book.init(at[1], this.height, minecraft, false, menu);
            }
            wideFurnaceRecipeButton(this, leftPos, topPos, children());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // The recipe book opening or closing can change whether the wide layout still has room: lay out again.
            if (wantsWide(this, WIDE_W, FURNACE_H, net.minecraft.world.inventory.RecipeBookType.FURNACE) != wide) {
                rebuildWidgets();
            }
            if (wide) {
                placeWide();
            }
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void init() {
            wide = wantsWide(this, WIDE_W, FURNACE_H, net.minecraft.world.inventory.RecipeBookType.FURNACE);
            imageWidth = wide ? WIDE_W : 176;
            imageHeight = wide ? FURNACE_H : 166;
            super.init();
            if (wide) {
                placeFurnace(menu);
                wideFurnaceRecipeButton(this, leftPos, topPos, children());
            } else {
                ContainerLayout.restore(menu);
            }
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            furnaceBackground(guiGraphics, menu, this, leftPos, topPos, imageWidth, imageHeight, COOK_TICKS, wide);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            furnaceLabels(guiGraphics, font, menu, title, playerInventoryTitle, titleLabelX, titleLabelY, inventoryLabelX, inventoryLabelY, wide);
        }
    }

    public static class BlastFurnace extends BlastFurnaceScreen implements VoidframeScreen, WideLayout {
        private static final int COOK_TICKS = 100;
        private boolean wide;

        public BlastFurnace(BlastFurnaceMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        public boolean wide() {
            return wide;
        }

        @Override
        public int[] boxOf(net.minecraft.world.inventory.Slot slot) {
            return furnaceBox(wide, slot);
        }

        private boolean lastBook;

        /** The wide panel with the recipe book open: the book to its left, both centered, the buttons following the panel. */
        private void placeWide() {
            net.minecraft.client.gui.screens.recipebook.RecipeBookComponent book = getRecipeBookComponent();
            boolean open = book.isVisible();
            int[] at = ContainerLayout.bookLayout(this.width, imageWidth, open);
            leftPos = at[0];
            if (open != lastBook) {
                lastBook = open;
                book.init(at[1], this.height, minecraft, false, menu);
            }
            wideFurnaceRecipeButton(this, leftPos, topPos, children());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // The recipe book opening or closing can change whether the wide layout still has room: lay out again.
            if (wantsWide(this, WIDE_W, FURNACE_H, net.minecraft.world.inventory.RecipeBookType.BLAST_FURNACE) != wide) {
                rebuildWidgets();
            }
            if (wide) {
                placeWide();
            }
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void init() {
            wide = wantsWide(this, WIDE_W, FURNACE_H, net.minecraft.world.inventory.RecipeBookType.BLAST_FURNACE);
            imageWidth = wide ? WIDE_W : 176;
            imageHeight = wide ? FURNACE_H : 166;
            super.init();
            if (wide) {
                placeFurnace(menu);
                wideFurnaceRecipeButton(this, leftPos, topPos, children());
            } else {
                ContainerLayout.restore(menu);
            }
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            furnaceBackground(guiGraphics, menu, this, leftPos, topPos, imageWidth, imageHeight, COOK_TICKS, wide);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            furnaceLabels(guiGraphics, font, menu, title, playerInventoryTitle, titleLabelX, titleLabelY, inventoryLabelX, inventoryLabelY, wide);
        }
    }

    public static class Smoker extends SmokerScreen implements VoidframeScreen, WideLayout {
        private static final int COOK_TICKS = 100;
        private boolean wide;

        public Smoker(SmokerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        public boolean wide() {
            return wide;
        }

        @Override
        public int[] boxOf(net.minecraft.world.inventory.Slot slot) {
            return furnaceBox(wide, slot);
        }

        private boolean lastBook;

        /** The wide panel with the recipe book open: the book to its left, both centered, the buttons following the panel. */
        private void placeWide() {
            net.minecraft.client.gui.screens.recipebook.RecipeBookComponent book = getRecipeBookComponent();
            boolean open = book.isVisible();
            int[] at = ContainerLayout.bookLayout(this.width, imageWidth, open);
            leftPos = at[0];
            if (open != lastBook) {
                lastBook = open;
                book.init(at[1], this.height, minecraft, false, menu);
            }
            wideFurnaceRecipeButton(this, leftPos, topPos, children());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // The recipe book opening or closing can change whether the wide layout still has room: lay out again.
            if (wantsWide(this, WIDE_W, FURNACE_H, net.minecraft.world.inventory.RecipeBookType.SMOKER) != wide) {
                rebuildWidgets();
            }
            if (wide) {
                placeWide();
            }
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void init() {
            wide = wantsWide(this, WIDE_W, FURNACE_H, net.minecraft.world.inventory.RecipeBookType.SMOKER);
            imageWidth = wide ? WIDE_W : 176;
            imageHeight = wide ? FURNACE_H : 166;
            super.init();
            if (wide) {
                placeFurnace(menu);
                wideFurnaceRecipeButton(this, leftPos, topPos, children());
            } else {
                ContainerLayout.restore(menu);
            }
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            furnaceBackground(guiGraphics, menu, this, leftPos, topPos, imageWidth, imageHeight, COOK_TICKS, wide);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            furnaceLabels(guiGraphics, font, menu, title, playerInventoryTitle, titleLabelX, titleLabelY, inventoryLabelX, inventoryLabelY, wide);
        }
    }

    private static void furnaceLabels(GuiGraphics g, net.minecraft.client.gui.Font font, AbstractFurnaceMenu menu, Component title,
                                      Component inventory, int titleX, int titleY, int inventoryX, int inventoryY, boolean wide) {
        if (wide) {
            VoidframeDraw.title(g, font, title, 14, 9);
            String state = Tr.s(menu.isLit() ? "furnace.on" : "furnace.off");
            g.drawString(font, state.toUpperCase(java.util.Locale.ROOT), WIDE_W - 14 - font.width(state), 9, menu.isLit() ? FordixPalette.FLAME : Glass.dim(), false);
            VoidframeDraw.label(g, font, Component.translatable("fordix.furnace.input"), 20, 34);
            VoidframeDraw.label(g, font, Component.translatable("fordix.furnace.result"), 124, 34);
            VoidframeDraw.label(g, font, inventory, 12, 128);
            return;
        }
        VoidframeDraw.title(g, font, title, titleX, titleY);
        VoidframeDraw.label(g, font, inventory, inventoryX, inventoryY);
    }

    /** Fuel gauge between the input and fuel slots, progress arrow to the result, and the time left under the arrow. */
    private static void furnaceBackground(GuiGraphics guiGraphics, AbstractFurnaceMenu menu, WideLayout screen,
                                          int left, int top, int width, int height, int cookTicks, boolean wide) {
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        int fuel = menu.isLit() ? menu.getLitProgress() + 1 : 0;
        int burn = menu.getBurnProgress();
        if (wide) {
            VoidframeDraw.wideBackground(guiGraphics, menu, screen, left, top, width, height);
            Glass.rule(guiGraphics, left + 10, top + 120, width - 20);
            VoidframeDraw.flame(guiGraphics, left + 24, top + 77, 12, 14, fuel);
            VoidframeDraw.arrow(guiGraphics, left + 64, top + 68, 40, 14, Math.round(burn * 40.0F / 24.0F));
            if (burn > 0 && burn < 24) {
                int seconds = Math.max(1, Math.round((24 - burn) / 24.0F * cookTicks / 20.0F));
                guiGraphics.drawString(font, seconds + " s", left + 66, top + 86, Glass.dim(), false);
            }
            return;
        }
        VoidframeDraw.containerBackground(guiGraphics, menu, left, top, width, height);
        VoidframeDraw.titleBand(guiGraphics, left, top, width);
        VoidframeDraw.flame(guiGraphics, left + 57, top + 37, 12, 14, fuel);
        VoidframeDraw.arrow(guiGraphics, left + 79, top + 34, 24, 17, burn);
        if (burn > 0 && burn < 24) {
            int seconds = Math.max(1, Math.round((24 - burn) / 24.0F * cookTicks / 20.0F));
            String text = seconds + " s";
            guiGraphics.drawString(font, text, left + 91 - font.width(text) / 2, top + 53, Glass.dim(), false);
        }
    }
}
