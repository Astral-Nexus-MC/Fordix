package com.omneria.fordix.client.chat;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.layout.HudSnap;
import com.omneria.fordix.client.gui.vanilla.Reflect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The command line of the chat (what the field does once the text starts with "/") in Voidframe 2: the game's suggestions as a
 * glass list with the chosen line lit, the usage or the error as a glass card (amber or red) above the field, and the sent
 * messages as a short list while the player walks through them with the arrows. The game still computes and handles everything; this
 * only reads what the game's {@link CommandSuggestions} holds and draws it. Every read is by field type, like the rest of the mod,
 * and any miss returns false so that the caller falls back on the game's own drawing.
 */
public final class CommandLineSkin {
    private static final int ERROR = 0xFFE0524A;
    private static final int USAGE = 0xFFE8A64A;

    private static final Field LIST = innerField();
    private static final Field USAGE_LINES = Reflect.uniqueField(CommandSuggestions.class, List.class);
    private static final Field PARSE = Reflect.uniqueField(CommandSuggestions.class, ParseResults.class);
    private static final Field RECT = LIST == null ? null : Reflect.uniqueField(LIST.getType(), Rect2i.class);
    private static final Field ITEMS = LIST == null ? null : Reflect.uniqueField(LIST.getType(), List.class);
    private static final Field OFFSET = LIST == null ? null : nthInt(LIST.getType(), 1);
    private static final Field CURRENT = LIST == null ? null : nthInt(LIST.getType(), 2);
    private static final Field HISTORY = Reflect.uniqueField(ChatScreen.class, int.class);

    private CommandLineSkin() {
    }

