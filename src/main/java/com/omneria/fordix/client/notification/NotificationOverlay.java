package com.omneria.fordix.client.notification;

import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.anim.Easing;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.notification.NotificationStyle;
import com.omneria.fordix.notification.network.ClientboundNotificationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.List;

/**
 * Draws server notifications in the top-right corner, on the HUD and on top of
 * any open screen - below the status effect icons when the HUD shows some. Layout and timing live in {@link NotificationStack}; this class
 * measures, draws and plays the arrival sound. Only called on the client, through
 * PanelNetwork's DistExecutor guard.
 */
public final class NotificationOverlay {
    private static final int WIDTH = 160;
    private static final int MARGIN = 6;
    private static final int PADDING = 5;
    private static final int STRIPE = 2;
    private static final int ICON_SIZE = 10;
    private static final int LINE_HEIGHT = 9;
    private static final int SUCCESS_GREEN = 0xFF5ED68A;
    /** Vanilla draws status effect icons in 24px frames, a row every 26px from the top. */
    private static final int EFFECT_ROW_HEIGHT = 26;

    private static final NotificationStack<Shown> STACK = new NotificationStack<>();

    private record Shown(NotificationStyle style, Component title, List<FormattedCharSequence> lines) {
    }

    private NotificationOverlay() {
    }

    public static void receive(ClientboundNotificationPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int textWidth = WIDTH - STRIPE - PADDING * 3 - ICON_SIZE;
        List<FormattedCharSequence> lines = payload.message().getString().isEmpty()
                ? List.of()
                : font.split(payload.message(), textWidth);
        int height = PADDING * 2 + LINE_HEIGHT + lines.size() * LINE_HEIGHT + 3;
        STACK.add(new Shown(payload.style(), payload.title(), lines), height,
                System.currentTimeMillis(), payload.durationTicks() * 50L);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.6F, 0.35F));
    }

    /** In-game HUD. Skipped while a screen is open - {@link #onScreenRenderPost} draws on top of it instead. */
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) {
            draw(event.getGuiGraphics());
        }
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        draw(event.getGuiGraphics());
    }

    private static void draw(GuiGraphics guiGraphics) {
        if (STACK.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int top = MARGIN + (minecraft.screen == null ? effectRowsHeight(minecraft) : 0);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
        for (NotificationStack.Placed<Shown> placed : STACK.layout(System.currentTimeMillis(), top, NotificationStack.Anchor.TOP)) {
            int x = Math.round(screenWidth - MARGIN - WIDTH + (1.0F - placed.shown()) * (WIDTH + MARGIN));
            drawOne(guiGraphics, minecraft.font, placed, x, Math.round(placed.y()));
        }
        guiGraphics.pose().popPose();
    }

    private static void drawOne(GuiGraphics guiGraphics, Font font, NotificationStack.Placed<Shown> placed, int x, int y) {
        Shown shown = placed.payload();
        int height = placed.height();
        int color = colorOf(shown.style());
        Panel.draw(guiGraphics, x, y, WIDTH, height);
        guiGraphics.fill(x, y, x + STRIPE, y + height, color);

        int iconX = x + STRIPE + PADDING;
        int iconY = y + PADDING;
        guiGraphics.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, Easing.withAlpha(color, 0.25F));
        guiGraphics.drawCenteredString(font, iconOf(shown.style()), iconX + ICON_SIZE / 2, iconY + 1, color);

        int textX = iconX + ICON_SIZE + PADDING;
        guiGraphics.drawString(font, shown.title(), textX, y + PADDING + 1, FordixPalette.TEXT_BRIGHT, false);
        int lineY = y + PADDING + 1 + LINE_HEIGHT;
        for (FormattedCharSequence line : shown.lines()) {
            guiGraphics.drawString(font, line, textX, lineY, FordixPalette.TEXT, false);
            lineY += LINE_HEIGHT;
        }

        // Time left, as a thin bar along the bottom edge.
        int barWidth = Math.round((WIDTH - STRIPE - 4) * placed.remaining());
        guiGraphics.fill(x + STRIPE + 2, y + height - 3, x + STRIPE + 2 + barWidth, y + height - 2, Easing.withAlpha(color, 0.6F));
    }

    /**
     * Height taken by the status effect icons the HUD draws in the top-right corner.
     * Vanilla puts beneficial effects on the first row and harmful ones always on the
     * second, even when the first is empty.
     */
    private static int effectRowsHeight(Minecraft minecraft) {
        if (minecraft.player == null) {
            return 0;
        }
        boolean beneficial = false;
        boolean harmful = false;
        for (MobEffectInstance effect : minecraft.player.getActiveEffects()) {
            if (!effect.showIcon()) {
                continue;
            }
            if (effect.getEffect().isBeneficial()) {
                beneficial = true;
            } else {
                harmful = true;
            }
        }
        return harmful ? 2 * EFFECT_ROW_HEIGHT : beneficial ? EFFECT_ROW_HEIGHT : 0;
    }

    private static int colorOf(NotificationStyle style) {
        return switch (style) {
            case INFO -> FordixPalette.ACCENT;
            case SUCCESS -> SUCCESS_GREEN;
            case WARNING -> FordixPalette.FLAME;
            case ERROR -> FordixPalette.ERROR;
        };
    }

    private static String iconOf(NotificationStyle style) {
        return switch (style) {
            case INFO -> "i";
            case SUCCESS -> "✔";
            case WARNING -> "!";
            case ERROR -> "✖";
        };
    }
}
