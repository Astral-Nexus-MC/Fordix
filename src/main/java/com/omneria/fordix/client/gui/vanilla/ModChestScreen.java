package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * The Voidframe version of the chests of Iron Chests (copper to crystal, whose size and number of columns change from one chest to
 * the next: 9 or 12 columns, 1 to 9 rows). It is built on the menu the mod's own screen was opened with, so the menu, the slots and
 * the server do not change. The columns and rows are read from the slots, so another mod's chest-like screen could be added by its
 * class name alone. On a screen with room for it the slots go to the wide layout; otherwise they stay where the mod put them and the
 * panel takes the size of the mod's own screen.
 */
public final class ModChestScreen extends AbstractContainerScreen<AbstractContainerMenu> implements VoidframeScreen, WideLayout {
    private static final String IRON_CHEST_SCREEN = "com.progwml6.ironchest.client.screen.IronChestScreen";
    private static final int GRID_Y = 56;

    private final int ownCount;
    private final int columns;
    private final int rows;
    private final int originalWidth;
    private final int originalHeight;
    private boolean wide;

    private ModChestScreen(AbstractContainerScreen<?> original, Inventory inventory) {
        super(castMenu(original), inventory, original.getTitle());
        slotColor = FordixPalette.SLOT_HOVER;
        originalWidth = original.getXSize();
        originalHeight = original.getYSize();
        int count = 0;
        int firstY = Integer.MIN_VALUE;
        int inFirstRow = 0;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                continue;
            }
            if (count == 0) {
                firstY = slot.y;
            }
            if (slot.y == firstY) {
                inFirstRow++;
            }
            count++;
        }
        ownCount = count;
        columns = Math.max(1, inFirstRow);
        rows = Math.max(1, (count + columns - 1) / columns);
    }

    private static AbstractContainerMenu castMenu(AbstractContainerScreen<?> original) {
        return original.getMenu();
    }

    /** The Voidframe screen for a chest screen of a mod that is opening, or null for any other screen. */
    public static Screen replacementFor(Screen screen, Inventory inventory) {
        if (!FordixConfig.modScreens() || !(screen instanceof AbstractContainerScreen<?> container)
                || !screen.getClass().getName().equals(IRON_CHEST_SCREEN)) {
            return null;
        }
        ModChestScreen replacement = new ModChestScreen(container, inventory);
        return replacement.ownCount > 0 ? replacement : null;
    }

    @Override
    public boolean wide() {
        return wide;
    }

    private int panelWidth() {
        return Math.max(218, 2 * 10 + columns * ContainerLayout.PITCH);
    }

    private int divider() {
        return GRID_Y + ContainerLayout.PITCH * rows + 10;
    }

    private int panelHeight() {
        return divider() + 168;
    }

    @Override
    public int[] searchRect() {
        return wide ? new int[]{58, 28, panelWidth() - 68 - 54, 20} : null;
    }

    @Override
    public int[] sortRect() {
        return wide ? new int[]{10, 28, 44, 20} : null;
    }

    @Override
    protected void init() {
        wide = ContainerLayout.fits(this, panelWidth(), panelHeight()) && ContainerLayout.ready(menu);
        imageWidth = wide ? panelWidth() : originalWidth;
        imageHeight = wide ? panelHeight() : originalHeight;
        inventoryLabelY = imageHeight - 94;
        super.init();
        if (!wide) {
            ContainerLayout.restore(menu);
            return;
        }
        ContainerLayout.grid(menu, ownCount, columns, (panelWidth() - columns * ContainerLayout.PITCH) / 2 + 1, GRID_Y);
        ContainerLayout.playerBlock(menu, (panelWidth() - 9 * ContainerLayout.PITCH) / 2 + 1, divider() + 26, divider() + 26 + 76);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        if (wide) {
            VoidframeDraw.wideBackground(g, menu, this, leftPos, topPos, panelWidth(), panelHeight());
            Glass.rule(g, leftPos + 10, topPos + divider(), panelWidth() - 20);
            g.drawString(font, Tr.s("container.hint"), leftPos + 12, topPos + divider() + 26 + 76 + 38, Glass.dim(), false);
            return;
        }
        VoidframeDraw.containerBackground(g, menu, leftPos, topPos, imageWidth, imageHeight);
        VoidframeDraw.titleBand(g, leftPos, topPos, imageWidth);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        if (wide) {
            VoidframeDraw.title(g, font, title, 14, 9);
            VoidframeDraw.fillCount(g, font, menu, ownCount, panelWidth() + 6, 9);
            VoidframeDraw.label(g, font, playerInventoryTitle, 12, divider() + 10);
            return;
        }
        VoidframeDraw.title(g, font, title, titleLabelX, titleLabelY);
        VoidframeDraw.label(g, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
        VoidframeDraw.fillCount(g, font, menu, ownCount, imageWidth, titleLabelY);
    }
}
