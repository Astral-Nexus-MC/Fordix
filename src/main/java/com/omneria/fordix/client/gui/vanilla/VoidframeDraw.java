package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.hud.HudTokens;
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

    /** The wide layout: a panel, a box under every slot (the screen says how big each one is), no band under the title. */
    public static void wideBackground(GuiGraphics guiGraphics, AbstractContainerMenu menu, WideLayout screen,
                                      int left, int top, int width, int height) {
        Panel.draw(guiGraphics, left, top, width, height);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            int[] box = screen.boxOf(slot);
            well(guiGraphics, left + slot.x - box[0], top + slot.y - box[0], box[1], box[1]);
        }
    }

    /** The accent rule under the title row, fading to the right: container titles sit on the glass, not on a band. */
    public static void titleBand(GuiGraphics guiGraphics, int left, int top, int width) {
        Glass.rule(guiGraphics, left + 6, top + 15, width - 12);
    }

    /** "14 / 27" at the right end of the title row: how many of the container's own slots hold something. */
    public static void fillCount(GuiGraphics guiGraphics, Font font, AbstractContainerMenu menu, int slots, int width, int titleY) {
        int used = 0;
        int total = Math.min(slots, menu.slots.size());
        for (int i = 0; i < total; i++) {
            if (menu.getSlot(i).hasItem()) {
                used++;
            }
        }
        String text = used + " / " + total;
        guiGraphics.drawString(font, text, width - 8 - font.width(text), titleY, used >= total ? FordixPalette.ERROR : Glass.dim(), false);
    }

    /** A recessed rectangle: see {@link Glass#well}. */
    public static void well(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        Glass.well(guiGraphics, x, y, width, height);
    }

    /** A right-pointing arrow: the full outline as a track, then {@code filled} pixels of it as a glossy accent bar. */
    public static void arrow(GuiGraphics guiGraphics, int x, int y, int width, int height, int filled) {
        for (ArrowShape.Span span : ArrowShape.spans(width, height, width)) {
            fillSpan(guiGraphics, x, y, span, FordixPalette.PROGRESS_TRACK);
        }
        int top = HudTokens.mix(FordixPalette.ACCENT, 0xFFFFFFFF, 0.3F);
        int bottom = HudTokens.mix(FordixPalette.ACCENT, 0xFF000000, 0.25F);
        for (ArrowShape.Span span : ArrowShape.spans(width, height, filled)) {
            float t = height <= 1 ? 0.0F : span.y() / (float) (height - 1);
            fillSpan(guiGraphics, x, y, span, HudTokens.mix(top, bottom, t));
        }
    }

    /** A furnace fuel gauge: a track that fills from the bottom by {@code filled} pixels, bright at the top of the flame. */
    public static void flame(GuiGraphics guiGraphics, int x, int y, int width, int height, int filled) {
        guiGraphics.fill(x, y, x + width, y + height, FordixPalette.PROGRESS_TRACK);
        int clamped = Math.max(0, Math.min(filled, height));
        if (clamped > 0) {
            guiGraphics.fillGradient(x, y + height - clamped, x + width, y + height,
                    HudTokens.mix(FordixPalette.FLAME, 0xFFFFFFFF, 0.35F), HudTokens.mix(FordixPalette.FLAME, 0xFF000000, 0.3F));
        }
    }

    /** A horizontal gauge filling from the left by {@code filled} pixels. */
    public static void gauge(GuiGraphics guiGraphics, int x, int y, int width, int height, int filled, int color) {
        guiGraphics.fill(x, y, x + width, y + height, FordixPalette.PROGRESS_TRACK);
        int clamped = Math.max(0, Math.min(filled, width));
        if (clamped > 0) {
            guiGraphics.fillGradient(x, y, x + clamped, y + height, HudTokens.mix(color, 0xFFFFFFFF, 0.3F), color);
        }
    }

    /** A vertical gauge filling downward from the top by {@code filled} pixels. */
    public static void gaugeDown(GuiGraphics guiGraphics, int x, int y, int width, int height, int filled, int color) {
        guiGraphics.fill(x, y, x + width, y + height, FordixPalette.PROGRESS_TRACK);
        int clamped = Math.max(0, Math.min(filled, height));
        if (clamped > 0) {
            guiGraphics.fillGradient(x, y, x + width, y + clamped, HudTokens.mix(color, 0xFFFFFFFF, 0.3F), color);
        }
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

    /** A section label (the inventory heading, a station caption): capitals in a dim tone. */
    public static void label(GuiGraphics guiGraphics, Font font, Component text, int x, int y) {
        Glass.label(guiGraphics, font, text.getString(), x, y);
    }

    /** The title of a container: capitals in a light accent. */
    public static void title(GuiGraphics guiGraphics, Font font, Component text, int x, int y) {
        Glass.title(guiGraphics, font, text.getString(), x, y);
    }

    private static void fillSpan(GuiGraphics guiGraphics, int x, int y, ArrowShape.Span span, int color) {
        guiGraphics.fill(x + span.x(), y + span.y(), x + span.x() + span.width(), y + span.y() + 1, color);
    }
}
