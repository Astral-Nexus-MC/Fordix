package com.omneria.fordix.client.gui.options;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.FordixClient;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.FordixSettingsScreen;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.widget.FlagRow;
import com.omneria.fordix.client.gui.widget.NavRow;
import com.omneria.fordix.client.gui.widget.PickRow;
import com.omneria.fordix.client.gui.widget.SliderRow;
import com.omneria.fordix.client.gui.widget.ToggleRow;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Options > Video in Voidframe 2: three tabs (display, quality, interface), a row per setting with a short sentence about it and a
 * three-bar mark for what it costs in frames per second. The values are the game's own {@link OptionInstance}s.
 */
public final class VideoScreen extends OptionsPageScreen {
    private static final int TINT_CHUNK = 0x3FD1C7;

    public VideoScreen(Screen parent) {
        super(Component.translatable("options.videoTitle"), parent);
    }

    @Override
    protected List<String> tabNames() {
        return List.of(Tr.s("options.video.tab.display"), Tr.s("options.video.tab.quality"), Tr.s("options.video.tab.interface"));
    }

    private static String vanilla(String key) {
        return Component.translatable(key).getString();
    }

    private int rowW;
    private int rowX;
    private int rowY;

    /** A description, or none on a small screen where the row is one line. */
    private String text(String key) {
        return small ? "" : Tr.s(key);
    }

    private void number(OptionInstance<Integer> option, String icon, String titleKey, String descKey, int cost, int min, int max, int step,
                            int def, Function<Integer, String> format) {
        SliderRow row = new SliderRow(rowX, rowY, rowW, rowHeight(), icon, TINT_CHUNK, vanilla(titleKey), text(descKey), small,
                min, max, step, def, small ? 80 : 130, () -> option.get(), v -> option.set((int) Math.round(v)), v -> format.apply((int) Math.round(v)));
        row.cost(cost);
        addRow(row, () -> option.set(def));
        rowY += rowHeight() + rowGap();
    }

    private void flag(OptionInstance<Boolean> option, String icon, String titleKey, String descKey, int cost, boolean def) {
        FlagRow row = new FlagRow(rowX, rowY, rowW, rowHeight(), icon, TINT_CHUNK, vanilla(titleKey), text(descKey), small,
                () -> option.get(), v -> option.set(v));
        row.cost(cost);
        addRow(row, () -> option.set(def));
        rowY += rowHeight() + rowGap();
    }

    private void pick(String icon, String titleKey, String descKey, int cost, List<String> values, java.util.function.IntSupplier get,
                      java.util.function.IntConsumer set, int def) {
        PickRow row = new PickRow(rowX, rowY, rowW, rowHeight(), icon, TINT_CHUNK, vanilla(titleKey), text(descKey), small, values, get, set);
        row.cost(cost);
        addRow(row, () -> set.accept(def));
        rowY += rowHeight() + rowGap();
    }

