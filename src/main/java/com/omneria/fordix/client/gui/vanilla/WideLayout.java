package com.omneria.fordix.client.gui.vanilla;

import net.minecraft.world.inventory.Slot;

/**
 * Implemented by the container screens that have the Voidframe 2 layout of the mockup (22 px pitch, 20 px slot boxes, the
 * tools inside the panel). The layout is used only when the screen is large enough ({@link ContainerLayout#fits}); on a small
 * one the screen keeps the game's own slot positions and {@link #wide()} is false. The shared effects (hover halo, rarity
 * line, search and sort) ask the screen where each slot box is.
 */
public interface WideLayout {
    /** Whether the mockup layout is in use on this screen. */
    boolean wide();

    /** The box drawn around a slot: {@code {offset, size}} so that the box starts at (slot.x - offset, slot.y - offset). */
    default int[] boxOf(Slot slot) {
        return wide() ? new int[]{2, 20} : new int[]{1, 18};
    }

    /** {@code {x, y, width, height}} relative to the panel for the search field, or null to put it above the panel. */
    default int[] searchRect() {
        return null;
    }

    /** Same for the sort button. */
    default int[] sortRect() {
        return null;
    }
}
