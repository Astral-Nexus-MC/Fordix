package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Restyles the plain vanilla buttons inside Voidframe screens. Each one is swapped
 * for a stand-in that draws the Fordix bevel and forwards clicks to the original's
 * own onPress - so whatever the screen wired to it still runs - and follows the
 * original's position, which some screens move (the recipe book button slides when
 * the book opens). Only exact Button/ImageButton classes are swapped; custom
 * subclasses (beacon powers, trade offers) draw themselves and are left alone.
 */
public final class VoidframeButtons {

    private VoidframeButtons() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof VoidframeScreen)) {
            return;
        }
        List<Button> originals = new ArrayList<>();
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener.getClass() == Button.class || listener.getClass() == ImageButton.class) {
                originals.add((Button) listener);
            }
        }
        for (Button original : originals) {
            event.removeListener(original);
            StandIn standIn = new StandIn(original);
            event.addListener(standIn);
            // Room next to the recipe book toggle on the inventory: a shortcut to the advancements.
            if (original instanceof ImageButton && event.getScreen() instanceof VoidframeScreens.PlayerInventory) {
                event.addListener(new AdvancementsButton(standIn));
            }
        }
    }

    /** A trophy button beside the recipe book toggle, opening the advancements screen. */
    static final class AdvancementsButton extends AbstractWidget {
        private static final int BEVEL_WIDTH = 1;
        private static final int GAP = 4;
        private final StandIn recipeToggle;

        AdvancementsButton(StandIn recipeToggle) {
            super(recipeToggle.getX() + recipeToggle.getWidth() + GAP, recipeToggle.getY(),
                    recipeToggle.getWidth(), recipeToggle.getHeight(), Component.translatable("gui.advancements"));
            this.recipeToggle = recipeToggle;
            setTooltip(Tooltip.create(getMessage()));
        }

        /** Follows the toggle, which slides sideways when the recipe book opens. */
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            setX(recipeToggle.getX() + recipeToggle.getWidth() + GAP);
            setY(recipeToggle.getY());
            visible = recipeToggle.visible;
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int fill = isHoveredOrFocused() ? FordixPalette.BUTTON_FILL_HOVERED : FordixPalette.BUTTON_FILL;
            guiGraphics.fill(x, y, x + w, y + h, fill);
            guiGraphics.fill(x, y, x + w, y + BEVEL_WIDTH, FordixPalette.BEVEL_HIGHLIGHT);
            guiGraphics.fill(x, y, x + BEVEL_WIDTH, y + h, FordixPalette.BEVEL_HIGHLIGHT);
            guiGraphics.fill(x, y + h - BEVEL_WIDTH, x + w, y + h, FordixPalette.BEVEL_SHADOW);
            guiGraphics.fill(x + w - BEVEL_WIDTH, y, x + w, y + h, FordixPalette.BEVEL_SHADOW);
            drawTrophy(guiGraphics, x + w / 2, y + h / 2, isHoveredOrFocused());
        }

        private static void drawTrophy(GuiGraphics guiGraphics, int cx, int cy, boolean hovered) {
            int color = hovered ? FordixPalette.TEXT_BRIGHT : FordixPalette.ACCENT;
            guiGraphics.fill(cx - 4, cy - 5, cx + 4, cy - 1, color);      // cup
            guiGraphics.fill(cx - 3, cy - 1, cx + 3, cy + 1, color);      // cup, narrowing
            guiGraphics.fill(cx - 1, cy + 1, cx + 1, cy + 3, color);      // stem
            guiGraphics.fill(cx - 3, cy + 3, cx + 3, cy + 4, color);      // base
            guiGraphics.fill(cx - 6, cy - 4, cx - 4, cy - 3, color);      // handles
            guiGraphics.fill(cx - 6, cy - 3, cx - 5, cy - 1, color);
            guiGraphics.fill(cx + 4, cy - 4, cx + 6, cy - 3, color);
            guiGraphics.fill(cx + 5, cy - 3, cx + 6, cy - 1, color);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements()));
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    static final class StandIn extends AbstractWidget {
        private static final int BEVEL_WIDTH = 1;
        private final Button original;

        StandIn(Button original) {
            super(original.getX(), original.getY(), original.getWidth(), original.getHeight(), original.getMessage());
            this.original = original;
        }

        /** Syncs before vanilla's visibility check, so a hidden stand-in still picks up the original becoming visible. */
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            syncFromOriginal();
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int fill = isHoveredOrFocused() ? FordixPalette.BUTTON_FILL_HOVERED : FordixPalette.BUTTON_FILL;
            guiGraphics.fill(x, y, x + w, y + h, fill);
            guiGraphics.fill(x, y, x + w, y + BEVEL_WIDTH, FordixPalette.BEVEL_HIGHLIGHT);
            guiGraphics.fill(x, y, x + BEVEL_WIDTH, y + h, FordixPalette.BEVEL_HIGHLIGHT);
            guiGraphics.fill(x, y + h - BEVEL_WIDTH, x + w, y + h, FordixPalette.BEVEL_SHADOW);
            guiGraphics.fill(x + w - BEVEL_WIDTH, y, x + w, y + h, FordixPalette.BEVEL_SHADOW);

            if (original instanceof ImageButton) {
                // Image buttons in these screens are the recipe book toggle: draw a small book glyph.
                drawBook(guiGraphics, x + w / 2, y + h / 2, isHoveredOrFocused());
            } else {
                Font font = Minecraft.getInstance().font;
                int color = active ? FordixPalette.TEXT_BRIGHT : FordixPalette.TEXT;
                guiGraphics.drawCenteredString(font, getMessage(), x + w / 2, y + (h - 8) / 2, color);
            }
        }

        private static void drawBook(GuiGraphics guiGraphics, int centerX, int centerY, boolean hovered) {
            int cover = hovered ? FordixPalette.TEXT_BRIGHT : FordixPalette.ACCENT;
            guiGraphics.fill(centerX - 6, centerY - 5, centerX + 6, centerY + 5, cover);
            guiGraphics.fill(centerX - 5, centerY - 4, centerX - 1, centerY + 4, FordixPalette.PANEL_FILL);
            guiGraphics.fill(centerX + 1, centerY - 4, centerX + 5, centerY + 4, FordixPalette.PANEL_FILL);
            guiGraphics.fill(centerX - 4, centerY - 2, centerX - 2, centerY - 1, cover);
            guiGraphics.fill(centerX + 2, centerY - 2, centerX + 4, centerY - 1, cover);
            guiGraphics.fill(centerX - 4, centerY + 1, centerX - 2, centerY + 2, cover);
            guiGraphics.fill(centerX + 2, centerY + 1, centerX + 4, centerY + 2, cover);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            original.onPress();
            syncFromOriginal();
        }

        private void syncFromOriginal() {
            setX(original.getX());
            setY(original.getY());
            visible = original.visible;
            active = original.active;
            setMessage(original.getMessage());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
