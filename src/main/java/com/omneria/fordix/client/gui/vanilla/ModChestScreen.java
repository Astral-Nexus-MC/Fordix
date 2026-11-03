package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.widget.FordixButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

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
    private static final int STRIP = 22;
    private static final int STRIP_COMPACT = 20;
    private static final int MARGIN = 8;
    private static final int ABOVE = GRID_Y;
    private static final int BELOW = 10 + 168;

    private final List<Slot> own = new ArrayList<>();
    private final int[] originalX;
    private final int[] originalY;
    private boolean wide;
    private boolean paged;
    private int shownRows;
    private int page;

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
        originalX = new int[menu.slots.size()];
        originalY = new int[menu.slots.size()];
        for (Slot slot : menu.slots) {
            originalX[slot.index] = slot.x;
            originalY[slot.index] = slot.y;
            if (!(slot.container instanceof Inventory)) {
                own.add(slot);
            }
        }
        columns = Math.max(1, inFirstRow);
        rows = Math.max(1, (count + columns - 1) / columns);
        shownRows = rows;
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
        return GRID_Y + ContainerLayout.PITCH * shownRows + 10 + (paged ? STRIP : 0);
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
        ContainerLayout.restore(menu);
        boolean ready = ContainerLayout.ready(menu);
        wide = false;
        paged = false;
        shownRows = rows;
        if (ready && width >= panelWidth() + 2 * MARGIN) {
            int fit = ChestPaging.wideRows(height, MARGIN, rows, ContainerLayout.PITCH, ABOVE, BELOW, STRIP);
            if (fit > 0) {
                wide = true;
                shownRows = fit;
                paged = fit < rows;
            }
        }
        if (!wide && ready) {
            shownRows = ChestPaging.compactRows(height, MARGIN, originalHeight, rows, 18, STRIP_COMPACT);
            paged = shownRows < rows;
        }
        int pages = ChestPaging.pages(rows, shownRows);
        page = Math.max(0, Math.min(page, pages - 1));
        imageWidth = wide ? panelWidth() : originalWidth;
        imageHeight = wide ? panelHeight() : originalHeight - compactCut();
        inventoryLabelY = imageHeight - 94;
        super.init();
        if (!wide && !paged) {
            return;
        }
        placeSlots();
        if (paged) {
            int stripY = wide ? GRID_Y + ContainerLayout.PITCH * shownRows + 2 : originalY[own.get(0).index] + shownRows * 18 + 1;
            int center = imageWidth / 2;
            addRenderableWidget(new FordixButton(leftPos + center - 52, topPos + stripY, 20, 18, Component.literal("<"), () -> turn(-1)));
            addRenderableWidget(new FordixButton(leftPos + center + 32, topPos + stripY, 20, 18, Component.literal(">"), () -> turn(1)));
        }
    }

    /** What the compact panel loses when it is paged: the hidden rows, less the strip of the page buttons. */
    private int compactCut() {
        return paged ? (rows - shownRows) * 18 - STRIP_COMPACT : 0;
    }

    private void turn(int by) {
        int pages = ChestPaging.pages(rows, shownRows);
        page = Math.max(0, Math.min(pages - 1, page + by));
        placeSlots();
    }

    /** The slots of the page shown go to the grid; the others go out of the screen (they stay in the menu, the server knows nothing of it). */
    private void placeSlots() {
        if (wide) {
            int gridX = (panelWidth() - columns * ContainerLayout.PITCH) / 2 + 1;
            for (int i = 0; i < own.size(); i++) {
                int row = i / columns;
                if (row / shownRows == page) {
                    ContainerLayout.put(own.get(i), gridX + i % columns * ContainerLayout.PITCH,
                            GRID_Y + row % shownRows * ContainerLayout.PITCH, ContainerLayout.BOX);
                } else {
                    ContainerLayout.put(own.get(i), -10000, -10000, ContainerLayout.BOX);
                }
            }
            ContainerLayout.playerBlock(menu, (panelWidth() - 9 * ContainerLayout.PITCH) / 2 + 1, divider() + 26, divider() + 26 + 76);
            return;
        }
        for (int i = 0; i < own.size(); i++) {
            int row = i / columns;
            int index = own.get(i).index;
            if (row / shownRows == page) {
                int first = (row % shownRows) * columns + i % columns;
                ContainerLayout.put(own.get(i), originalX[index], originalY[own.get(first).index], 16);
            } else {
                ContainerLayout.put(own.get(i), -10000, -10000, 16);
            }
        }
        int shift = compactCut();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                ContainerLayout.put(slot, originalX[slot.index], originalY[slot.index] - shift, 16);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (paged && mouseX >= leftPos && mouseX < leftPos + imageWidth && mouseY >= topPos && mouseY < topPos + imageHeight) {
            turn(delta > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
        } else {
            VoidframeDraw.containerBackground(g, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(g, leftPos, topPos, imageWidth);
        }
        if (paged) {
            int stripY = wide ? GRID_Y + ContainerLayout.PITCH * shownRows + 2 : originalY[own.get(0).index] + shownRows * 18 + 1;
            String label = (page + 1) + " / " + ChestPaging.pages(rows, shownRows);
            g.drawString(font, label, leftPos + imageWidth / 2 - font.width(label) / 2, topPos + stripY + 5, Glass.dim(), false);
        }
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
        // Quark's buttons sit at the top right of the slots: the count stops before them.
        VoidframeDraw.fillCount(g, font, menu, ownCount, imageWidth - 34, titleLabelY);
    }
}
