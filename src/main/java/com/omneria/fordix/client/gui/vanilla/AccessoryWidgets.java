package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.function.Supplier;

/** The small controls of the accessory bar: the button over the player, the pager, the cosmetic switch, the eye on a slot. */
final class AccessoryWidgets {
    private AccessoryWidgets() {
    }

    /** A control placed from the corner of the panel, which moves when the recipe book slides the panel aside. */
    abstract static class Anchored extends AbstractWidget {
        private final int relX;
        private final int relY;

        Anchored(int relX, int relY, int width, int height, Component message) {
            super(0, 0, width, height, message);
            this.relX = relX;
            this.relY = relY;
        }

        void follow(int left, int top) {
            setX(left + relX);
            setY(top + relY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** The button above the player that opens and closes the accessory bar: the same place on both sides of the switch. */
    static final class Toggle extends Anchored {
        private final boolean open;
        private final Runnable action;
        private final Glass.Fade fade = new Glass.Fade();

        Toggle(int relX, int relY, boolean open, Component tip, Runnable action) {
            super(relX, relY, 18, 18, tip);
            this.open = open;
            this.action = action;
            setTooltip(Tooltip.create(tip));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Glass.button(g, getX(), getY(), width, height, open ? Glass.Kind.PRIMARY : Glass.Kind.NORMAL,
                    fade.update(isHoveredOrFocused()), true);
            int color = isHoveredOrFocused() ? FordixPalette.TEXT_BRIGHT : FordixPalette.ACCENT;
            int cx = getX() + width / 2;
            int cy = getY() + height / 2 + 1;
            // A ring with its stone: the accessory slots.
            g.fill(cx - 2, cy - 4, cx + 2, cy - 3, color);
            g.fill(cx - 3, cy - 3, cx - 2, cy - 2, color);
            g.fill(cx + 2, cy - 3, cx + 3, cy - 2, color);
            g.fill(cx - 4, cy - 2, cx - 3, cy + 2, color);
            g.fill(cx + 3, cy - 2, cx + 4, cy + 2, color);
            g.fill(cx - 3, cy + 2, cx - 2, cy + 3, color);
            g.fill(cx + 2, cy + 2, cx + 3, cy + 3, color);
            g.fill(cx - 2, cy + 3, cx + 2, cy + 4, color);
            g.fill(cx - 1, cy - 7, cx + 1, cy - 4, open ? 0xFFFFFFFF : color);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }
    }

    /** A small text button: the pages "<" and ">", the cosmetic switch "C". */
    static final class Small extends Anchored {
        private final Supplier<String> label;
        private final Supplier<Boolean> lit;
        private final Runnable action;
        private final Glass.Fade fade = new Glass.Fade();

        Small(int relX, int relY, Supplier<String> label, Supplier<Boolean> lit, Component tip, Runnable action) {
            super(relX, relY, 14, 14, tip);
            this.label = label;
            this.lit = lit;
            this.action = action;
            setTooltip(Tooltip.create(tip));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Glass.Kind kind = lit.get() ? Glass.Kind.PRIMARY : Glass.Kind.NORMAL;
            Glass.button(g, getX(), getY(), width, height, kind, fade.update(isHoveredOrFocused()), true);
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            String text = label.get();
            g.drawString(font, text, getX() + (width - font.width(text)) / 2, getY() + (height - 8) / 2 + 1,
                    Glass.buttonText(kind, true), false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }
    }

    /** The eye on an accessory slot: whether what it holds shows on the player. Drawn over the items by the screen. */
    static final class Eye extends Anchored {
        private final Slot slot;
        private final Runnable action;

        Eye(Slot slot, Runnable action) {
            super(slot.x + 11, slot.y - 3, 8, 8, Component.translatable("gui.curios.toggle"));
            this.slot = slot;
            this.action = action;
        }

        /** What the slot shows on the player: re-read at every frame, the server changes it. */
        boolean shown() {
            return !CuriosLink.test(slot, "canToggleRender") || CuriosLink.test(slot, "getRenderStatus");
        }

        void drawOverlay(GuiGraphics g, int mouseX, int mouseY) {
            int x = getX();
            int y = getY();
            int color = shown() ? FordixPalette.ACCENT : Glass.dim();
            if (isHoveredOrFocused()) {
                color = FordixPalette.TEXT_BRIGHT;
            }
            int edge = 0xFF000000 | (color & 0xFFFFFF);
            g.fill(x + 1, y, x + 7, y + 1, edge);
            g.fill(x + 1, y + 7, x + 7, y + 8, edge);
            g.fill(x, y + 1, x + 1, y + 7, edge);
            g.fill(x + 7, y + 1, x + 8, y + 7, edge);
            if (shown()) {
                g.fill(x + 2, y + 2, x + 6, y + 6, edge);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            // Drawn by the screen over the items.
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }
    }
}
