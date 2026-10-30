package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.Fordix;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * The Voidframe version of two containers of Botania: the flower pouch and the bauble box. They are built on the menu Botania's own
 * screen was opened with, so the menu, the slots and what the server knows do not change: only the drawing and the place of the slots
 * on the client. Botania's screen is matched by its exact class name (another mod's subclass is left alone), there is no dependency
 * on Botania, and every piece of information read from it by reflection (the flower a pouch slot is for) has a fallback.
 * On a screen too small for the wide layout the slots keep the places Botania gives them.
 */
public final class ModContainerScreen extends AbstractContainerScreen<AbstractContainerMenu> implements VoidframeScreen, WideLayout {
    public enum Kind {
        POUCH, BOX
    }

    private static final String POUCH_SCREEN = "vazkii.botania.client.gui.bag.FlowerPouchGui";
    private static final String BOX_SCREEN = "vazkii.botania.client.gui.box.BaubleBoxGui";

    private static boolean flowerResolved;
    private static Method flowerForSlot;

    private final Kind kind;
    private final int expected;
    private boolean wide;

    private ModContainerScreen(Kind kind, AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.kind = kind;
        this.expected = kind == Kind.POUCH ? ModLayouts.POUCH_SLOTS : ModLayouts.BOX_SLOTS;
        slotColor = FordixPalette.SLOT_HOVER;
    }

    /** The Voidframe screen for a Botania container screen that is opening, or null for any other screen. */
    public static Screen replacementFor(Screen screen, Inventory inventory) {
        if (!FordixConfig.modScreens() || !(screen instanceof AbstractContainerScreen<?> container)) {
            return null;
        }
        String name = screen.getClass().getName();
        Kind kind = name.equals(POUCH_SCREEN) ? Kind.POUCH : name.equals(BOX_SCREEN) ? Kind.BOX : null;
        return kind == null ? null : new ModContainerScreen(kind, container.getMenu(), inventory, container.getTitle());
    }

    /** For the dev test: the screen of a kind on a menu of the test's making. */
    public static ModContainerScreen forTest(Kind kind, AbstractContainerMenu menu, Inventory inventory, Component title) {
        return new ModContainerScreen(kind, menu, inventory, title);
    }

    @Override
    public boolean wide() {
        return wide;
    }

    private int panelWidth() {
        return kind == Kind.POUCH ? ModLayouts.POUCH_W : ModLayouts.BOX_W;
    }

    private int panelHeight() {
        return kind == Kind.POUCH ? ModLayouts.POUCH_H : ModLayouts.BOX_H;
    }

    private int divider() {
        return kind == Kind.POUCH ? ModLayouts.POUCH_DIVIDER : ModLayouts.BOX_DIVIDER;
    }

