package com.omneria.fordix.client;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.hud.HudToast;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraftforge.client.event.sound.PlaySoundEvent;

import java.util.Locale;

/** A short notice when a new piece of music starts (option {@code musicToast}, in Options > Sounds). */
public final class MusicToast {
    private MusicToast() {
    }

    /** "music.game" or "music_disc.cat" read as words: "Game", "Cat". */
    static String readable(ResourceLocation id) {
        String path = id.getPath();
        int dot = path.lastIndexOf('.');
        String last = dot >= 0 ? path.substring(dot + 1) : path;
        String words = last.replace('_', ' ').trim();
        return words.isEmpty() ? path : words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
    }

    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || sound.getSource() != SoundSource.MUSIC || !FordixConfig.MUSIC_TOAST.get()) {
            return;
        }
        HudToast.show(Tr.s("hud.toast.music"), readable(sound.getLocation()), 0xB45FE0);
    }
}
