package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.FordixClient;
import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** A row with a segmented control: one segment per allowed value of a text option, the chosen one lit. */
public class ChoiceRow extends SettingRow {
    private final List<String> values;
    private final ForgeConfigSpec.ConfigValue<String> option;

    public ChoiceRow(int x, int y, int width, int height, String icon, int tint, String title, String description,
                     List<String> values, ForgeConfigSpec.ConfigValue<String> option) {
        super(x, y, width, height, icon, tint, title, description, false);
        this.values = values;
        this.option = option;
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
        for (String value : values) {
            int sw = segmentWidth(font, value);
            boolean on = value.equals(option.get());
            if (on) {
                g.fillGradient(cx + 1, cy + 1, cx + sw - 1, cy + 13, 0xFF1C6F69, 0xFF157A73);
                g.fill(cx + 1, cy + 12, cx + sw - 1, cy + 13, 0xFF000000 | accent);
            }
            g.drawString(font, value, cx + 4, cy + 3, on ? 0xFFFFFFFF : 0xFF7D93A3, false);
            cx += sw;
        }
    }

    @Override
    protected void activate(double mouseX) {
        Font font = Minecraft.getInstance().font;
        String next = null;
        if (mouseX >= 0) {
            int cx = getX() + getWidth() - 6 - controlWidth();
            for (String value : values) {
                int sw = segmentWidth(font, value);
                if (mouseX >= cx && mouseX < cx + sw) {
                    next = value;
                }
                cx += sw;
            }
        } else {
            next = values.get((Math.max(0, values.indexOf(option.get())) + 1) % values.size());
        }
        if (next != null) {
            option.set(next);
            option.save();
            FordixClient.applyConfig();
        }
    }
}
