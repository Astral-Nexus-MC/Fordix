package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** A row with segments, like {@link ChoiceRow} but over any list of choices (a game option) instead of a Fordix config entry. */
public class PickRow extends SettingRow {
    private final List<String> values;
    private final IntSupplier getter;
    private final IntConsumer setter;

    public PickRow(int x, int y, int width, int height, String icon, int tint, String title, String description, boolean compact,
                   List<String> values, IntSupplier getter, IntConsumer setter) {
        super(x, y, width, height, icon, tint, title, description, compact);
        this.values = values;
        this.getter = getter;
        this.setter = setter;
    }

    private int segmentWidth(Font font, String value) {
        return font.width(value) + 8;
    }

    @Override
    protected int controlWidth() {
        Font font = Minecraft.getInstance().font;
        int total = 0;
        for (String value : values) {
            total += segmentWidth(font, value);
        }
        return total;
    }

    @Override
    protected void renderControl(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        int accent = FordixPalette.ACCENT & 0xFFFFFF;
        int cx = x + w - 6 - controlWidth();
        int cy = y + (h - 14) / 2;
        g.fill(cx, cy, cx + controlWidth(), cy + 14, 0xFF0A1016);
        g.renderOutline(cx, cy, controlWidth(), 14, 0xFF2A3B4A);
        for (int i = 0; i < values.size(); i++) {
            int sw = segmentWidth(font, values.get(i));
            boolean on = i == getter.getAsInt();
            if (on) {
                g.fillGradient(cx + 1, cy + 1, cx + sw - 1, cy + 13, 0xFF1C6F69, 0xFF157A73);
                g.fill(cx + 1, cy + 12, cx + sw - 1, cy + 13, 0xFF000000 | accent);
            }
            g.drawString(font, values.get(i), cx + 4, cy + 3, on ? 0xFFFFFFFF : 0xFF7D93A3, false);
            cx += sw;
        }
    }

    @Override
    protected void activate(double mouseX) {
        Font font = Minecraft.getInstance().font;
        if (mouseX >= 0) {
            int cx = getX() + getWidth() - 6 - controlWidth();
            for (int i = 0; i < values.size(); i++) {
                int sw = segmentWidth(font, values.get(i));
                if (mouseX >= cx && mouseX < cx + sw) {
                    setter.accept(i);
                    return;
                }
                cx += sw;
            }
        } else {
            setter.accept((getter.getAsInt() + 1) % values.size());
        }
    }
}
