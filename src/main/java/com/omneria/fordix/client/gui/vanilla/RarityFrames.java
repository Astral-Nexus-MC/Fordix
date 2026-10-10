package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.tooltip.ItemRarity;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.HashMap;
import java.util.Map;

/**
 * The rarity of an item shown by its slot's background: a tint that grows with the rank (uncommon to legendary, commons stay
 * plain), under the item; over it, on epic and legendary, a shine that passes now and then, and on legendary also a gold frame
 * turning around the slot, sparkles and a glow that breathes. A pointer over a slot sends the shine once, an item that just
 * arrived flashes. The numbers are in {@link RarityMotion}; the item itself is never changed.
 */
public final class RarityFrames {
    private static final int GOLD_LIGHT = 0xFFE39A;
    private static final int SPARK = 0xFFF4C8;
    /** The first moment each slot of the open screen was seen holding what it holds, to flash new arrivals (not the first view). */
    private static final Map<Integer, Long> ARRIVED = new HashMap<>();
    private static final Map<Integer, ItemStack> LAST = new HashMap<>();
    private static final Map<Integer, Long> HOVERED = new HashMap<>();
    private static Object watching;
    private static int hoveredIndex = -1;

    private RarityFrames() {
    }

    private static double seconds() {
        return Util.getMillis() / 1000.0D;
    }

