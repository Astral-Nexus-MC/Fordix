package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A button drawn with the same bevel primitive as {@link Panel} instead of vanilla's
 * stone-button texture, which can't be recolored to match Voidframe without shipping
 * a texture asset.
 */
public class FordixButton extends AbstractWidget {
    private static final int BEVEL_WIDTH = 1;

    private final Runnable onPress;

    public FordixButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int fill = this.isHoveredOrFocused() ? FordixPalette.BUTTON_FILL_HOVERED : FordixPalette.BUTTON_FILL;
        guiGraphics.fill(x, y, x + w, y + h, fill);
        guiGraphics.fill(x, y, x + w, y + BEVEL_WIDTH, FordixPalette.BEVEL_HIGHLIGHT);
        guiGraphics.fill(x, y, x + BEVEL_WIDTH, y + h, FordixPalette.BEVEL_HIGHLIGHT);
        guiGraphics.fill(x, y + h - BEVEL_WIDTH, x + w, y + h, FordixPalette.BEVEL_SHADOW);
        guiGraphics.fill(x + w - BEVEL_WIDTH, y, x + w, y + h, FordixPalette.BEVEL_SHADOW);

        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(this.getMessage());
        guiGraphics.drawString(font, this.getMessage(), x + (w - textWidth) / 2, y + (h - 8) / 2, FordixPalette.TEXT_BRIGHT, false);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
