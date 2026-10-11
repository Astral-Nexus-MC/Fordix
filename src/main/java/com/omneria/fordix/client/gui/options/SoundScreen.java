package com.omneria.fordix.client.gui.options;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.FordixClient;
import com.omneria.fordix.client.gui.widget.FlagRow;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.SliderRow;
import com.omneria.fordix.client.gui.widget.ToggleRow;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.gui.screens.Screen;

/**
 * Options > Sounds in Voidframe 2: the master volume in a card with a test sound, the nine categories as sliders (two columns on a
 * wide screen), then two comfort switches. Every slider has a thin mark at 100 %.
 */
public final class SoundScreen extends OptionsPageScreen {
    private static final SoundSource[] CATEGORIES = {SoundSource.MUSIC, SoundSource.RECORDS, SoundSource.WEATHER, SoundSource.BLOCKS,
            SoundSource.HOSTILE, SoundSource.NEUTRAL, SoundSource.PLAYERS, SoundSource.AMBIENT, SoundSource.VOICE};
    private static final int TINT = 0x3FD1C7;

    public SoundScreen(Screen parent) {
        super(Component.translatable("options.sounds.title"), parent);
    }

    private SliderRow volume(SoundSource source, int x, int y, int width, int height, boolean compact, String description, int track) {
        OptionInstance<Double> option = this.minecraft.options.getSoundSourceOptionInstance(source);
        String title = Component.translatable("soundCategory." + source.getName()).getString();
        String icon = title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        SliderRow row = new SliderRow(x, y, width, height, icon, TINT, title, description, compact, 0, 1, 0.01, 1.0, track,
                () -> option.get(), v -> option.set(v), v -> Math.round(v * 100) + " %");
        return addRow(row, () -> option.set(1.0));
    }

    @Override
    protected int buildRows(int left, int width) {
        Options o = this.minecraft.options;
        int y = listTop + 2;
        boolean wide = !small && width > 440;
        // The master volume, with a button that plays a short sound at the volume just set.
        int masterHeight = small ? 24 : 34;
        int testWidth = 52;
        volume(SoundSource.MASTER, left, y, width - testWidth - 4, masterHeight, false, small ? "" : Tr.s("options.sound.desc.master"), small ? 70 : 180);
        FordixButton test = new FordixButton(left + width - testWidth, y + (masterHeight - 18) / 2, testWidth, 18, Tr.c("options.sound.test"),
                () -> this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F)));
        addRow(test, null);
        y += masterHeight + 8;
        int rowH = small ? 16 : 22;
        int columns = wide ? 2 : 1;
        int columnW = (width - (columns - 1) * 8) / columns;
        int perColumn = (CATEGORIES.length + columns - 1) / columns;
        for (int i = 0; i < CATEGORIES.length; i++) {
            int column = i / perColumn;
            int row = i % perColumn;
            volume(CATEGORIES[i], left + column * (columnW + 8), y + row * rowH, columnW, rowH - 2, small, "", wide ? 56 : small ? 90 : 160);
        }
        y += perColumn * rowH + 6;
        addRow(new Heading(left, y, width, Tr.s("options.sound.comfort")), null);
        y += 14;
        int rowHeight = small ? 22 : 28;
        FlagRow subtitles = new FlagRow(left, y, width, rowHeight, "S", TINT, Component.translatable("options.showSubtitles").getString(),
                small ? "" : Tr.s("options.sound.desc.subtitles"), small, () -> o.showSubtitles().get(), v -> o.showSubtitles().set(v));
        addRow(subtitles, () -> o.showSubtitles().set(false));
        y += rowHeight + 2;
        ToggleRow music = new ToggleRow(left, y, width, rowHeight, "M", TINT, Tr.s("options.sound.music_toast"),
                small ? "" : Tr.s("options.sound.desc.music_toast"), small, FordixConfig.MUSIC_TOAST);
        addRow(music, () -> {
            FordixConfig.MUSIC_TOAST.set(true);
            FordixConfig.MUSIC_TOAST.save();
            FordixClient.applyConfig();
        });
        y += rowHeight + 2;
        return y;
    }
}
