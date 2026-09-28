package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.joml.Quaternionf;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Voidframe versions of the vanilla workstation screens - the ones with more than
 * slots and an arrow. As with {@link VoidframeScreens}, only drawing changes;
 * where vanilla keeps state it needs for drawing in private fields (stonecutter
 * scroll, smithing preview, horse), it's read by type through {@link Reflect}.
 */
public final class VoidframeStations {
    private static final int VANILLA_GREEN = 0xFF80FF20;
    private static final int VANILLA_RED = 0xFFFF6060;

    private VoidframeStations() {
    }

    public static class Grindstone extends GrindstoneScreen implements VoidframeScreen {
        public Grindstone(GrindstoneMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
            VoidframeDraw.arrow(guiGraphics, leftPos + 88, topPos + 34, 24, 15, 24);
            boolean hasInput = menu.getSlot(0).hasItem() || menu.getSlot(1).hasItem();
            if (hasInput && !menu.getSlot(2).hasItem()) {
                VoidframeDraw.cross(guiGraphics, leftPos + 92, topPos + 33, 16, FordixPalette.ERROR);
            }
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    public static class Anvil extends AnvilScreen implements VoidframeScreen {
        public Anvil(AnvilMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
            // Behind the rename field, which vanilla draws borderless on top.
            VoidframeDraw.well(guiGraphics, leftPos + 59, topPos + 20, 110, 16);
            VoidframeDraw.plus(guiGraphics, leftPos + 60, topPos + 55, 10, FordixPalette.TEXT);
            VoidframeDraw.arrow(guiGraphics, leftPos + 102, topPos + 48, 22, 15, 22);
            boolean hasInput = menu.getSlot(0).hasItem() || menu.getSlot(1).hasItem();
            if (hasInput && !menu.getSlot(2).hasItem()) {
                VoidframeDraw.cross(guiGraphics, leftPos + 105, topPos + 47, 16, FordixPalette.ERROR);
            }
        }

        /** Same cost line as vanilla (red when too expensive or unaffordable), with Voidframe titles. */
        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            int cost = menu.getCost();
            if (cost <= 0) {
                return;
            }
            Player player = minecraft.player;
            Component text;
            int color = VANILLA_GREEN;
            if (cost >= 40 && !player.getAbilities().instabuild) {
                text = Component.translatable("container.repair.expensive");
                color = VANILLA_RED;
            } else if (!menu.getSlot(2).hasItem()) {
                return;
            } else {
                text = Component.translatable("container.repair.cost", cost);
                if (!menu.getSlot(2).mayPickup(player)) {
                    color = VANILLA_RED;
                }
            }
            int x = imageWidth - 8 - font.width(text) - 2;
            guiGraphics.fill(x - 2, 67, imageWidth - 8, 79, FordixPalette.TOOLTIP_FILL);
            guiGraphics.drawString(font, text, x, 69, color);
        }
    }

    public static class BrewingStand extends BrewingStandScreen implements VoidframeScreen {
        private static final int[] BUBBLE_HEIGHTS = {27, 22, 18, 14, 10, 5, 0};

