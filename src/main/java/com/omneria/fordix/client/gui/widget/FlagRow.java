package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** A row with a switch, like {@link ToggleRow} but over any on/off value (a game option) instead of a Fordix config entry. */
public class FlagRow extends SettingRow {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private float knob;

    public FlagRow(int x, int y, int width, int height, String icon, int tint, String title, String description, boolean compact,
                   BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, width, height, icon, tint, title, description, compact);
        this.getter = getter;
        this.setter = setter;
        this.knob = getter.getAsBoolean() ? 1.0F : 0.0F;
    }

    @Override
    protected int controlWidth() {
        return 28;
    }

    @Override
    protected void renderControl(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean on = getter.getAsBoolean();
        knob += ((on ? 1.0F : 0.0F) - knob) * 0.35F;
        int tx = x + w - 6 - 26;
        int ty = y + (h - 12) / 2;
        int accent = FordixPalette.ACCENT & 0xFFFFFF;
        if (on) {
            g.renderOutline(tx - 1, ty - 1, 28, 14, 0x33000000 | accent);
            g.fillGradient(tx, ty, tx + 26, ty + 12, 0xFF1C6F69, 0xFF2FB3AA);
            g.renderOutline(tx, ty, 26, 12, 0xFF000000 | accent);
        } else {
            g.fill(tx, ty, tx + 26, ty + 12, 0xFF0A1016);
            g.renderOutline(tx, ty, 26, 12, 0xFF2A3B4A);
        }
        int kx = tx + 2 + Math.round(knob * 14);
        g.fill(kx, ty + 2, kx + 8, ty + 10, on ? 0xFFFFFFFF : 0xFF7D93A3);
    }

    @Override
    protected void activate(double mouseX) {
        setter.accept(!getter.getAsBoolean());
    }
}
