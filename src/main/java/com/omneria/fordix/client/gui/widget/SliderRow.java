package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.layout.SliderMath;
import com.omneria.fordix.client.hud.HudTokens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

/**
 * A setting row with a slider: a recessed track filled with the accent, a thumb, a thin mark at the default value and the value
 * written at the right. Drag it, click on it or use the left and right arrows while it has the focus.
 */
public class SliderRow extends SettingRow {
    private static final int VALUE_WIDTH = 44;
    private static final int GAP = 6;

    private final double min;
    private final double max;
    private final double step;
    private final double defaultValue;
    private final int trackWidth;
    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private final DoubleFunction<String> format;
    private boolean dragging;

    public SliderRow(int x, int y, int width, int height, String icon, int tint, String title, String description, boolean compact,
                     double min, double max, double step, double defaultValue, int trackWidth,
                     DoubleSupplier getter, DoubleConsumer setter, DoubleFunction<String> format) {
        super(x, y, width, height, icon, tint, title, description, compact);
        this.min = min;
        this.max = max;
        this.step = step;
        this.defaultValue = defaultValue;
        this.trackWidth = trackWidth;
        this.getter = getter;
        this.setter = setter;
        this.format = format;
    }

    @Override
    protected int controlWidth() {
        return trackWidth + GAP + VALUE_WIDTH;
    }

    private int trackLeft() {
        return getX() + getWidth() - 6 - VALUE_WIDTH - GAP - trackWidth;
    }

    /** Puts the value back to the one the game starts with. */
    public void reset() {
        setter.accept(defaultValue);
    }

    @Override
    protected void renderControl(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        double value = getter.getAsDouble();
        double fraction = SliderMath.fraction(value, min, max);
        int left = trackLeft();
        int top = y + h / 2 - 2;
        int accent = FordixPalette.ACCENT;
        g.fill(left, top, left + trackWidth, top + 4, 0xFF0A1016);
        g.fill(left, top, left + trackWidth, top + 1, 0xB0000000);
        g.fill(left, top + 4, left + trackWidth, top + 5, 0x14FFFFFF);
        int filled = Math.round((float) (fraction * trackWidth));
        if (filled > 0) {
            g.fillGradient(left, top, left + filled, top + 4, HudTokens.withAlpha(accent, 0x90), HudTokens.withAlpha(accent, 0xFF));
        }
        int mark = left + Math.round((float) (SliderMath.fraction(defaultValue, min, max) * trackWidth));
        g.fill(mark, top - 2, mark + 1, top + 6, 0x4DFFFFFF);
        boolean hot = this.active && (isHoveredOrFocused() || dragging);
        int thumb = left + Math.round((float) (fraction * (trackWidth - 1))) - 2;
        if (hot) {
            g.fill(thumb - 1, y + h / 2 - 7, thumb + 6, y + h / 2 + 7, HudTokens.withAlpha(accent, 0x30));
        }
        g.fill(thumb, y + h / 2 - 6, thumb + 5, y + h / 2 + 6, HudTokens.withAlpha(accent, 0xFF));
        g.fill(thumb + 1, y + h / 2 - 5, thumb + 4, y + h / 2 + 5, 0xFFD8F6F3);
        String text = format.apply(value);
        g.drawString(font, text, x + w - 6 - font.width(text), y + (h - 8) / 2, FordixPalette.TEXT_BRIGHT, false);
    }

    private void setFrom(double mouseX) {
        double next = SliderMath.valueAt(SliderMath.fractionAt(mouseX, trackLeft(), trackWidth), min, max, step);
        if (next != getter.getAsDouble()) {
            setter.accept(next);
        }
    }

    @Override
    protected void activate(double mouseX) {
        if (mouseX >= trackLeft() - 4 && mouseX <= trackLeft() + trackWidth + 4) {
            dragging = true;
            setFrom(mouseX);
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (dragging) {
            setFrom(mouseX);
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        dragging = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.active && this.visible && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            setter.accept(SliderMath.nudge(getter.getAsDouble(), keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1, min, max, step));
            return true;
        }
        return false;
    }
}
