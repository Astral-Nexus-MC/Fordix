package com.omneria.fordix.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the player last saw inside a chest, barrel or shulker box: the client never receives a container's content until
 * it is opened, so the contents are remembered when the container screen closes and shown again under the crosshair.
 */
public final class ContainerMemory {
    private static final int MAX_REMEMBERED = 64;

    /** Aggregated contents and the block they were seen in (a replaced block makes the memory void). */
    public record Snapshot(String block, List<ItemStack> items, long seenAt) {
    }

    private static final Map<String, Snapshot> SNAPSHOTS = new LinkedHashMap<>();
    private static String opening;
    private static String openingBlock;

    private ContainerMemory() {
    }

    private static String key(Minecraft minecraft, BlockPos pos) {
        return minecraft.level.dimension().location() + "|" + pos.asLong();
    }

    /** A container screen opens: the block under the crosshair is the one being opened. */
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Minecraft minecraft = Minecraft.getInstance();
        opening = null;
        if (minecraft.level == null || !(event.getNewScreen() instanceof AbstractContainerScreen<?>)
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
        if (state.hasBlockEntity()) {
            opening = key(minecraft, hit.getBlockPos());
            openingBlock = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        }
    }

    /** The screen closes: keep what its container (not the player's own inventory) held. */
    public static void onScreenClosing(ScreenEvent.Closing event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (opening == null || minecraft.player == null || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container != minecraft.player.getInventory() && slot.hasItem()) {
                merge(items, slot.getItem());
            }
        }
        SNAPSHOTS.remove(opening);
        SNAPSHOTS.put(opening, new Snapshot(openingBlock, items, System.currentTimeMillis()));
        while (SNAPSHOTS.size() > MAX_REMEMBERED) {
            SNAPSHOTS.remove(SNAPSHOTS.keySet().iterator().next());
        }
        opening = null;
    }

    /** Remembers a content directly (what closing a container screen does; the in-game test uses it). */
    public static void put(Minecraft minecraft, BlockPos pos, BlockState state, List<ItemStack> items) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : items) {
            merge(merged, stack);
        }
        SNAPSHOTS.put(key(minecraft, pos), new Snapshot(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), merged,
                System.currentTimeMillis()));
    }

    /** Adds a stack to a list, merging it into an identical one already there. */
    static void merge(List<ItemStack> items, ItemStack stack) {
        for (ItemStack present : items) {
            if (ItemStack.isSameItemSameTags(present, stack)) {
                present.grow(stack.getCount());
                return;
            }
        }
        items.add(stack.copy());
    }

    /** The remembered content of the block at this position, or null (never opened, or the block is not the same any more). */
    public static Snapshot at(Minecraft minecraft, BlockPos pos, BlockState state) {
        Snapshot snapshot = SNAPSHOTS.get(key(minecraft, pos));
        if (snapshot == null || !snapshot.block().equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())) {
            return null;
        }
        return snapshot;
    }

    public static void clear() {
        SNAPSHOTS.clear();
        opening = null;
    }
}
