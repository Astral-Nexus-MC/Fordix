package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.Fordix;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Moves the slots of a container menu on the client, for the Voidframe 2 layouts. A slot's position ({@code x}, {@code y}) only
 * decides where the client draws it and where a click lands: the server knows slots by index, so nothing changes for it.
 * The two fields are final in the game's class, so they are written by reflection, found by their place among the integer
 * fields of {@link Slot} (slot, index, x, y) and checked against the values the game itself reports: when they do not
 * match, {@link #ready} is false and every screen keeps the game's layout.
 */
public final class ContainerLayout {
    public static final int PITCH = 22;
    public static final int BOX = 20;
    public static final int BIG = 26;
    private static final int MARGIN = 8;

    private static Field fieldX;
    private static Field fieldY;
    private static boolean resolved;
    private static boolean usable;
    private static final Map<Slot, int[]> ORIGINAL = new WeakHashMap<>();

    private ContainerLayout() {
    }

    /** Whether a panel of this size fits the screen with a margin: the layout is wide only then. */
    public static boolean fits(Screen screen, int width, int height) {
        return screen.width >= width + 2 * MARGIN && screen.height >= height + 2 * MARGIN;
    }

    /** Resolves the two position fields once, validated on the slots of {@code menu}. */
    public static boolean ready(AbstractContainerMenu menu) {
        if (resolved) {
            return usable;
        }
        List<Field> ints = new ArrayList<>();
        for (Field field : Slot.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
                ints.add(field);
            }
        }
        if (ints.size() == 4 && menu.slots.size() >= 4) {
            try {
                for (Field field : ints) {
                    field.setAccessible(true);
                }
                boolean matches = true;
                for (int i = 0; i < Math.min(12, menu.slots.size()); i++) {
                    Slot slot = menu.slots.get(i);
                    matches &= ints.get(2).getInt(slot) == slot.x && ints.get(3).getInt(slot) == slot.y;
                }
                if (matches) {
                    fieldX = ints.get(2);
                    fieldY = ints.get(3);
                    usable = true;
                }
            } catch (IllegalAccessException | RuntimeException e) {
                usable = false;
            }
        }
        resolved = true;
        if (!usable) {
            Fordix.LOGGER.warn("Fordix: the slot position fields were not found, containers keep the game's layout");
        }
        return usable;
    }

    private static void remember(Slot slot) {
        ORIGINAL.computeIfAbsent(slot, key -> new int[]{key.x, key.y});
    }

    /** Puts the slot so that its box (of {@code box} px) starts at (boxX, boxY): the item sits centered in it. */
    public static void put(Slot slot, int boxX, int boxY, int box) {
        if (!usable) {
            return;
        }
        remember(slot);
        set(slot, boxX + (box - 16) / 2, boxY + (box - 16) / 2);
    }

    private static void set(Slot slot, int x, int y) {
        try {
            fieldX.setInt(slot, x);
            fieldY.setInt(slot, y);
        } catch (IllegalAccessException | RuntimeException e) {
            Fordix.LOGGER.warn("Fordix: cannot move a slot: {}", e.toString());
        }
    }

    /** Gives every moved slot of the menu its original position back. */
    public static void restore(AbstractContainerMenu menu) {
        if (!usable) {
            return;
        }
        for (Slot slot : menu.slots) {
            int[] original = ORIGINAL.get(slot);
            if (original != null) {
                set(slot, original[0], original[1]);
            }
        }
    }

    /** The player's bag (3 rows) and hotbar (1 row) of a menu, wherever they sit in it. */
    public static void playerBlock(AbstractContainerMenu menu, int bagX, int bagY, int hotbarY) {
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) {
                continue;
            }
            int index = slot.getContainerSlot();
            if (index >= 9 && index < 36) {
                put(slot, bagX + (index - 9) % 9 * PITCH, bagY + (index - 9) / 9 * PITCH, BOX);
            } else if (index >= 0 && index < 9) {
                put(slot, bagX + index * PITCH, hotbarY, BOX);
            }
        }
    }

    /** Places the container's own slots (those that are not the player's) in a grid, in menu order. */
    public static void grid(AbstractContainerMenu menu, int count, int columns, int x, int y) {
        int placed = 0;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                continue;
            }
            if (placed >= count) {
                break;
            }
            put(slot, x + placed % columns * PITCH, y + placed / columns * PITCH, BOX);
            placed++;
        }
    }
}
