package com.omneria.fordix.client.notification;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.anim.Easing;
import com.omneria.fordix.client.gui.vanilla.Reflect;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.notification.NotificationStyle;
import com.omneria.fordix.notification.network.ClientboundNotificationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.List;

/**
 * Draws server notifications in the configured {@link NotificationCorner}, on the HUD
 * and on top of any open screen. In the top-right corner they sit below vanilla toasts
 * and, on the HUD, the status effect icons; in a bottom corner, above the hotbar when
 * it's in the way. Layout and timing live in {@link NotificationStack}; this class
 * measures, draws and plays the arrival sound. Only called on the client, through
 * PanelNetwork's DistExecutor guard.
 */
public final class NotificationOverlay {
    private static final int MIN_WIDTH = 160;
    private static final int MAX_WIDTH = 260;
    private static final int MARGIN = 6;
    private static final int PADDING = 5;
    private static final int STRIPE = 2;
    private static final int ICON_SIZE = 10;
    private static final int LINE_HEIGHT = 9;
    private static final int SUCCESS_GREEN = 0xFF5ED68A;
    private static final Field TOAST_SLOTS = Reflect.uniqueField(ToastComponent.class, BitSet.class);
    private static final int TOAST_SLOT_HEIGHT = 32;
    /** Vanilla draws status effect icons in 24px frames, a row every 26px from the top. */
    private static final int EFFECT_ROW_HEIGHT = 26;
    /** Half the hotbar's width, and the height of hotbar, hearts and armor above the bottom edge. */
    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int HOTBAR_CLEARANCE = 52;

    private static final NotificationStack<Shown> STACK = new NotificationStack<>();

    private record Shown(NotificationStyle style, List<FormattedCharSequence> title, List<FormattedCharSequence> lines,
                         int width) {
    }

    private NotificationOverlay() {
    }

    public static void receive(ClientboundNotificationPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int overhead = STRIPE + PADDING * 3 + ICON_SIZE;
        int maxTextWidth = MAX_WIDTH - overhead;
        List<FormattedCharSequence> title = font.split(payload.title(), maxTextWidth);
        List<FormattedCharSequence> lines = payload.message().getString().isEmpty()
                ? List.of()
                : font.split(payload.message(), maxTextWidth);
        int textWidth = 0;
        for (FormattedCharSequence line : title) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        for (FormattedCharSequence line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        int width = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, textWidth + overhead));
        int height = PADDING * 2 + (title.size() + lines.size()) * LINE_HEIGHT + 4;
        STACK.add(new Shown(payload.style(), title, lines, width), height,
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
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        NotificationCorner corner = NotificationCorner.resolve(FordixConfig.NOTIFICATION_POSITION.get());
        boolean hud = minecraft.screen == null;
        int edge;
        if (corner.isTop()) {
            // Vanilla toasts are drawn over screens too; status effect icons only on the HUD.
            int effects = hud ? effectRowsHeight(minecraft) : 0;
            edge = MARGIN + (corner.isRight() ? Math.max(effects, toastsHeight(minecraft)) : 0);
        } else {
            int left = corner.isRight() ? screenWidth - MARGIN - MAX_WIDTH : MARGIN;
            boolean hotbarInTheWay = left < screenWidth / 2 + HOTBAR_HALF_WIDTH
                    && left + MAX_WIDTH > screenWidth / 2 - HOTBAR_HALF_WIDTH;
            edge = screenHeight - MARGIN - (hud && hotbarInTheWay ? HOTBAR_CLEARANCE : 0);
        }
        NotificationStack.Anchor anchor = corner.isTop() ? NotificationStack.Anchor.TOP : NotificationStack.Anchor.BOTTOM;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 5000.0F);
        for (NotificationStack.Placed<Shown> placed : STACK.layout(System.currentTimeMillis(), edge, anchor)) {
            // Slides in from the nearest side edge.
            int width = placed.payload().width();
            float hidden = (1.0F - placed.shown()) * (width + MARGIN);
            int x = corner.isRight()
                    ? Math.round(screenWidth - MARGIN - width + hidden)
                    : Math.round(MARGIN - hidden);
            drawOne(guiGraphics, minecraft.font, placed, x, Math.round(placed.y()));
        }
        guiGraphics.pose().popPose();
    }

    private static void drawOne(GuiGraphics guiGraphics, Font font, NotificationStack.Placed<Shown> placed, int x, int y) {
        Shown shown = placed.payload();
        int height = placed.height();
        int width = shown.width();
        int color = colorOf(shown.style());
        Panel.draw(guiGraphics, x, y, width, height);
        guiGraphics.fill(x, y, x + STRIPE, y + height, color);

        int iconX = x + STRIPE + PADDING;
        int iconY = y + PADDING;
        guiGraphics.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, Easing.withAlpha(color, 0.25F));
        guiGraphics.drawCenteredString(font, iconOf(shown.style()), iconX + ICON_SIZE / 2, iconY + 1, color);

        int textX = iconX + ICON_SIZE + PADDING;
        int lineY = y + PADDING + 1;
        for (FormattedCharSequence line : shown.title()) {
            guiGraphics.drawString(font, line, textX, lineY, FordixPalette.TEXT_BRIGHT, false);
            lineY += LINE_HEIGHT;
        }
        for (FormattedCharSequence line : shown.lines()) {
            guiGraphics.drawString(font, line, textX, lineY, FordixPalette.TEXT, false);
            lineY += LINE_HEIGHT;
        }

        // Time left, as a thin bar along the bottom edge.
        int barWidth = Math.round((width - STRIPE - 4) * placed.remaining());
        guiGraphics.fill(x + STRIPE + 2, y + height - 3, x + STRIPE + 2 + barWidth, y + height - 2, Easing.withAlpha(color, 0.6F));
    }

    /**
     * Height taken by vanilla toasts (advancements, recipes, tutorials) in the same
     * corner. ToastComponent keeps a BitSet of occupied 32px slots in a private field;
     * it's its only BitSet, so it's read by type. Without it, toasts are ignored.
     */
    private static int toastsHeight(Minecraft minecraft) {
        if (Reflect.get(TOAST_SLOTS, minecraft.getToasts()) instanceof BitSet occupied) {
            return occupied.length() * TOAST_SLOT_HEIGHT;
        }
        return 0;
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