        public BrewingStand(BrewingStandMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
            int fuel = Math.max(0, Math.min((18 * menu.getFuel() + 19) / 20, 18));
            VoidframeDraw.gauge(guiGraphics, leftPos + 60, topPos + 44, 18, 4, fuel, FordixPalette.FLAME);
            int ticks = menu.getBrewingTicks();
            int brewed = ticks > 0 ? (int) (28.0F * (1.0F - ticks / 400.0F)) : 0;
            VoidframeDraw.gaugeDown(guiGraphics, leftPos + 97, topPos + 16, 9, 28, brewed, FordixPalette.ACCENT);
            int bubbles = ticks > 0 ? BUBBLE_HEIGHTS[ticks / 2 % BUBBLE_HEIGHTS.length] : 0;
            VoidframeDraw.flame(guiGraphics, leftPos + 65, topPos + 16, 8, 27, bubbles);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    /** The power buttons are vanilla widgets and keep their own look; the panel and payment row are redrawn. */
    public static class Beacon extends BeaconScreen implements VoidframeScreen {
        private static final Component PRIMARY = Component.translatable("block.minecraft.beacon.primary");
        private static final Component SECONDARY = Component.translatable("block.minecraft.beacon.secondary");

        public Beacon(BeaconMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
            guiGraphics.renderItem(new ItemStack(Items.NETHERITE_INGOT), leftPos + 20, topPos + 109);
            guiGraphics.renderItem(new ItemStack(Items.EMERALD), leftPos + 41, topPos + 109);
            guiGraphics.renderItem(new ItemStack(Items.DIAMOND), leftPos + 41 + 22, topPos + 109);
            guiGraphics.renderItem(new ItemStack(Items.GOLD_INGOT), leftPos + 42 + 44, topPos + 109);
            guiGraphics.renderItem(new ItemStack(Items.IRON_INGOT), leftPos + 42 + 66, topPos + 109);
            guiGraphics.pose().popPose();
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            guiGraphics.drawCenteredString(font, PRIMARY, 62, 10, FordixPalette.TEXT_BRIGHT);
            guiGraphics.drawCenteredString(font, SECONDARY, 169, 10, FordixPalette.TEXT_BRIGHT);
        }
    }

    /**
     * The three enchantment offers are drawn as wells with the same rules as vanilla
     * (greyed when unaffordable, highlighted on hover); vanilla still handles clicks
     * and the enchantment tooltip. The animated 3D book becomes a floating enchanted book.
     */
    public static class Enchanting extends EnchantmentScreen implements VoidframeScreen {
        private static final int OPTION_X = 60;
        private static final int OPTION_Y = 14;
        private static final int OPTION_WIDTH = 108;
        private static final int OPTION_HEIGHT = 19;
        private static final ItemStack BOOK = new ItemStack(Items.ENCHANTED_BOOK);

        public Enchanting(EnchantmentMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            renderBook(guiGraphics);
            Player player = minecraft.player;
            int lapis = menu.getGoldCount();
            EnchantmentNames.getInstance().initSeed(menu.getEnchantmentSeed());
            for (int i = 0; i < 3; i++) {
                int x = leftPos + OPTION_X;
                int y = topPos + OPTION_Y + OPTION_HEIGHT * i;
                int cost = menu.costs[i];
                VoidframeDraw.well(guiGraphics, x, y, OPTION_WIDTH, OPTION_HEIGHT);
                if (cost == 0) {
                    continue;
                }
                String costText = Integer.toString(cost);
                FormattedText name = EnchantmentNames.getInstance().getRandomName(font, 86 - font.width(costText));
                boolean affordable = (lapis >= i + 1 && player.experienceLevel >= cost) || player.getAbilities().instabuild;
                boolean hovered = mouseX >= x && mouseX < x + OPTION_WIDTH && mouseY >= y && mouseY < y + OPTION_HEIGHT;
                if (affordable && hovered) {
                    guiGraphics.fill(x + 1, y + 1, x + OPTION_WIDTH - 1, y + OPTION_HEIGHT - 1, FordixPalette.SLOT_HOVER);
                }
                int badgeColor = affordable ? FordixPalette.ACCENT : FordixPalette.PROGRESS_TRACK;
                guiGraphics.fill(x + 2, y + 2, x + 17, y + 17, badgeColor);
                guiGraphics.drawCenteredString(font, Integer.toString(i + 1), x + 10, y + 6,
                        affordable ? FordixPalette.PANEL_FILL : FordixPalette.TEXT);
                int nameColor = !affordable ? 0xFF5A6670 : hovered ? FordixPalette.TEXT_BRIGHT : FordixPalette.TEXT;
                guiGraphics.drawWordWrap(font, name, x + 20, y + 2, 86 - font.width(costText), nameColor);
                int costColor = affordable ? VANILLA_GREEN : 0xFF407040;
                guiGraphics.drawString(font, costText, x + OPTION_WIDTH - 2 - font.width(costText), y + 10, costColor);
            }
        }

        private void renderBook(GuiGraphics guiGraphics) {
            VoidframeDraw.well(guiGraphics, leftPos + 12, topPos + 12, 36, 32);
            float bob = (float) Math.sin(System.currentTimeMillis() / 400.0) * 1.5F;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos + 16.0F, topPos + 14.0F + bob, 0.0F);
            guiGraphics.pose().scale(1.75F, 1.75F, 1.0F);
            guiGraphics.renderItem(BOOK, 0, 0);
            guiGraphics.pose().popPose();
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    /** Villager trading. The trade list buttons stay vanilla widgets; the panel, wells and level bar are redrawn. */
    public static class Merchant extends MerchantScreen implements VoidframeScreen {
        private static final Component TRADES = Component.translatable("merchant.trades");

        public Merchant(MerchantMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
            VoidframeDraw.well(guiGraphics, leftPos + 4, topPos + 17, 89, 142);
            VoidframeDraw.well(guiGraphics, leftPos + 93, topPos + 17, 8, 142);
            VoidframeDraw.arrow(guiGraphics, leftPos + 186, topPos + 38, 24, 13, 24);
            int level = menu.getTraderLevel();
            if (menu.showProgressBar() && level < 5) {
                int min = VillagerData.getMinXpPerLevel(level);
                int xp = menu.getTraderXp();
                int filled = 0;
                if (xp >= min && VillagerData.canLevelUp(level)) {
                    float perXp = 102.0F / (VillagerData.getMaxXpPerLevel(level) - min);
                    filled = Math.min((int) (perXp * (xp - min)), 102);
                }
                VoidframeDraw.gauge(guiGraphics, leftPos + 136, topPos + 16, 102, 5, filled, FordixPalette.ACCENT);
            }
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            int level = menu.getTraderLevel();
            Component heading = title;
            if (level > 0 && level <= 5 && menu.showProgressBar()) {
                heading = title.copy().append(" - ").append(Component.translatable("merchant.level." + level));
            }
            VoidframeDraw.label(guiGraphics, font, heading, 49 + imageWidth / 2 - font.width(heading) / 2, 6);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            VoidframeDraw.label(guiGraphics, font, TRADES, 5 - font.width(TRADES) / 2 + 48, 6);
        }
    }

    /**
     * The recipe grid is part of vanilla's background, so it's redrawn here from the
     * menu's recipe list; the scroll position lives in private fields read by type.
     */
    public static class Stonecutter extends StonecutterScreen implements VoidframeScreen {
        private static final Field START_INDEX = Reflect.uniqueField(StonecutterScreen.class, int.class);
        private static final Field SCROLL_OFFSET = Reflect.uniqueField(StonecutterScreen.class, float.class);
        private static final int GRID_X = 52;
        private static final int GRID_Y = 14;

        public Stonecutter(StonecutterMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.well(guiGraphics, leftPos + GRID_X - 1, topPos + GRID_Y - 1, 66, 56);
            VoidframeDraw.well(guiGraphics, leftPos + 118, topPos + 14, 14, 57);

            Object offset = Reflect.get(SCROLL_OFFSET, this);
            float scroll = offset instanceof Float value ? value : 0.0F;
            boolean scrollable = menu.hasInputItem() && menu.getNumRecipes() > 12;
            int thumbY = topPos + 15 + (int) (41.0F * scroll);
            guiGraphics.fill(leftPos + 119, thumbY, leftPos + 131, thumbY + 15,
                    scrollable ? FordixPalette.ACCENT : FordixPalette.PROGRESS_TRACK);

            if (!menu.hasInputItem()) {
                return;
            }
            Object start = Reflect.get(START_INDEX, this);
            int first = start instanceof Integer value ? value : 0;
            List<StonecutterRecipe> recipes = menu.getRecipes();
            for (int i = first; i < first + 12 && i < menu.getNumRecipes(); i++) {
                int index = i - first;
                int x = leftPos + GRID_X + index % 4 * 16;
                int y = topPos + GRID_Y + index / 4 * 18 + 2;
                boolean hovered = mouseX >= x && mouseX < x + 16 && mouseY >= y - 1 && mouseY < y + 17;
                int fill = i == menu.getSelectedRecipeIndex() ? FordixPalette.ACCENT
                        : hovered ? FordixPalette.BUTTON_FILL_HOVERED : FordixPalette.BUTTON_FILL;
                guiGraphics.fill(x, y - 1, x + 16, y + 17, fill);
                guiGraphics.renderItem(recipes.get(i).getResultItem(minecraft.level.registryAccess()), x, y);
            }
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    /** Smithing table, keeping vanilla's cycling slot hints and armor stand preview (read by type). */
    public static class Smithing extends SmithingScreen implements VoidframeScreen {
        private static final Field ARMOR_STAND = Reflect.uniqueField(SmithingScreen.class, ArmorStand.class);
        private static final List<Field> SLOT_HINTS = hintFields();
        private static final Quaternionf ARMOR_STAND_ANGLE = new Quaternionf().rotationXYZ(0.43633232F, 0.0F, (float) Math.PI);

        public Smithing(SmithingMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            slotColor = FordixPalette.SLOT_HOVER;
        }

        private static List<Field> hintFields() {
            List<Field> fields = new ArrayList<>();
            for (Field field : SmithingScreen.class.getDeclaredFields()) {
                if (field.getType() == CyclingSlotBackground.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        field.setAccessible(true);
                        fields.add(field);
                    } catch (RuntimeException ignored) {
                        // The slot hints are a nicety; the screen works without them.
                    }
                }
            }
            return fields;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.arrow(guiGraphics, leftPos + 66, topPos + 48, 22, 15, 22);
            VoidframeDraw.well(guiGraphics, leftPos + 120, topPos + 18, 44, 64);
            for (Field field : SLOT_HINTS) {
                if (Reflect.get(field, this) instanceof CyclingSlotBackground hint) {
                    hint.render(menu, guiGraphics, partialTick, leftPos, topPos);
                }
            }
            if (Reflect.get(ARMOR_STAND, this) instanceof ArmorStand stand) {
                InventoryScreen.renderEntityInInventory(guiGraphics, leftPos + 141, topPos + 75, 25, ARMOR_STAND_ANGLE, null, stand);
            }
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }

    /** Horse, donkey, mule and llama inventories. Saddle, armor and chest slots only get a well when active. */
    public static class Horse extends HorseInventoryScreen implements VoidframeScreen {
        static final Field HORSE = Reflect.uniqueField(HorseInventoryScreen.class, AbstractHorse.class);
        private final AbstractHorse horse;

        public Horse(HorseInventoryMenu menu, Inventory inventory, AbstractHorse horse) {
            super(menu, inventory, horse);
            this.horse = horse;
            slotColor = FordixPalette.SLOT_HOVER;
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            VoidframeDraw.containerBackground(guiGraphics, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(guiGraphics, leftPos, topPos, imageWidth);
            VoidframeDraw.well(guiGraphics, leftPos + 26, topPos + 17, 52, 53);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, leftPos + 51, topPos + 60, 17,
                    (float) (leftPos + 51 - mouseX), (float) (topPos + 75 - 50 - mouseY), horse);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            VoidframeDraw.label(guiGraphics, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(guiGraphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        }
    }
}
