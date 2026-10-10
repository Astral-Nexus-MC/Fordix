package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.Glass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * A text field in the Voidframe 2 look: a recessed dark field whose edge turns to the accent (with a soft glow) while it has
 * the focus. It is an ordinary {@link EditBox} (same keys, same suggestions, same API); only the background and the text
 * inset differ.
 */
public class FieldBox extends EditBox {
    private static final int INSET = 5;
    private boolean topAligned;

    /** Draws the text from the top of the field (a tall field for a message) instead of centered. */
    public FieldBox topAligned(boolean topAligned) {
        this.topAligned = topAligned;
        return this;
    }

    public FieldBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        setBordered(false);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        Glass.field(g, x, y, width, height, isFocused());
        // The box draws its text from its own corner when it has no border: move it inside the field for the draw.
        setX(x + INSET);
        setY(topAligned ? y + 7 : y + (height - 8) / 2);
        setWidth(width - INSET * 2);
        super.renderWidget(g, mouseX, mouseY, partialTick);
        setX(x);
        setY(y);
        setWidth(width);
    }
}
