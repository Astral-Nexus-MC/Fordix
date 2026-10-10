package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.anim.Easing;
import com.omneria.fordix.client.tooltip.ItemRarity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * Effects layered on every Voidframe screen through Forge events, so they live in
 * one place instead of in each vanilla-screen subclass: the opening "pop", the
 * fading accent outline on the hovered slot, and Voidframe-colored tooltips.
 * Only one screen is open at a time, hence the plain static state.
 */
public final class VoidframeChrome {
    private static final long OPEN_DURATION_MS = 160L;
    private static final float OPEN_SLIDE = 8.0F;
    private static final long HOVER_FADE_MS = 150L;

    private static Screen animatedScreen;
    private static long openedAt;
    private static boolean scalePushed;

    private static Slot hoveredSlot;
    private static long hoveredSince;

    private VoidframeChrome() {
    }

    /**
     * Fired right after the dimmed world backdrop is drawn: slides everything drawn after it (the container itself) up
     * from a few pixels lower, so the backdrop stays still while the panel settles in. Text and slots stay at their
     * normal size (a zoom would blur them). Undone in {@link #onRenderPost}.
     */
    public static void onBackgroundRendered(ScreenEvent.BackgroundRendered event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof VoidframeScreen) || scalePushed) {
            return;
        }
        long now = System.currentTimeMillis();
        if (screen != animatedScreen) {
            animatedScreen = screen;
            openedAt = now;
        }
        float t = Easing.easeOutCubic(Easing.progress(now - openedAt, OPEN_DURATION_MS));
        if (t >= 1.0F) {
            return;
        }
        GuiGraphics guiGraphics = event.getGuiGraphics();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, Math.round((1.0F - t) * OPEN_SLIDE), 0.0F);
        scalePushed = true;
    }

    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (scalePushed) {
            event.getGuiGraphics().pose().popPose();
            scalePushed = false;
        }
    }

    /** Drawn after slots and labels, already translated to the container's top-left corner. */
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!(screen instanceof VoidframeScreen)) {
            return;
        }
        Slot slot = screen.getSlotUnderMouse();
        long now = System.currentTimeMillis();
        if (slot != hoveredSlot) {
            hoveredSlot = slot;
            hoveredSince = now;
        }
        if (slot == null || !slot.isActive()) {
            return;
        }
        float alpha = Easing.progress(now - hoveredSince, HOVER_FADE_MS);
        int rgb = FordixPalette.ACCENT & 0xFFFFFF;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int[] boxSpec = screen instanceof WideLayout wideScreen ? wideScreen.boxOf(slot) : new int[]{1, 18};
        int size = boxSpec[1];
        int x = slot.x - boxSpec[0];
        int y = slot.y - boxSpec[0];
        int soft = Easing.withAlpha(rgb | 0xFF000000, alpha * 0.16F);
        int glow = Easing.withAlpha(rgb | 0xFF000000, alpha * 0.22F);
        int line = Easing.withAlpha(rgb | 0xFF000000, alpha);
        guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, soft);
        guiGraphics.fill(x - 1, y - 1, x + size + 1, y, glow);
        guiGraphics.fill(x - 1, y + size, x + size + 1, y + size + 1, glow);
        guiGraphics.fill(x - 1, y, x, y + size, glow);
        guiGraphics.fill(x + size, y, x + size + 1, y + size, glow);
        guiGraphics.fill(x, y, x + size, y + 1, line);
        guiGraphics.fill(x, y + size - 1, x + size, y + size, line);
        guiGraphics.fill(x, y + 1, x + 1, y + size - 1, line);
        guiGraphics.fill(x + size - 1, y + 1, x + size, y + size - 1, line);
    }

    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        if (!FordixConfig.VOIDFRAME_VANILLA_SCREENS.get()) {
            return;
        }
        event.setBackground(FordixPalette.TOOLTIP_FILL);
        event.setBorderStart(FordixPalette.TOOLTIP_BORDER_TOP);
        event.setBorderEnd(FordixPalette.TOOLTIP_BORDER_BOTTOM);
        // Uncommon-or-better items get a border in their rarity color, like the tooltip title.
        ItemStack stack = event.getItemStack();
        if (ItemRarity.isNotable(stack)) {
            int color = ItemRarity.color(stack);
            event.setBorderStart(color);
            event.setBorderEnd(0xFF000000 | ((color & 0xFEFEFE) >> 1));
        }
    }
}
