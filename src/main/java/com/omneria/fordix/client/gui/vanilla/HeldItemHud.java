package com.omneria.fordix.client.gui.vanilla;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.omneria.fordix.FordixConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;

/**
 * The held item's name line ("Held item name" in the layout screen) gets the item's 3D model, slowly turning, to its
 * left. It lives and moves with that overlay: it is drawn right after it, inside the same shift and size. Vanilla keeps the
 * text; the timing (40 ticks, fading over the last 10) is tracked here because vanilla's timer isn't reachable.
 */
public final class HeldItemHud {
    private static final int SHOW_TICKS = 40;
    private static final int MODEL_SIZE = 20;

    private static ItemStack last = ItemStack.EMPTY;
    private static int ticksLeft;

    private HeldItemHud() {
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            last = ItemStack.EMPTY;
            ticksLeft = 0;
            return;
        }
        ItemStack held = minecraft.player.getMainHandItem();
        if (held.isEmpty()) {
            ticksLeft = 0;
        } else if (last.isEmpty() || !ItemStack.isSameItem(last, held) || !last.getHoverName().equals(held.getHoverName())) {
            ticksLeft = SHOW_TICKS;
        } else if (ticksLeft > 0) {
            ticksLeft--;
        }
        last = held.copy();
    }

    /** After the vanilla name line: the model beside it. */
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.ITEM_NAME.type() || !FordixConfig.voidframeHud() || ticksLeft <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui instanceof ForgeGui gui) || minecraft.player == null || last.isEmpty()
                || minecraft.options.hideGui) {
            return;
        }
        int alpha = Math.min(255, (int) (ticksLeft * 256.0F / 10.0F));
        if (alpha <= 0) {
            return;
        }
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;
        Component name = Component.empty().append(last.getHoverName());
        int textWidth = font.width(name);
        int y = height - Math.max(gui.leftHeight, gui.rightHeight);
        if (minecraft.gameMode != null && !minecraft.gameMode.canHurtPlayer()) {
            y += 14;
        }
        draw(event.getGuiGraphics(), last, (width - textWidth) / 2 - MODEL_SIZE - 4, y - (MODEL_SIZE - 8) / 2, alpha, minecraft);
    }

    /** Draws the item as a small turning 3D model with its top-left corner at (x, y). */
    static void draw(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int alpha, Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        ItemRenderer renderer = minecraft.getItemRenderer();
        BakedModel model = renderer.getModel(stack, minecraft.level, minecraft.player, 0);
        float angle = (System.nanoTime() / 20_000_000L) % 360L;
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x + MODEL_SIZE / 2.0F, y + MODEL_SIZE / 2.0F, 150.0F);
        pose.scale(MODEL_SIZE, -MODEL_SIZE, MODEL_SIZE);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        boolean flat = !model.usesBlockLight();
        if (flat) {
            Lighting.setupForFlatItems();
        } else {
            Lighting.setupFor3DItems();
        }
        renderer.render(stack, ItemDisplayContext.GUI, false, pose, guiGraphics.bufferSource(), 15728880,
                OverlayTexture.NO_OVERLAY, model);
        guiGraphics.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }
}
