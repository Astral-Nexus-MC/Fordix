package com.omneria.fordix.client.gui.options;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.ChatVisiblity;

import java.util.List;
import java.util.Locale;

/** Options > Chat in Voidframe 2: what the chat shows, how it looks, and its command suggestions. */
public final class ChatSettingsScreen extends OptionsPageScreen {
    public ChatSettingsScreen(Screen parent) {
        super(Component.translatable("options.chat.title"), parent);
    }

    @Override
    protected int rowHeight() {
        return 22;
    }

    @Override
    protected int buildRows(int left, int width) {
        Options o = this.minecraft.options;
        startRows(left, width);
        addPick(vanillaText("options.chat.visibility"), List.of(vanillaText("options.chat.visibility.full"),
                vanillaText("options.chat.visibility.system"), vanillaText("options.chat.visibility.hidden")),
                () -> o.chatVisibility().get().ordinal(), i -> o.chatVisibility().set(ChatVisiblity.values()[i]), 0);
        addFlag(o.chatColors(), "options.chat.color", true);
        addFlag(o.chatLinks(), "options.chat.links", true);
        addFlag(o.chatLinksPrompt(), "options.chat.links.prompt", true);
        addFlag(o.autoSuggestions(), "options.autoSuggestCommands", true);
        addFlag(o.hideMatchedNames(), "options.hideMatchedNames", true);
        addFlag(o.onlyShowSecureChat(), "options.onlyShowSecureChat", false);
        addPercent(o.chatOpacity(), "options.chat.opacity", 1.0);
        addPercent(o.textBackgroundOpacity(), "options.accessibility.text_background_opacity", 0.5);
        addPercent(o.chatScale(), "options.chat.scale", 1.0);
        addPercent(o.chatLineSpacing(), "options.chat.line_spacing", 0.0);
        addValue(o.chatDelay(), "~options.page.chat_delay", 0.0, 6.0, 0.1, 0.0, v -> String.format(Locale.ROOT, "%.1f s", v));
        addPercent(o.chatWidth(), "options.chat.width", 1.0);
        addPercent(o.chatHeightFocused(), "options.chat.height.focused", 1.0);
        addPercent(o.chatHeightUnfocused(), "options.chat.height.unfocused", 0.4375);
        return rowY;
    }
}
