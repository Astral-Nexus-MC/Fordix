package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.widget.Panel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;

/**
 * Drawing shared by the Voidframe versions of vanilla container screens. Slot wells
 * are placed from each slot's own position in the menu, so any container layout
 * (chest rows, hopper, dispenser grid...) gets its frame without per-screen code.
 */
public final class VoidframeDraw {
    private static final int SLOT_SIZE = 18;
    private static final int RESULT_SLOT_SIZE = 24;

    private VoidframeDraw() {
    }

    /** Panel behind the whole container, then a well under every slot of the menu. */
    public static void containerBackground(GuiGraphics guiGraphics, AbstractContainerMenu menu,
                                           int left, int top, int width, int height) {
        Panel.draw(guiGraphics, left, top, width, height);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            if (slot instanceof ResultSlot || slot instanceof FurnaceResultSlot) {
                int offset = (RESULT_SLOT_SIZE - 16) / 2;
                well(guiGraphics, left + slot.x - offset, top + slot.y - offset, RESULT_SLOT_SIZE, RESULT_SLOT_SIZE);
            } else {
                well(guiGraphics, left + slot.x - 1, top + slot.y - 1, SLOT_SIZE, SLOT_SIZE);
            }
        }
    }

    /** Lighter strip behind the title row, ending just above the first row of slots. */
    public static void titleBand(GuiGraphics guiGraphics, int left, int top, int width) {
        guiGraphics.fill(left + 2, top + 2, left + width - 2, top + 16, FordixPalette.TITLE_BAND);
        guiGraphics.fill(left + 2, top + 15, left + width - 2, top + 16, FordixPalette.TITLE_RULE);
    }

    /** A recessed rectangle: dark along top/left, light along bottom/right - the inverse of a panel's bevel. */
    public static void well(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, FordixPalette.SLOT_FILL);
        guiGraphics.fill(x, y, x + width, y + 1, FordixPalette.BEVEL_SHADOW);
        guiGraphics.fill(x, y, x + 1, y + height, FordixPalette.BEVEL_SHADOW);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, FordixPalette.BEVEL_HIGHLIGHT);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, FordixPalette.BEVEL_HIGHLIGHT);
    }

    /** A right-pointing arrow: the full outline as a track, then {@code filled} pixels of it in the accent color. */
    public static void arrow(GuiGraphics guiGraphics, int x, int y, int width, int height, int filled) {
        for (ArrowShape.Span span : ArrowShape.spans(width, height, width)) {
            fillSpan(guiGraphics, x, y, span, FordixPalette.PROGRESS_TRACK);
        }
        for (ArrowShape.Span span : ArrowShape.spans(width, height, filled)) {
            fillSpan(guiGraphics, x, y, span, FordixPalette.ACCENT);
        }
    }

    /** A furnace fuel gauge: a track that fills from the bottom by {@code filled} pixels. */
    public static void flame(GuiGraphics guiGraphics, int x, int y, int width, int height, int filled) {
        guiGraphics.fill(x, y, x + width, y + height, FordixPalette.PROGRESS_TRACK);
        int clamped = Math.max(0, Math.min(filled, height));
        guiGraphics.fill(x, y + height - clamped, x + width, y + height, FordixPalette.FLAME);
    }

    /** A horizontal gauge filling from the left by {@code filled} pixels. */
    public static void gauge(GuiGraphics guiGraphics, int x, int y, int width, int height, int filled, int color) {
        guiGraphics.fill(x, y, x + width, y + height, FordixPalette.PROGRESS_TRACK);
        int clamped = Math.max(0, Math.min(filled, width));
        guiGraphics.fill(x, y, x + clamped, y + height, color);
    }

    /** A vertical gauge filling downward from the top by {@code filled} pixels. */
    public static void gaugeDown(GuiGraphics guiGraphics, int x, int y, int width, int height, int filled, int color) {
        guiGraphics.fill(x, y, x + width, y + height, FordixPalette.PROGRESS_TRACK);
        int clamped = Math.max(0, Math.min(filled, height));
        guiGraphics.fill(x, y, x + width, y + clamped, color);
    }

    /** A 2px-thick diagonal cross, used where vanilla shows its red "can't do that" X. */
    public static void cross(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        for (int i = 0; i < size - 1; i++) {
            guiGraphics.fill(x + i, y + i, x + i + 2, y + i + 2, color);
            guiGraphics.fill(x + size - 2 - i, y + i, x + size - i, y + i + 2, color);
        }
    }

    /** A plus sign, as between the two inputs of an anvil. */
    public static void plus(GuiGraphics guiGraphics, int centerX, int centerY, int size, int color) {
        int half = size / 2;
        guiGraphics.fill(centerX - half, centerY - 1, centerX + half, centerY + 1, color);
        guiGraphics.fill(centerX - 1, centerY - half, centerX + 1, centerY + half, color);
    }

    public static void label(GuiGraphics guiGraphics, Font font, Component text, int x, int y) {
        guiGraphics.drawString(font, text, x, y, FordixPalette.TEXT, false);
    }

    private static void fillSpan(GuiGraphics guiGraphics, int x, int y, ArrowShape.Span span, int color) {
        guiGraphics.fill(x + span.x(), y + span.y(), x + span.x() + span.width(), y + span.y() + 1, color);
    }
}
