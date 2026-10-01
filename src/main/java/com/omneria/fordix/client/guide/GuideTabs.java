package com.omneria.fordix.client.guide;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.vanilla.VoidframeScreens;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * A column of "Guides" tabs along the right edge of the survival inventory, one per
 * configured guide whose mod is installed. JEI lays its item list out from the
 * screen's declared width, so the Voidframe inventory widens itself by
 * {@link #stripWidth()} to keep the column clear of it; the vanilla inventory
 * (Voidframe screens turned off) can't reserve the space, so JEI may overlap the
 * tabs there. Tabs follow the inventory when the recipe book slides it sideways.
 */
public final class GuideTabs {
    private static final int SIZE = 20;
    private static final int SPACING = 22;
    private static final int GAP_TO_PANEL = 2;

    /** Added when the list has no "minecraft" entry of its own, so vanilla items also point to their wiki page. */
    private static final GuideEntry VANILLA_WIKI = GuideEntry.parse(
            "minecraft | Minecraft : Wiki | minecraft:knowledge_book | url:https://minecraft.wiki/w/{objet}");

    private GuideTabs() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }
        List<GuideEntry> entries = installedEntries();
        for (int index = 0; index < entries.size(); index++) {
            event.addListener(new Tab(screen, index, entries.get(index)));
        }
    }

    /** Guides whose mod is installed, in order: the server's list when it sent one, else the local config. */
    public static List<GuideEntry> installedEntries() {
        List<GuideEntry> entries = new ArrayList<>();
        List<? extends String> lines = ServerGuides.lines().isEmpty() ? FordixConfig.GUIDES.get() : ServerGuides.lines();
        for (String line : lines) {
            GuideEntry entry = GuideEntry.parse(line);
            if (entry != null && isInstalled(entry.modId())) {
                entries.add(entry);
            }
        }
        if (entries.stream().noneMatch(entry -> entry.modId().equals("minecraft"))) {
            entries.add(VANILLA_WIKI);
        }
        return entries;
    }

    /** Width the tab column takes beside the panel, or 0 when there are no tabs to show. */
    public static int stripWidth() {
        return installedEntries().isEmpty() ? 0 : GAP_TO_PANEL + SIZE;
    }

    private static boolean isInstalled(String modId) {
        return modId.equals("minecraft") || ModList.get().isLoaded(modId);
    }

    public static ItemStack iconFor(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item == Items.AIR ? Items.BOOK : item);
    }

    static final class Tab extends AbstractWidget {
        private final AbstractContainerScreen<?> screen;
        private final int index;
        private final GuideEntry entry;
        private final ItemStack icon;

        Tab(AbstractContainerScreen<?> screen, int index, GuideEntry entry) {
            super(0, 0, SIZE, SIZE, Component.literal(entry.label()));
            this.screen = screen;
            this.index = index;
            this.entry = entry;
            this.icon = iconFor(entry.icon());
            setTooltip(Tooltip.create(Component.literal(entry.label())));
            follow();
        }

        /** Re-anchors to the inventory panel every frame, since the recipe book moves it. */
        private void follow() {
            // The Voidframe inventory already counts the column in its width; vanilla's doesn't.
            int panelRight = screen instanceof VoidframeScreens.PlayerInventory voidframe
                    ? screen.getGuiLeft() + voidframe.panelWidth()
                    : screen.getGuiLeft() + screen.getXSize();
            setX(panelRight + GAP_TO_PANEL);
            setY(screen.getGuiTop() + 4 + index * SPACING);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            follow();
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int fill = isHoveredOrFocused() ? FordixPalette.BUTTON_FILL_HOVERED : FordixPalette.PANEL_FILL;
            guiGraphics.fill(x, y, x + SIZE, y + SIZE, fill);
            guiGraphics.fill(x, y, x + SIZE, y + 1, FordixPalette.BEVEL_HIGHLIGHT);
            guiGraphics.fill(x + SIZE - 1, y, x + SIZE, y + SIZE, FordixPalette.BEVEL_SHADOW);
            guiGraphics.fill(x, y + SIZE - 1, x + SIZE, y + SIZE, FordixPalette.BEVEL_SHADOW);
            // Accent strip on the edge facing the inventory, lit on hover.
            guiGraphics.fill(x, y, x + 2, y + SIZE, isHoveredOrFocused() ? FordixPalette.ACCENT : FordixPalette.BEVEL_HIGHLIGHT);
            guiGraphics.renderItem(icon, x + 3, y + 2);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            GuideActions.run(entry);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
