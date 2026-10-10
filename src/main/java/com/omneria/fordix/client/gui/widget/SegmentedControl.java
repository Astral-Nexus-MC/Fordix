package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.hud.HudTokens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A row of exclusive choices side by side (Voidframe 2 "segments"): the current one has the accent wash and a lit underline,
 * the others show a faint wash on hover. It replaces the cycle buttons of the admin forms, where every choice was hidden
 * behind a click. Its width follows its labels.
 */
public class SegmentedControl extends AbstractWidget {
    private static final int PAD = 8;

    private final List<String> labels;
    private final IntSupplier current;
    private final IntConsumer setter;

    public SegmentedControl(int x, int y, int height, List<String> labels, IntSupplier current, IntConsumer setter) {
        super(x, y, widthOf(labels), height, CommonComponents.EMPTY);
        this.labels = labels;
        this.current = current;
        this.setter = setter;
    }

    private static int widthOf(List<String> labels) {
        Font font = Minecraft.getInstance().font;
        int width = 0;
        for (String label : labels) {
            width += font.width(label) + PAD * 2;
        }
        return width;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int h = getHeight();
        g.fill(x, y, x + getWidth(), y + h, 0x66000000);
        g.fill(x, y, x + getWidth(), y + 1, 0x20FFFFFF);
        g.fill(x, y + h - 1, x + getWidth(), y + h, 0x20FFFFFF);
        g.fill(x, y + 1, x + 1, y + h - 1, 0x20FFFFFF);
        g.fill(x + getWidth() - 1, y + 1, x + getWidth(), y + h - 1, 0x20FFFFFF);
        int cursor = x;
        for (int i = 0; i < labels.size(); i++) {
            int width = font.width(labels.get(i)) + PAD * 2;
            boolean on = i == current.getAsInt();
            boolean hover = mouseX >= cursor && mouseX < cursor + width && mouseY >= y && mouseY < y + h;
            if (on) {
                g.fill(cursor + 1, y + 1, cursor + width - 1, y + h - 1, HudTokens.withAlpha(FordixPalette.ACCENT, 0x30));
                g.fill(cursor + 1, y + h - 3, cursor + width - 1, y + h - 1, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
            } else if (hover) {
                g.fill(cursor + 1, y + 1, cursor + width - 1, y + h - 1, 0x14FFFFFF);
            }
            g.drawString(font, labels.get(i), cursor + PAD, y + (h - 8) / 2 - (on ? 1 : 0),
                    on ? FordixPalette.TEXT_BRIGHT : Glass.dim(), false);
            cursor += width;
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int cursor = getX();
        for (int i = 0; i < labels.size(); i++) {
            int width = font.width(labels.get(i)) + PAD * 2;
            if (mouseX >= cursor && mouseX < cursor + width) {
                setter.accept(i);
                return;
            }
            cursor += width;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
