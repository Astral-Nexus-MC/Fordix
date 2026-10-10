package com.omneria.fordix.client.chat;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.layout.HudSnap;
import com.omneria.fordix.client.gui.vanilla.ExtraHud;
import com.omneria.fordix.client.gui.vanilla.Reflect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import com.omneria.fordix.client.gui.widget.FieldBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The Voidframe chat: tabs, a history with the sender's head, a time and a color per kind of line, and a styled text
 * field. It replaces vanilla's chat overlay and chat screen drawing (vanilla keeps receiving and sending everything);
 * the whole block is one HUD element shifted and sized by {@link ExtraHud}, and every position here is in the block's
 * own coordinates, so a moved or resized chat only needs the mouse mapped back with {@link ExtraHud#chatFrame}.
 */
public final class ChatPanel {
    private static final int TIME_X = 4;
    private static final int HEAD_X = 32;
    private static final int TEXT_X = 44;
    private static final int LINE = 9;
    private static final int PAD = 3;
    private static final long VISIBLE_MS = 10_000L;
    private static final long FADE_MS = 1_500L;
    private static final int PANEL_FILL = 0xD00B1118;
    private static final int ROW_FILL = 0x0B1118;
    private static final int TAB_FILL = 0xE0141F2B;
    private static final int TIME_COLOR = 0x8AA0AD;
    private static final int SYSTEM_BAR = 0xFFB347;
    private static final int SERVER_BAR = 0x7DFFB0;
    private static final int PRIVATE_BAR = 0xC792EA;
    private static final int SEARCH_MIN_WIDTH = 56;
    private static final int BADGE = 0xFF6B6B;
    private static final int MENTION = 0x30FFB347;
    private static final Field SUGGESTIONS = Reflect.uniqueField(ChatScreen.class, CommandSuggestions.class);
    private static final Field INPUT = Reflect.uniqueField(ChatScreen.class, EditBox.class);

    private static ChatTab selected = ChatTab.ALL;
    private static int scroll;
    /** The search field on the tabs row and what it holds; null while the chat is closed. */
    private static EditBox search;
    private static String query = "";
    private static final List<Hit> HITS = new ArrayList<>();

    private ChatPanel() {
    }

    private record Hit(int x, int top, int bottom, FormattedCharSequence line) {
    }

    public static boolean enabled() {
        return FordixConfig.voidframeChat();
    }

    /** The chat's width in scaled GUI pixels: the player's chat width setting, kept readable. */
    public static int panelWidth(int screenWidth) {
        return ChatLayout.width(Minecraft.getInstance().gui.getChat().getWidth(), screenWidth);
    }

    /** Switches tab (the click on a tab does the same; used by the in-game test). */
    public static void selectTab(ChatTab tab) {
        selected = tab;
        scroll = 0;
        ChatLog.markRead(tab);
    }

    /** Whether a line of this kind is on screen right now (chat open on a tab that lists it): it counts as read. */
    static boolean isReading(ChatKind kind) {
        return Minecraft.getInstance().screen instanceof ChatScreen && (selected == ChatTab.ALL || selected.matches(kind));
    }

    // ---- the overlay: tabs and history

    /** Normal priority, after {@link ExtraHud} has shifted the pose: draws the chat and cancels vanilla's. */
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!enabled() || event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) {
            return;
        }
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
            return;
        }
        draw(event.getGuiGraphics(), minecraft, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(),
                minecraft.screen instanceof ChatScreen);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static int fade(long ageMs) {
        if (ageMs < VISIBLE_MS - FADE_MS) {
            return 255;
        }
        return (int) Math.max(0, 255 * (VISIBLE_MS - ageMs) / (double) FADE_MS);
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, int screenWidth, int screenHeight, boolean open) {
        Font font = minecraft.font;
        int width = panelWidth(screenWidth);
        HudSnap.Rect log = ChatLayout.log(screenWidth, screenHeight, width);
        HITS.clear();
        List<ChatLog.Entry> source = open ? ChatLog.filtered(selected, query) : ChatLog.entries();
        if (open) {
            ChatLog.markRead(selected);
            com.omneria.fordix.client.hud.HudStyle.panel(graphics, log.x(), log.y(), log.right() - log.x(), log.bottom() - log.y(),
                    FordixPalette.ACCENT, PANEL_FILL >>> 24);
            scroll = Mth.clamp(scroll, 0, Math.max(0, source.size() - 1));
        }
        int skip = open ? scroll : 0;
        int textWidth = width - TEXT_X - 8;
        long now = System.currentTimeMillis();
        int y = log.bottom() - 1;
        int shown = 0;
        for (int i = source.size() - 1; i >= 0; i--) {
            ChatLog.Entry entry = source.get(i);
            long age = now - entry.addedAt;
            if (!open && age >= VISIBLE_MS) {
                break;
            }
            if (skip > 0) {
                skip--;
                continue;
            }
            List<FormattedCharSequence> lines = entry.lines(font, textWidth);
            int top = y - (lines.size() * LINE + PAD);
            if (top < log.y()) {
                break;
            }
            int alpha = open ? 255 : fade(age);
            if (alpha > 8) {
                drawRow(graphics, font, entry, lines, log.x(), top, y, width, alpha, open);
            }
            y = top;
            shown++;
        }
        if (open) {
            drawScrollbar(graphics, log, source.size(), shown);
            drawTabs(graphics, font, screenWidth, screenHeight, width);
        }
    }

    private static void drawRow(GuiGraphics graphics, Font font, ChatLog.Entry entry, List<FormattedCharSequence> lines,
                                int x, int top, int bottom, int width, int alpha, boolean open) {
        if (!open) {
            graphics.fill(x, top, x + width, bottom, withAlpha(ROW_FILL, (int) (0x70 * alpha / 255.0)));
        }
        if (entry.mention) {
            graphics.fill(x + 30, top, x + width - (open ? 4 : 0), bottom, MENTION);
        }
        int bar = switch (entry.kind) {
            case SYSTEM -> SYSTEM_BAR;
            case SERVER -> SERVER_BAR;
            case PRIVATE -> PRIVATE_BAR;
            default -> -1;
        };
        if (bar != -1) {
            graphics.fill(x + 30, top + 1, x + 32, bottom - 1, withAlpha(bar, alpha));
        }
        graphics.drawString(font, entry.time, x + TIME_X, top + 2, withAlpha(TIME_COLOR, alpha), false);
        ResourceLocation skin = entry.skin();
        if (skin != null) {
            PlayerFaceRenderer.draw(graphics, skin, x + HEAD_X, top + 2, 8);
        }
        for (int j = 0; j < lines.size(); j++) {
            int lineTop = top + 2 + j * LINE;
            graphics.drawString(font, lines.get(j), x + TEXT_X, lineTop, withAlpha(0xFFFFFF, alpha), true);
            if (open) {
                HITS.add(new Hit(x + TEXT_X, lineTop, lineTop + LINE, lines.get(j)));
            }
        }
    }

    private static void drawScrollbar(GuiGraphics graphics, HudSnap.Rect log, int total, int shown) {
        if (total <= shown || total == 0) {
            return;
        }
        int track = log.h() - 4;
        int thumb = Math.max(8, track * shown / total);
        int bottomOffset = (int) ((track - thumb) * (scroll / (double) Math.max(1, total - shown)));
        int thumbBottom = log.bottom() - 2 - Math.min(track - thumb, bottomOffset);
        graphics.fill(log.right() - 4, log.y() + 2, log.right() - 2, log.bottom() - 2, 0x30FFFFFF);
        graphics.fill(log.right() - 4, thumbBottom - thumb, log.right() - 2, thumbBottom, withAlpha(FordixPalette.ACCENT, 255));
    }

    // ---- tabs

    private static int tabWidth(Font font, ChatTab tab) {
        return font.width(Tr.s("chat.tab." + tab.id())) + 12;
    }

    private static HudSnap.Rect tabRect(Font font, int screenWidth, int screenHeight, int width, ChatTab target) {
        HudSnap.Rect tabs = ChatLayout.tabs(screenWidth, screenHeight, width);
        int x = tabs.x();
        for (ChatTab tab : ChatTab.values()) {
            int w = tabWidth(font, tab);
            if (tab == target) {
                return new HudSnap.Rect(x, tabs.y(), w, tabs.h());
            }
            x += w + 2;
        }
        return tabs;
    }

    /** Where the search field sits: the rest of the tabs row, or null when the chat is too narrow for it. */
    private static HudSnap.Rect searchRect(Font font, int screenWidth, int screenHeight, int width) {
        HudSnap.Rect tabs = ChatLayout.tabs(screenWidth, screenHeight, width);
        HudSnap.Rect last = tabRect(font, screenWidth, screenHeight, width, ChatTab.values()[ChatTab.values().length - 1]);
        int x = last.right() + 6;
        int w = tabs.right() - x;
        return w >= SEARCH_MIN_WIDTH ? new HudSnap.Rect(x, tabs.y(), w, tabs.h()) : null;
    }

    private static boolean searching() {
        return search != null && search.isFocused();
    }

    private static void focusSearch(ChatScreen screen, boolean focus) {
        if (search == null) {
            return;
        }
        search.setFocused(focus);
        EditBox box = input(screen);
        if (box != null) {
            box.setFocused(!focus);
        }
    }

    private static void drawTabs(GuiGraphics graphics, Font font, int screenWidth, int screenHeight, int width) {
        for (ChatTab tab : ChatTab.values()) {
            HudSnap.Rect rect = tabRect(font, screenWidth, screenHeight, width, tab);
            boolean on = tab == selected;
            graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), on ? withAlpha(FordixPalette.ACCENT, 255) : TAB_FILL);
            graphics.drawString(font, Tr.s("chat.tab." + tab.id()), rect.x() + 6, rect.y() + 3,
                    on ? 0xFF05070A : withAlpha(TIME_COLOR, 255), false);
            int unread = ChatLog.unread(tab);
            if (!on && unread > 0) {
                String text = unread > 9 ? "9+" : Integer.toString(unread);
                int badge = font.width(text) + 4;
                graphics.fill(rect.right() - badge + 3, rect.y() - 4, rect.right() + 3, rect.y() + 6, withAlpha(BADGE, 255));
                graphics.drawString(font, text, rect.right() - badge + 5, rect.y() - 3, 0xFFFFFFFF, false);
            }
        }
    }

    // ---- the chat screen: text field and suggestions

    private static CommandSuggestions suggestions(ChatScreen screen) {
        return SUGGESTIONS != null && Reflect.get(SUGGESTIONS, screen) instanceof CommandSuggestions found ? found : null;
    }

    private static EditBox input(ChatScreen screen) {
        return INPUT != null && Reflect.get(INPUT, screen) instanceof EditBox found ? found : null;
    }

    /** Opened: back to the latest line, and the text field makes room for the prompt. */
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!enabled() || !(event.getScreen() instanceof ChatScreen screen)) {
            return;
        }
        scroll = 0;
        ChatLog.markRead(selected);
        query = "";
        search = new FieldBox(Minecraft.getInstance().font, 0, 0, SEARCH_MIN_WIDTH, ChatLayout.TABS_HEIGHT, Tr.c("chat.search"));
        search.setBordered(false);
        search.setMaxLength(60);
        search.setHint(Tr.c("chat.search").withStyle(style -> style.withColor(TIME_COLOR)));
        search.setResponder(text -> {
            query = text;
            scroll = 0;
        });
        EditBox box = input(screen);
        if (box != null) {
            box.setX(ChatLayout.MARGIN + 12);
            box.setWidth(panelWidth(screen.width) - 16);
        }
    }

    /**
     * Normal priority, once {@link ExtraHud} has shifted the screen: draws the text field and the suggestions itself
     * (vanilla's own drawing would also look up its now invisible history under the mouse) and cancels vanilla's.
     */
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (!enabled() || !(event.getScreen() instanceof ChatScreen screen)) {
            return;
        }
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        ExtraHud.ChatFrame frame = ExtraHud.chatFrame(screen.width, screen.height);
        double mouseX = frame.toBlockX(event.getMouseX());
        double mouseY = frame.toBlockY(event.getMouseY());
        HudSnap.Rect input = ChatLayout.input(screen.width, screen.height, panelWidth(screen.width));
        int accent = withAlpha(FordixPalette.ACCENT, 255);
        graphics.fill(input.x(), input.y(), input.right(), input.bottom(), 0xF00B1118);
        graphics.fill(input.x(), input.y(), input.right(), input.y() + 1, accent);
        graphics.fill(input.x(), input.bottom() - 1, input.right(), input.bottom(), accent);
        graphics.fill(input.x(), input.y(), input.x() + 1, input.bottom(), accent);
        graphics.fill(input.right() - 1, input.y(), input.right(), input.bottom(), accent);
        graphics.drawString(minecraft.font, "›", input.x() + 5, input.y() + 2, accent, false);
        EditBox box = input(screen);
        if (box != null) {
            box.render(graphics, (int) mouseX, (int) mouseY, event.getPartialTick());
        }
        HudSnap.Rect field = search == null ? null : searchRect(minecraft.font, screen.width, screen.height, panelWidth(screen.width));
        if (field != null) {
            graphics.fill(field.x(), field.y(), field.right(), field.bottom(), TAB_FILL);
            if (search.isFocused()) {
                graphics.fill(field.x(), field.bottom() - 1, field.right(), field.bottom(), accent);
            }
            search.setX(field.x() + 4);
            search.setY(field.y() + (field.h() - 8) / 2);
            search.setWidth(field.w() - 8);
            search.render(graphics, (int) mouseX, (int) mouseY, event.getPartialTick());
        }
        CommandSuggestions suggestions = suggestions(screen);
        if (suggestions != null) {
            suggestions.render(graphics, (int) mouseX, (int) mouseY);
        }
        for (Hit hit : HITS) {
            if (mouseY >= hit.top() && mouseY < hit.bottom() && mouseX >= hit.x()) {
                Style style = minecraft.font.getSplitter().componentStyleAtWidth(hit.line(), (int) (mouseX - hit.x()));
                if (style != null && style.getHoverEvent() != null) {
                    graphics.renderComponentHoverEffect(minecraft.font, style, (int) mouseX, (int) mouseY);
                }
                break;
            }
        }
    }

    // ---- the chat screen: mouse and keys

    /** Everything is handled here: vanilla would act on its own, now invisible, history. */
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!enabled() || !(event.getScreen() instanceof ChatScreen screen)) {
            return;
        }
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        ExtraHud.ChatFrame frame = ExtraHud.chatFrame(screen.width, screen.height);
        double x = frame.toBlockX(event.getMouseX());
        double y = frame.toBlockY(event.getMouseY());
        int width = panelWidth(screen.width);
        HudSnap.Rect field = search == null ? null : searchRect(font, screen.width, screen.height, width);
        if (field != null && contains(field, x, y) && event.getButton() == 0) {
            focusSearch(screen, true);
            search.mouseClicked(x, y, event.getButton());
            return;
        }
        if (searching()) {
            focusSearch(screen, false);
        }
        CommandSuggestions suggestions = suggestions(screen);
        if (suggestions != null && suggestions.mouseClicked(x, y, event.getButton())) {
            return;
        }
        if (event.getButton() == 0) {
            for (ChatTab tab : ChatTab.values()) {
                if (contains(tabRect(font, screen.width, screen.height, width, tab), x, y)) {
                    selected = tab;
                    scroll = 0;
                    ChatLog.markRead(tab);
                    return;
                }
            }
            if (contains(ChatLayout.log(screen.width, screen.height, width), x, y)) {
                for (Hit hit : HITS) {
                    if (y >= hit.top() && y < hit.bottom() && x >= hit.x()) {
                        Style style = font.getSplitter().componentStyleAtWidth(hit.line(), (int) (x - hit.x()));
                        if (style != null) {
                            screen.handleComponentClicked(style);
                        }
                        return;
                    }
                }
                return;
            }
        }
        if (contains(ChatLayout.input(screen.width, screen.height, width), x, y)) {
            EditBox box = input(screen);
            if (box != null) {
                box.mouseClicked(x, y, event.getButton());
            }
        }
    }

    private static boolean contains(HudSnap.Rect rect, double x, double y) {
        return x >= rect.x() && x < rect.right() && y >= rect.y() && y < rect.bottom();
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!enabled() || !(event.getScreen() instanceof ChatScreen screen)) {
            return;
        }
        event.setCanceled(true);
        double delta = Mth.clamp(event.getScrollDelta(), -1.0D, 1.0D);
        CommandSuggestions suggestions = suggestions(screen);
        if (suggestions != null && suggestions.mouseScrolled(delta)) {
            return;
        }
        scroll = Math.max(0, scroll + (int) Math.signum(delta) * (Screen.hasShiftDown() ? 5 : 2));
    }

    /** Page Up and Page Down scroll the history. */
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!enabled() || !(event.getScreen() instanceof ChatScreen screen)) {
            return;
        }
        if (search != null && !searching() && com.omneria.fordix.client.FordixClient.pressed(com.omneria.fordix.client.FordixClient.SEARCH_FIELD, event.getKeyCode(), event.getScanCode())) {
            focusSearch(screen, true);
            event.setCanceled(true);
            return;
        }
        if (searching() && event.getKeyCode() != GLFW.GLFW_KEY_ESCAPE) {
            if (event.getKeyCode() == GLFW.GLFW_KEY_ENTER || event.getKeyCode() == GLFW.GLFW_KEY_KP_ENTER) {
                focusSearch(screen, false);
            } else {
                search.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            }
            event.setCanceled(true);
            return;
        }
        if (event.getKeyCode() == GLFW.GLFW_KEY_PAGE_UP) {
            scroll += 6;
            event.setCanceled(true);
        } else if (event.getKeyCode() == GLFW.GLFW_KEY_PAGE_DOWN) {
            scroll = Math.max(0, scroll - 6);
            event.setCanceled(true);
        }
    }

    /** Letters typed while the search field has the focus go there instead of the chat line. */
    public static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (enabled() && event.getScreen() instanceof ChatScreen && searching()) {
            search.charTyped(event.getCodePoint(), event.getModifiers());
            event.setCanceled(true);
        }
    }

    /** Types into the search field (used by the in-game test). */
    public static void setSearch(String text) {
        if (search != null) {
            search.setValue(text);
        }
    }
}