    /** Containers, before the items are drawn (the screen's own origin is not applied yet): the tint under each item. */
    public static void onContainerBackground(ContainerScreenEvent.Render.Background event) {
        if (!FordixConfig.voidframeScreens()) {
            return;
        }
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        GuiGraphics g = event.getGuiGraphics();
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (slot.isActive() && ItemRarity.isNotable(stack)) {
                int[] box = screen instanceof WideLayout wideScreen ? wideScreen.boxOf(slot) : new int[]{1, 18};
                int x = screen.getGuiLeft() + slot.x - box[0];
                int y = screen.getGuiTop() + slot.y - box[0];
                tint(g, x, y, box[1], ItemRarity.rank(stack), ItemRarity.color(stack));
            }
        }
    }

    /** Containers: after slots and labels, already translated to the container's top-left corner: the shine, the frame, the flash. */
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!FordixConfig.voidframeScreens()) {
            return;
        }
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        GuiGraphics g = event.getGuiGraphics();
        if (watching != screen) {
            watching = screen;
            ARRIVED.clear();
            LAST.clear();
            HOVERED.clear();
            hoveredIndex = -1;
        }
        Slot over = screen.getSlotUnderMouse();
        int overIndex = over == null ? -1 : over.index;
        if (overIndex != hoveredIndex) {
            hoveredIndex = overIndex;
            if (overIndex >= 0) {
                HOVERED.put(overIndex, Util.getMillis());
            }
        }
        long now = Util.getMillis();
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            ItemStack before = LAST.get(slot.index);
            if (before != null && !stack.isEmpty() && (before.isEmpty() || before.getItem() != stack.getItem())) {
                ARRIVED.put(slot.index, now);
            }
            LAST.put(slot.index, stack.copy());
            if (!ItemRarity.isNotable(stack)) {
                continue;
            }
            int[] box = screen instanceof WideLayout wideScreen ? wideScreen.boxOf(slot) : new int[]{1, 18};
            int x = slot.x - box[0];
            int y = slot.y - box[0];
            double since = ARRIVED.containsKey(slot.index) ? (now - ARRIVED.get(slot.index)) / 1000.0D : -1.0D;
            double hover = HOVERED.containsKey(slot.index) && slot.index == hoveredIndex ? (now - HOVERED.get(slot.index)) / 1000.0D : -1.0D;
            over(g, x, y, box[1], ItemRarity.rank(stack), ItemRarity.color(stack), RarityMotion.delayOf(slot.index), since, hover,
                    slot.index == hoveredIndex);
        }
    }

    /** Hotbar: the nine slots sit at fixed offsets from the screen's bottom center. */
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type() || !FordixConfig.voidframeHud()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int centerX = event.getWindow().getGuiScaledWidth() / 2;
        int bottom = event.getWindow().getGuiScaledHeight();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!ItemRarity.isNotable(stack)) {
                continue;
            }
            int x = centerX - 90 + i * 20 + 1;
            // The box starts one pixel above and left of the item (the game draws it at bottom - 19): the item sits inside it.
            int y = bottom - 20;
            int rank = ItemRarity.rank(stack);
            int color = ItemRarity.color(stack);
            // The game draws the item before this: tint behind it, then the item again over the tint.
            tint(g, x, y, 18, rank, color);
            g.renderItem(stack, x + 1, y + 1, i);
            g.renderItemDecorations(minecraft.font, stack, x + 1, y + 1);
            over(g, x, y, 18, rank, color, RarityMotion.delayOf(i), -1.0D, -1.0D, false);
        }
    }

    /** The tint under the item, from the rank's top color to its bottom color; on epic a soft violet edge inside. */
    private static void tint(GuiGraphics g, int x, int y, int size, int rank, int color) {
        int[] tint = RarityMotion.tint(rank, color);
        g.fillGradient(x, y, x + size, y + size, tint[0], tint[1]);
        if (rank == RarityMotion.EPIC) {
            int edge = color & 0xFFFFFF;
            g.fill(x, y, x + size, y + 1, (0x50 << 24) | edge);
            g.fill(x, y + size - 1, x + size, y + size, (0x50 << 24) | edge);
            g.fill(x, y + 1, x + 1, y + size - 1, (0x50 << 24) | edge);
            g.fill(x + size - 1, y + 1, x + size, y + size - 1, (0x50 << 24) | edge);
        }
    }

    /** What moves over the item: the shine, then for the legendary the frame, the glow and the sparkles; the hover and arrival lights. */
    private static void over(GuiGraphics g, int x, int y, int size, int rank, int color, double delay, double sinceArrival,
                             double sinceHover, boolean hovered) {
        double t = seconds();
        if (rank >= RarityMotion.LEGENDARY) {
            double breath = RarityMotion.breath(t, delay);
            int alpha = (int) Math.round(0x60 * breath);
            g.fillGradient(x + 1, y + size / 3, x + size - 1, y + size - 1, 0, (alpha << 24) | (color & 0xFFFFFF));
        }
        double sweep = sinceHover >= 0 ? RarityMotion.hoverSweep(sinceHover) : -1.0D;
        if (sweep < 0 && RarityMotion.hasSweep(rank)) {
            sweep = RarityMotion.sweep(rank, t, delay);
        }
        if (sweep >= 0) {
            shine(g, x, y, size, sweep, rank >= RarityMotion.LEGENDARY ? 0xFFEDAA : 0xD6AAFF, rank >= RarityMotion.LEGENDARY ? 0x58 : 0x34);
        }
        if (rank >= RarityMotion.LEGENDARY) {
            frame(g, x, y, size, RarityMotion.frameTurn(t, delay));
            sparkles(g, x, y, size, t, delay);
        }
        if (hovered) {
            g.fill(x, y, x + size, y + size, 0x1CFFFFFF);
        }
        double flash = RarityMotion.flash(sinceArrival);
        if (flash > 0) {
            g.fill(x, y, x + size, y + size, ((int) Math.round(255 * flash) << 24) | 0xFFFFFF);
        }
    }

    /** A slanted band crossing the slot from left to right, clipped to the slot. */
    private static void shine(GuiGraphics g, int x, int y, int size, double progress, int rgb, int alpha) {
        int band = Math.max(3, size / 3);
        int slant = size / 3;
        int start = (int) Math.round(-band - slant + progress * (size + band * 2 + slant));
        g.enableScissor(x, y, x + size, y + size);
        for (int row = 0; row < size; row++) {
            int from = x + start + (size - row) * slant / size;
            g.fill(from, y + row, from + band, y + row + 1, (alpha << 24) | rgb);
            g.fill(from + band / 3, y + row, from + band * 2 / 3 + 1, y + row + 1, ((alpha / 2) << 24) | 0xFFFFFF);
        }
        g.disableScissor();
    }

    /** A gold line around the slot, brighter on two arcs that turn. */
    private static void frame(GuiGraphics g, int x, int y, int size, double turn) {
        int perimeter = 4 * (size - 1);
        for (int i = 0; i < perimeter; i++) {
            double position = (double) i / perimeter;
            double distance = Math.min(Math.abs(position - turn), 1.0D - Math.abs(position - turn));
            double other = Math.min(Math.abs(position - ((turn + 0.5D) % 1.0D)), 1.0D - Math.abs(position - ((turn + 0.5D) % 1.0D)));
            double near = Math.min(distance, other);
            int alpha = near < 0.09D ? 0xFF : near < 0.16D ? 0xB0 : 0x48;
            int px;
            int py;
            int side = i / (size - 1);
            int along = i % (size - 1);
            switch (side) {
                case 0 -> {
                    px = x + along;
                    py = y;
                }
                case 1 -> {
                    px = x + size - 1;
                    py = y + along;
                }
                case 2 -> {
                    px = x + size - 1 - along;
                    py = y + size - 1;
                }
                default -> {
                    px = x;
                    py = y + size - 1 - along;
                }
            }
            g.fill(px, py, px + 1, py + 1, (alpha << 24) | GOLD_LIGHT);
        }
    }

    /** Four points that twinkle at their own pace. */
    private static void sparkles(GuiGraphics g, int x, int y, int size, double t, double delay) {
        int[][] spots = {{size / 5, size / 5}, {size * 3 / 4, size / 8}, {size * 4 / 5, size * 2 / 3}, {size / 7, size * 3 / 4}};
        for (int i = 0; i < spots.length; i++) {
            double light = RarityMotion.twinkle(t, delay, i);
            if (light > 0.05D) {
                int alpha = (int) Math.round(255 * light);
                g.fill(x + spots[i][0], y + spots[i][1], x + spots[i][0] + 1, y + spots[i][1] + 1, (alpha << 24) | SPARK);
            }
        }
    }
}
