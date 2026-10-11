package com.omneria.fordix.client.gui.options;

import com.omneria.fordix.Tr;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Options > Accessibility in Voidframe 2: narrator, subtitles, text background, toggles for crouch and sprint, and the screen effects. */
public final class AccessibilityScreen extends OptionsPageScreen {
    public AccessibilityScreen(Screen parent) {
        super(Component.translatable("options.accessibility.title"), parent);
    }

    @Override
    protected int rowHeight() {
        return 22;
    }

    @Override
    protected int buildRows(int left, int width) {
        Options o = this.minecraft.options;
        startRows(left, width);
        addPick(vanillaText("options.narrator"), List.of(vanillaText("options.narrator.off"), vanillaText("options.narrator.all"),
                vanillaText("options.narrator.chat"), vanillaText("options.narrator.system")),
                () -> o.narrator().get().ordinal(), i -> o.narrator().set(NarratorStatus.values()[i]), 0);
        addFlag(o.showSubtitles(), "options.showSubtitles", false);
        addPick(vanillaText("options.accessibility.text_background"), List.of(vanillaText("options.accessibility.text_background.chat"),
                vanillaText("options.accessibility.text_background.everywhere")),
                () -> o.backgroundForChatOnly().get() ? 0 : 1, i -> o.backgroundForChatOnly().set(i == 0), 0);
        addPercent(o.textBackgroundOpacity(), "options.accessibility.text_background_opacity", 0.5);
        addPercent(o.chatOpacity(), "options.chat.opacity", 1.0);
        addPercent(o.chatLineSpacing(), "options.chat.line_spacing", 0.0);
        addHeading(Tr.s("options.page.heading.play"));
        addPick(vanillaText("key.sneak"), List.of(vanillaText("options.key.hold"), vanillaText("options.key.toggle")),
                () -> o.toggleCrouch().get() ? 1 : 0, i -> o.toggleCrouch().set(i == 1), 0);
        addPick(vanillaText("key.sprint"), List.of(vanillaText("options.key.hold"), vanillaText("options.key.toggle")),
                () -> o.toggleSprint().get() ? 1 : 0, i -> o.toggleSprint().set(i == 1), 0);
        addFlag(o.autoJump(), "options.autoJump", true);
        addPercent(o.screenEffectScale(), "options.screenEffectScale", 1.0);
        addPercent(o.fovEffectScale(), "options.fovEffectScale", 1.0);
        addFlag(o.darkMojangStudiosBackground(), "options.darkMojangStudiosBackgroundColor", false);
        addFlag(o.hideLightningFlash(), "options.hideLightningFlashes", false);
        addPercent(o.panoramaSpeed(), "options.accessibility.panorama_speed", 1.0);
        return rowY;
    }
}
