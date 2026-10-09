package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** A row that opens a sub-screen or runs an action: a chevron (or a short word, like "Test") on the right. */
public class NavRow extends SettingRow {
    private final Runnable action;
    private final String word;

    public NavRow(int x, int y, int width, int height, String icon, int tint, String title, String description, String word, Runnable action) {
        super(x, y, width, height, icon, tint, title, description, false);
        this.action = action;
        this.word = word;
    }

    @Override
    protected int controlWidth() {
        return word == null ? 8 : Minecraft.getInstance().font.width(word) + 12;
    }

    @Override
    protected void renderControl(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int accent = 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF);
        if (word == null) {
            // The chevron leans a pixel to the right while the row is hovered.
            int lean = isHoveredOrFocused() ? 2 : 0;
            g.drawString(font, ">", x + w - 12 + lean, y + (h - 8) / 2, accent, false);
        } else {
            int bw = font.width(word) + 12;
            int bx = x + w - 6 - bw;
            g.fillGradient(bx, y + (h - 14) / 2, bx + bw, y + (h + 14) / 2, 0xFF1A2631, 0xFF101921);
            g.renderOutline(bx, y + (h - 14) / 2, bw, 14, isHoveredOrFocused() ? accent : 0xFF2A3B4A);
            g.drawString(font, word, bx + 6, y + (h - 8) / 2, 0xFFDDFFFF, false);
        }
    }

    @Override
    protected void activate(double mouseX) {
        action.run();
    }
}
