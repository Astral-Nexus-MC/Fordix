package com.omneria.fordix.client.gui.options;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Options > Mouse in Voidframe 2: the game's mouse options as one-line rows (sliders and switches). */
public final class MouseScreen extends OptionsPageScreen {
    public MouseScreen(Screen parent) {
        super(Component.translatable("options.mouse_settings.title"), parent);
    }

    @Override
    protected int rowHeight() {
        return 22;
    }

    @Override
    protected int buildRows(int left, int width) {
        Options o = this.minecraft.options;
        startRows(left, width);
        addValue(o.sensitivity(), "options.sensitivity", 0.0, 1.0, 0.005, 0.5, v -> Math.round(v * 200) + " %");
        addFlag(o.invertYMouse(), "options.invertMouse", false);
        addValue(o.mouseWheelSensitivity(), "options.mouseWheelSensitivity", 0.1, 10.0, 0.1, 1.0, v -> String.format(Locale.ROOT, "x%.1f", v));
        addFlag(o.discreteMouseScroll(), "options.discrete_mouse_scroll", false);
        addFlag(o.touchscreen(), "options.touchscreen", false);
        addFlag(o.rawMouseInput(), "options.rawMouseInput", true);
        return rowY;
    }
}