    @Override
    protected int buildRows(int left, int width) {
        Options o = this.minecraft.options;
        rowX = left;
        rowW = width;
        rowY = listTop + 2;
        switch (tab) {
            case 0 -> {
                number(o.renderDistance(), "D", "options.renderDistance", "options.video.desc.render", 3, 2, 32, 1, 12, v -> Tr.s("options.video.chunks", v));
                number(o.simulationDistance(), "S", "options.simulationDistance", "options.video.desc.simulation", 2, 5, 32, 1, 12, v -> Tr.s("options.video.chunks", v));
                number(o.framerateLimit(), "F", "options.framerateLimit", "options.video.desc.fps", 0, 10, 260, 10, 120,
                        v -> v >= 260 ? vanilla("options.framerateLimit.max") : v + " FPS");
                flag(o.enableVsync(), "V", "options.vsync", "options.video.desc.vsync", 1, true);
                ToggleRow fullscreen = new ToggleRow(rowX, rowY, rowW, rowHeight(), "W", 0x4DA3FF, Tr.s("options.borderless_fullscreen"),
                        text("settings.desc.fullscreen"), small, FordixConfig.BORDERLESS_FULLSCREEN);
                addRow(fullscreen, () -> {
                    FordixConfig.BORDERLESS_FULLSCREEN.set(false);
                    FordixConfig.BORDERLESS_FULLSCREEN.save();
                    FordixClient.applyConfig();
                });
                rowY += rowHeight() + rowGap();
            }
            case 1 -> {
                pick("G", "options.graphics", "options.video.desc.graphics", 2,
                        List.of(vanilla("options.graphics.fast"), vanilla("options.graphics.fancy"), vanilla("options.graphics.fabulous")),
                        () -> o.graphicsMode().get().ordinal(), i -> o.graphicsMode().set(GraphicsStatus.values()[i]), 1);
                pick("C", "options.renderClouds", "options.video.desc.clouds", 2,
                        List.of(vanilla("options.off"), vanilla("options.clouds.fast"), vanilla("options.clouds.fancy")),
                        () -> o.cloudStatus().get().ordinal(), i -> o.cloudStatus().set(CloudStatus.values()[i]), 2);
                pick("P", "options.particles", "options.video.desc.particles", 1,
                        List.of(vanilla("options.particles.all"), vanilla("options.particles.decreased"), vanilla("options.particles.minimal")),
                        () -> o.particles().get().ordinal(), i -> o.particles().set(ParticleStatus.values()[i]), 0);
                number(o.biomeBlendRadius(), "B", "options.biomeBlendRadius", "options.video.desc.biome", 1, 0, 7, 1, 2, v -> String.valueOf(v));
                flag(o.entityShadows(), "O", "options.entityShadows", "options.video.desc.shadows", 1, true);
            }
            default -> {
                pick("U", "options.guiScale", "options.video.desc.scale", 0,
                        List.of(vanilla("options.guiScale.auto"), "1", "2", "3", "4"),
                        () -> Math.max(0, Math.min(4, o.guiScale().get())), i -> o.guiScale().set(i), 0);
                number(o.fov(), "V", "options.fov", "options.video.desc.fov", 0, 30, 110, 1, 70, v -> String.valueOf(v));
                SliderRow effects = new SliderRow(rowX, rowY, rowW, rowHeight(), "E", TINT_CHUNK, vanilla("options.fovEffectScale"), text("options.video.desc.effects"), small,
                        0, 1, 0.05, 1.0, small ? 80 : 130, () -> o.fovEffectScale().get(), v -> o.fovEffectScale().set(v), v -> Math.round(v * 100) + " %");
                addRow(effects, () -> o.fovEffectScale().set(1.0));
                rowY += rowHeight() + rowGap();
                SliderRow gamma = new SliderRow(rowX, rowY, rowW, rowHeight(), "L", TINT_CHUNK, vanilla("options.gamma"), text("options.video.desc.gamma"), small,
                        0, 1, 0.05, 0.5, small ? 80 : 130, () -> o.gamma().get(), v -> o.gamma().set(v),
                        v -> v <= 0.001 ? vanilla("options.gamma.min") : v >= 0.999 ? vanilla("options.gamma.max") : Math.abs(v - 0.5) < 0.001 ? vanilla("options.gamma.default") : Math.round(v * 100) + " %");
                addRow(gamma, () -> o.gamma().set(0.5));
                rowY += rowHeight() + rowGap();
                NavRow compact = new NavRow(rowX, rowY, rowW, rowHeight(), "H", 0x3FD1C7, Tr.s("options.video.compact"), text("options.video.desc.compact"), null,
                        () -> this.minecraft.setScreen(new FordixSettingsScreen(this)));
                addRow(compact, null);
                rowY += rowHeight() + rowGap();
            }
        }
        return rowY;
    }

    @Override
    protected void renderHeader(GuiGraphics g, int x, int y, int right) {
        if (small) {
            return;
        }
        String low = Tr.s("options.video.cost.low");
        String mid = Tr.s("options.video.cost.mid");
        String high = Tr.s("options.video.cost.high");
        int total = this.font.width(Tr.s("options.video.cost")) + 3 * 13 + this.font.width(low) + this.font.width(mid) + this.font.width(high) + 24;
        int cx = Math.max(x, right - total);
        g.drawString(this.font, Tr.s("options.video.cost"), cx, y, Glass.dim(), false);
        cx += this.font.width(Tr.s("options.video.cost")) + 6;
        int[] colors = {0xFF5ED68A, 0xFFE8A64A, 0xFFE5534B};
        String[] names = {low, mid, high};
        for (int level = 0; level < 3; level++) {
            int[] heights = {3, 5, 8};
            for (int i = 0; i < 3; i++) {
                g.fill(cx + i * 3, y + 8 - heights[i], cx + i * 3 + 2, y + 8, i <= level ? colors[level] : 0x24FFFFFF);
            }
            cx += 11;
            g.drawString(this.font, names[level], cx, y, Glass.dim(), false);
            cx += this.font.width(names[level]) + 8;
        }
    }

    @Override
    protected void renderFoot(GuiGraphics g, int x, int y, int right) {
        int fps = this.minecraft.getFps();
        String text = Tr.s("options.video.now", fps);
        int color = fps >= 60 ? 0xFF5ED68A : fps >= 30 ? 0xFFE8A64A : 0xFFE5534B;
        if (right - x > this.font.width(text)) {
            g.drawString(this.font, text, x, y, color, false);
        }
    }
}