    /** The field of {@link CommandSuggestions} that holds its list: the only one whose type is a class nested in it. */
    private static Field innerField() {
        for (Field field : CommandSuggestions.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType().getEnclosingClass() == CommandSuggestions.class) {
                try {
                    field.setAccessible(true);
                    return field;
                } catch (RuntimeException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /** The list keeps its scroll offset, then the chosen index, as its first two ints: {@code nth} 1 is the offset, 2 the chosen one. */
    private static Field nthInt(Class<?> owner, int nth) {
        int seen = 0;
        for (Field field : owner.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
                if (++seen == nth) {
                    try {
                        field.setAccessible(true);
                        return field;
                    } catch (RuntimeException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /** Whether the game finds the command in the field wrong (an exception from the parse while text is left to read). */
    public static boolean hasError(CommandSuggestions suggestions, EditBox input) {
        if (suggestions == null || input == null || !input.getValue().startsWith("/")) {
            return false;
        }
        Object parse = Reflect.get(PARSE, suggestions);
        return parse instanceof ParseResults<?> results && !results.getExceptions().isEmpty() && results.getReader().canRead();
    }

    /** Draws the suggestions, the usage or the error above the field; false when the game's own drawing should be used. */
    public static boolean render(GuiGraphics g, CommandSuggestions suggestions, EditBox input, HudSnap.Rect field, int mouseX, int mouseY) {
        if (suggestions == null || LIST == null || USAGE_LINES == null) {
            return false;
        }
        Font font = Minecraft.getInstance().font;
        Object list = Reflect.get(LIST, suggestions);
        if (list != null) {
            return drawList(g, font, list, field);
        }
        if (Reflect.get(USAGE_LINES, suggestions) instanceof List<?> lines && !lines.isEmpty()) {
            return drawCard(g, font, lines, field, hasError(suggestions, input));
        }
        return true;
    }

    private static boolean drawList(GuiGraphics g, Font font, Object list, HudSnap.Rect field) {
        if (RECT == null || ITEMS == null || CURRENT == null || OFFSET == null || !(Reflect.get(RECT, list) instanceof Rect2i rect)
                || !(Reflect.get(ITEMS, list) instanceof List<?> items)) {
            return false;
        }
        int current;
        int offset;
        try {
            current = CURRENT.getInt(list);
            offset = OFFSET.getInt(list);
        } catch (IllegalAccessException e) {
            return false;
        }
        int rowHeight = 12;
        // The same rows, at the same place, as the game's list: its mouse handling still reads its own rectangle.
        int shown = Math.max(1, Math.min(items.size(), rect.getHeight() / rowHeight));
        int from = Math.max(0, Math.min(items.size() - shown, offset));
        int width = Math.max(60, rect.getWidth() + 8);
        for (int i = from; i < from + shown; i++) {
            if (items.get(i) instanceof Suggestion suggestion) {
                String note = suggestion.getTooltip() == null ? "" : suggestion.getTooltip().getString();
                width = Math.max(width, font.width(suggestion.getText()) + (note.isEmpty() ? 0 : font.width(note) + 14) + 14);
            }
        }
        int height = shown * rowHeight + 14;
        int x = Math.max(field.x(), Math.min(rect.getX() - 4, field.right() - width));
        int y = field.y() - 3 - height;
        Glass.panel(g, x, y, width, height);
        Glass.label(g, font, Tr.s("chat.cmd.suggestions", items.size()), x + 7, y + 4);
        for (int i = from; i < from + shown; i++) {
            if (!(items.get(i) instanceof Suggestion suggestion)) {
                continue;
            }
            int rowY = y + 14 + (i - from) * rowHeight;
            boolean selected = i == current;
            Glass.row(g, x + 2, rowY, width - 4, rowHeight, selected, false);
            g.drawString(font, suggestion.getText(), x + 7, rowY + 2, selected ? 0xFFFFFFFF : FordixPalette.TEXT, false);
            if (suggestion.getTooltip() != null) {
                String note = suggestion.getTooltip().getString();
                g.drawString(font, font.plainSubstrByWidth(note, width - font.width(suggestion.getText()) - 24), x + 7 + font.width(suggestion.getText()) + 8,
                        rowY + 2, Glass.dim(), false);
            }
        }
        return true;
    }

    private static boolean drawCard(GuiGraphics g, Font font, List<?> lines, HudSnap.Rect field, boolean error) {
        List<FormattedCharSequence> text = new ArrayList<>();
        for (Object line : lines) {
            if (line instanceof FormattedCharSequence sequence) {
                text.add(sequence);
            }
        }
        if (text.isEmpty()) {
            return true;
        }
        int widest = 0;
        for (FormattedCharSequence sequence : text) {
            widest = Math.max(widest, font.width(sequence));
        }
        int width = Math.min(field.w(), widest + 16);
        int height = text.size() * 10 + 8;
        int x = field.x();
        int y = field.y() - 3 - height;
        Glass.panel(g, x, y, width, height, error ? ERROR : USAGE);
        for (int i = 0; i < text.size(); i++) {
            g.drawString(font, text.get(i), x + 8, y + 5 + i * 10, 0xFFFFFFFF, false);
        }
        return true;
    }

    /** The sent messages around the one in the field, while the player walks through them; draws nothing when not browsing. */
    public static void renderHistory(GuiGraphics g, EditBox input, ChatScreen screen, HudSnap.Rect field) {
        if (HISTORY == null || input == null || !(Reflect.get(HISTORY, screen) instanceof Integer position)) {
            return;
        }
        List<String> recent = Minecraft.getInstance().gui.getChat().getRecentChat();
        HistoryView.Window window = HistoryView.window(position, recent.size(), 4);
        if (window.empty() || !recent.get(window.selected()).equals(input.getValue())) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int width = field.w();
        int height = (window.to() - window.from()) * 12 + 14;
        int y = field.y() - 3 - height;
        Glass.panel(g, field.x(), y, width, height);
        Glass.label(g, font, Tr.s("chat.cmd.history"), field.x() + 7, y + 4);
        String counter = HistoryView.counter(window.selected(), recent.size());
        g.drawString(font, counter, field.right() - 7 - font.width(counter), y + 4, Glass.dim(), false);
        for (int i = window.from(); i < window.to(); i++) {
            int rowY = y + 14 + (i - window.from()) * 12;
            boolean selected = i == window.selected();
            Glass.row(g, field.x() + 2, rowY, width - 4, 12, selected, false);
            g.drawString(font, font.plainSubstrByWidth(recent.get(i), width - 18), field.x() + 7, rowY + 2, selected ? 0xFFFFFFFF : Glass.dim(), false);
        }
    }
}
