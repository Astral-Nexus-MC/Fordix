package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.Glass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A Voidframe 2 glass button ({@link Glass#button}): gradient body, light top edge, accent line and glow that ease in on
 * hover. {@link Glass.Kind#PRIMARY} marks the one main action of a screen, {@link Glass.Kind#DANGER} a destructive one.
 */
public class FordixButton extends AbstractWidget {
    private final Runnable onPress;
    private final Glass.Fade fade = new Glass.Fade();
    private Glass.Kind kind = Glass.Kind.NORMAL;
    private boolean selected;

    public FordixButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public FordixButton kind(Glass.Kind kind) {
        this.kind = kind;
        return this;
    }

    /** Marks the button as the current choice of a tab strip or list: it keeps the lit accent line and an accent label. */
    public FordixButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        float hover = Math.max(fade.update(active && isHoveredOrFocused()), selected ? 1.0F : 0.0F);
        Glass.button(guiGraphics, x, y, getWidth(), getHeight(), kind, hover, active);
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(getMessage());
        guiGraphics.drawString(font, getMessage(), x + (getWidth() - textWidth) / 2, y + (getHeight() - 8) / 2,
                selected ? com.omneria.fordix.client.hud.HudTokens.mix(com.omneria.fordix.client.gui.FordixPalette.ACCENT, 0xFFFFFFFF, 0.5F) : Glass.buttonText(kind, active), false);
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