    private int ownSlots() {
        int count = 0;
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void init() {
        // Wide only on a screen with room for it, and only when the menu has the slots this layout is made for.
        wide = ContainerLayout.fits(this, panelWidth(), panelHeight()) && ContainerLayout.ready(menu) && ownSlots() == expected;
        imageWidth = wide ? panelWidth() : 176;
        imageHeight = wide ? panelHeight() : kind == Kind.POUCH ? 202 : 166;
        inventoryLabelY = imageHeight - 94;
        super.init();
        if (!wide) {
            ContainerLayout.restore(menu);
            return;
        }
        int placed = 0;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory || placed >= expected) {
                continue;
            }
            int[] box = kind == Kind.POUCH ? ModLayouts.pouchBox(placed) : ModLayouts.boxBox(placed);
            ContainerLayout.put(slot, box[0], box[1], ContainerLayout.BOX);
            placed++;
        }
        ContainerLayout.playerBlock(menu, 10, divider() + 26, divider() + 26 + 76);
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
            if (kind == Kind.BOX) {
                int[] well = ModLayouts.BOX_PLAYER;
                VoidframeDraw.well(g, leftPos + well[0], topPos + well[1], well[2], well[3]);
                player(g, leftPos + well[0] + well[2] / 2, topPos + well[1] + well[3] - 6, 34, mouseX, mouseY);
            }
        } else {
            VoidframeDraw.containerBackground(g, menu, leftPos, topPos, imageWidth, imageHeight);
            VoidframeDraw.titleBand(g, leftPos, topPos, imageWidth);
            if (kind == Kind.BOX) {
                VoidframeDraw.well(g, leftPos + 7, topPos + 7, 49, 72);
                player(g, leftPos + 31, topPos + 75, 30, mouseX, mouseY);
            }
        }
        ghostFlowers(g);
    }

    private void player(GuiGraphics g, int x, int y, int scale, int mouseX, int mouseY) {
        if (minecraft != null && minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x, y, scale, (float) (x - mouseX), (float) (y - 50 - mouseY), minecraft.player);
        }
    }

    /** The flower an empty pouch slot is for, about a third as bright as a real one (Botania draws it the same way). */
    private void ghostFlowers(GuiGraphics g) {
        if (kind != Kind.POUCH) {
            return;
        }
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory || slot.hasItem() || !slot.isActive()) {
                continue;
            }
            Item flower = flowerFor(slot.index);
            if (flower == null) {
                continue;
            }
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            g.renderItem(new ItemStack(flower), x, y);
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 200.0F);
            g.fill(x, y, x + 16, y + 16, 0xA00B1016);
            g.pose().popPose();
        }
        RenderSystem.enableBlend();
    }

    private static Item flowerFor(int index) {
        if (!flowerResolved) {
            flowerResolved = true;
            try {
                flowerForSlot = Class.forName("vazkii.botania.common.item.FlowerPouchItem").getMethod("getFlowerForSlot", int.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                flowerForSlot = null;
                Fordix.LOGGER.info("Fordix: the flower of each pouch slot is not available, empty slots stay empty");
            }
        }
        if (flowerForSlot == null) {
            return null;
        }
        try {
            return flowerForSlot.invoke(null, index) instanceof Item item ? item : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            flowerForSlot = null;
            return null;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // The count of a stack of one is written too: a pouch slot holding one flower must not look empty.
        if (kind == Kind.POUCH) {
            for (Slot slot : menu.slots) {
                if (!(slot.container instanceof Inventory) && slot.isActive() && slot.getItem().getCount() == 1) {
                    g.drawString(font, "1", slot.x + 11, slot.y + 9, 0xFFFFFFFF, true);
                }
            }
        }
        if (wide) {
            VoidframeDraw.title(g, font, title, 14, 9);
            VoidframeDraw.fillCount(g, font, menu, expected, panelWidth() + 6, 9);
            if (kind == Kind.POUCH) {
                VoidframeDraw.label(g, font, Component.translatable("fordix.mod.small_flowers"), 12, ModLayouts.POUCH_SMALL_Y - 12);
                VoidframeDraw.label(g, font, Component.translatable("fordix.mod.tall_flowers"), 12, ModLayouts.POUCH_TALL_Y - 12);
            } else {
                VoidframeDraw.label(g, font, Component.translatable("fordix.mod.accessories_only"), ModLayouts.BOX_GRID_X, ModLayouts.BOX_GRID_Y - 12);
            }
            VoidframeDraw.label(g, font, playerInventoryTitle, 12, divider() + 10);
            g.drawString(font, Tr.s(kind == Kind.POUCH ? "mod.pouch_hint" : "mod.box_hint"), 12, divider() + 26 + 76 + 38, Glass.dim(), false);
        } else if (kind == Kind.POUCH) {
            VoidframeDraw.title(g, font, title, titleLabelX, titleLabelY);
            VoidframeDraw.label(g, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
            VoidframeDraw.fillCount(g, font, menu, expected, imageWidth, titleLabelY);
        }
    }
}
