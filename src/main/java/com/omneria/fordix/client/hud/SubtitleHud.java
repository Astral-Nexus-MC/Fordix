package com.omneria.fordix.client.hud;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The sound subtitles in Voidframe 2: a glass line per sound, an arrow toward the side it came from, fading after three seconds. The
 * game keeps its own list private and only listens while the option is on, so this listens to the sound manager itself and takes the
 * game's overlay out of the way. The position stays the one of the game's overlay (the HUD layout screen moves it).
 */
public final class SubtitleHud implements SoundEventListener {
    private static final long LIFE_MS = 3000L;
    private static final long FADE_MS = 500L;
    private static final int MAX_LINES = 7;
    private static final SubtitleHud INSTANCE = new SubtitleHud();

    private record Entry(Component text, Vec3 position, long time) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private boolean listening;

    private SubtitleHud() {
    }

    @Override
    public void onPlaySound(SoundInstance sound, WeighedSoundEvents accessor) {
        Component subtitle = accessor.getSubtitle();
        if (subtitle == null || !Minecraft.getInstance().options.showSubtitles().get()) {
            return;
        }
        Vec3 position = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        long now = System.currentTimeMillis();
        synchronized (entries) {
            entries.removeIf(entry -> entry.text().getString().equals(subtitle.getString()));
            entries.add(new Entry(subtitle, position, now));
            if (entries.size() > 20) {
                entries.remove(0);
            }
        }
    }

    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.SUBTITLES.type() || event.isCanceled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!INSTANCE.listening && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().addListener(INSTANCE);
            INSTANCE.listening = true;
        }
        event.setCanceled(true);
        if (!minecraft.options.showSubtitles().get() || minecraft.player == null) {
            return;
        }
        INSTANCE.draw(event.getGuiGraphics(), minecraft, minecraft.player, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
    }

    private void draw(GuiGraphics g, Minecraft minecraft, Player player, int width, int height) {
        long now = System.currentTimeMillis();
        List<Entry> shown;
        synchronized (entries) {
            for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) {
                if (now - it.next().time() > LIFE_MS) {
                    it.remove();
                }
            }
            int from = Math.max(0, entries.size() - MAX_LINES);
            shown = new ArrayList<>(entries.subList(from, entries.size()));
        }
        if (shown.isEmpty()) {
            return;
        }
        Font font = minecraft.font;
        Vec3 view = player.getViewVector(1.0F);
        Vec3 right = view.cross(new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 eye = player.getEyePosition();
        int y = height - 36;
        for (int i = shown.size() - 1; i >= 0; i--) {
            Entry entry = shown.get(i);
            long age = now - entry.time();
            float alpha = age > LIFE_MS - FADE_MS ? Math.max(0.0F, (LIFE_MS - age) / (float) FADE_MS) : 1.0F;
            Vec3 toSound = entry.position().subtract(eye);
            double side = toSound.lengthSqr() < 1.0E-4 ? 0.0 : toSound.normalize().dot(right);
            String arrowLeft = side < -0.35 ? "<" : " ";
            String arrowRight = side > 0.35 ? ">" : " ";
            String text = entry.text().getString();
            int textWidth = font.width(text);
            int boxWidth = textWidth + 34;
            int x = width - 6 - boxWidth;
            int a = Math.max(8, Math.round(255 * alpha));
            g.pose().pushPose();
            g.fill(x, y - 12, x + boxWidth, y + 2, HudTokens.withAlpha(0x070B10, Math.round(200 * alpha)));
            g.fill(x + boxWidth - 2, y - 12, x + boxWidth, y + 2, HudTokens.withAlpha(FordixPalette.ACCENT, a));
            g.fill(x, y - 12, x + boxWidth, y - 11, HudTokens.withAlpha(0xFFFFFF, Math.round(30 * alpha)));
            g.drawString(font, arrowLeft, x + 5, y - 9, HudTokens.withAlpha(FordixPalette.ACCENT, a), false);
            g.drawString(font, text, x + 17, y - 9, HudTokens.withAlpha(0xDFF3F1, a), false);
            g.drawString(font, arrowRight, x + boxWidth - 12, y - 9, HudTokens.withAlpha(FordixPalette.ACCENT, a), false);
            g.pose().popPose();
            y -= 15;
        }
    }
}
