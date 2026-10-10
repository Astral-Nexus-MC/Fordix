package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.hud.HudTokens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The pause menu in Voidframe 2. The game's own screen is kept (so Realms, the LAN dialog, the disconnect logic and the
 * buttons other mods add all keep working); only its buttons are laid out again in a glass column on the left, the
 * world stays visible, and a session card on the right shows where the player is. The "Game Menu" title the screen draws
 * itself is blanked, the column carries a larger one.
 */
public final class PauseSkin {
    private static final int COLUMN_X = 40;
    private static final int COLUMN_WIDTH = 240;
    private static final int PAD = 12;
    private static final int ROOMY_TOP = 98;
    private static final int TIGHT_TOP = 54;
    private static final int CARD_WIDTH = 234;
    private static final int CARD_HEIGHT = 110;

    private static int panelHeight;
    private static int panelTop = ROOMY_TOP;
    private static PauseScreen laidOut;

    private PauseSkin() {
    }

    private static String keyOf(AbstractWidget widget) {
        return widget.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable
                ? translatable.getKey() : "";
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        laidOut = null;
        if (!FordixConfig.VOIDFRAME_VANILLA_SCREENS.get() || event.getScreen().getClass() != PauseScreen.class) {
            return;
        }
        AbstractWidget back = null;
        AbstractWidget guides = null;
        AbstractWidget stats = null;
        AbstractWidget options = null;
        AbstractWidget lan = null;
        AbstractWidget reporting = null;
        AbstractWidget quit = null;
        List<AbstractWidget> extra = new ArrayList<>();
        for (GuiEventListener listener : event.getListenersList()) {
            if (!(listener instanceof Button button) || !button.visible) {
                continue;
            }
            switch (keyOf(button)) {
                case "menu.returnToGame" -> back = button;
                case "fordix.guides.button" -> guides = button;
                case "gui.advancements" -> button.visible = false;
                case "gui.stats" -> stats = button;
                case "menu.options" -> options = button;
                case "menu.shareToLan" -> lan = button;
                case "menu.playerReporting" -> reporting = button;
                case "menu.returnToMenu", "menu.disconnect" -> quit = button;
                case "menu.sendFeedback", "menu.reportBugs" -> button.visible = false;
                default -> extra.add(button);
            }
        }
        if (back == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // The rows of the mockup, in its order: the main action, the four rows with a value line, the pair, the way out.
        List<List<AbstractWidget>> rows = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        rows.add(List.of(back));
        heights.add(30);
        if (guides != null) {
            rows.add(List.of(guides));
            heights.add(34);
            RowButtons.register((Button) guides, "G", () -> Tr.s("pause.guides.detail"),
                    () -> com.omneria.fordix.client.guide.ServerPages.hasUnread() ? Tr.s("guides.new_badge") : "");
        }
        if (stats != null) {
            rows.add(List.of(stats));
            heights.add(34);
            RowButtons.register((Button) stats, "S", () -> "");
        }
        if (options != null) {
            rows.add(List.of(options));
            heights.add(34);
            RowButtons.register((Button) options, "O", () -> Tr.s("pause.options.detail"));
        }
        for (AbstractWidget other : extra) {
            rows.add(List.of(other));
            heights.add(26);
        }
        if (lan != null || reporting != null) {
            List<AbstractWidget> pair = new ArrayList<>();
            if (lan != null) {
                pair.add(lan);
            }
            if (reporting != null) {
                pair.add(reporting);
            }
            rows.add(pair);
            heights.add(24);
        }
        if (quit != null) {
            rows.add(List.of(quit));
            heights.add(28);
        }
        int inner = COLUMN_WIDTH - PAD * 2;
        int screenHeight = event.getScreen().height;
        // Roomy on a tall screen, tight (smaller rows, closer heading) on a small one, so the whole menu always fits.
        int top = screenHeight >= 400 ? ROOMY_TOP : TIGHT_TOP;
        int available = screenHeight - top - 8 - PAD * 2;
        int shrink = 0;
        while (needed(heights, shrink) > available && shrink < 14) {
            shrink++;
        }
        panelTop = top;
        int y = top + PAD;
        for (int i = 0; i < rows.size(); i++) {
            List<AbstractWidget> row = rows.get(i);
            int height = Math.max(18, heights.get(i) - shrink);
            int each = (inner - (row.size() - 1) * 4) / row.size();
            for (int k = 0; k < row.size(); k++) {
                AbstractWidget widget = row.get(k);
                widget.setX(COLUMN_X + PAD + k * (each + 4));
                widget.setY(y);
                widget.setWidth(each);
                widget.setHeight(height);
            }
            y += height + gapAfter(i, rows.size(), heights, shrink);
        }
        panelHeight = y - top + PAD - 4;
        // The game's "Game Menu" caption is a text widget of its own: the column carries a larger heading.
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof net.minecraft.client.gui.components.StringWidget caption) {
                caption.visible = false;
            }
        }
        laidOut = (PauseScreen) event.getScreen();
    }

    /** Space under a row: wide after the main action and before the way out, narrow between rows. */
    private static int gapAfter(int index, int count, List<Integer> heights, int shrink) {
        if (index == 0) {
            return Math.max(4, 10 - shrink);
        }
        if (index == count - 2) {
            return Math.max(4, 22 - shrink * 2);
        }
        return heights.get(index) >= 30 ? Math.max(2, 4 - shrink / 4) : Math.max(2, 8 - shrink / 2);
    }

    private static int needed(List<Integer> heights, int shrink) {
        int total = 0;
        for (int i = 0; i < heights.size(); i++) {
            total += Math.max(18, heights.get(i) - shrink) + gapAfter(i, heights.size(), heights, shrink);
        }
        return total;
    }

    /** Drawn right after the dimmed world and before the buttons: the shade, the column, the heading and the session card. */
    public static void onBackgroundRendered(ScreenEvent.BackgroundRendered event) {
        if (event.getScreen() != laidOut || !FordixConfig.VOIDFRAME_VANILLA_SCREENS.get()) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int width = event.getScreen().width;
        int height = event.getScreen().height;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        // A shade from the left edge so the column reads on any world.
        int shadeWidth = COLUMN_X + COLUMN_WIDTH + 90;
        for (int i = 0; i < shadeWidth; i += 6) {
            g.fill(i, 0, Math.min(shadeWidth, i + 6), height, HudTokens.withAlpha(0x03060A, Math.round(150 * (1.0F - i / (float) shadeWidth))));
        }
        Glass.panel(g, COLUMN_X, panelTop, COLUMN_WIDTH, panelHeight);
        boolean tight = panelTop < ROOMY_TOP;
        int headingY = tight ? 8 : 34;
        Glass.label(g, font, Tr.s("pause.heading"), COLUMN_X + 2, headingY);
        float scale = tight ? 1.4F : 3.0F;
        g.pose().pushPose();
        g.pose().translate(COLUMN_X + 2, headingY + (tight ? 11 : 13), 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, font.plainSubstrByWidth(worldName(minecraft), (int) ((COLUMN_WIDTH - 4) / scale)), 0, 0, FordixPalette.TEXT_BRIGHT, true);
        g.pose().popPose();
        Glass.rule(g, COLUMN_X + 2, panelTop - 6, COLUMN_WIDTH - 4);
        int cardX = width - CARD_WIDTH - 40;
        if (cardX >= COLUMN_X + COLUMN_WIDTH + 16) {
            sessionCard(g, font, minecraft, cardX, 34);
        }
    }

    private static String worldName(Minecraft minecraft) {
        if (minecraft.getCurrentServer() != null) {
            return minecraft.getCurrentServer().name;
        }
        if (minecraft.getSingleplayerServer() != null) {
            return minecraft.getSingleplayerServer().getWorldData().getLevelName();
        }
        return Tr.s("pause.heading");
    }

    private static void sessionCard(GuiGraphics g, Font font, Minecraft minecraft, int x, int y) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        Glass.panel(g, x, y, CARD_WIDTH, CARD_HEIGHT);
        Glass.label(g, font, Tr.s("pause.session"), x + 14, y + 10);
        String mode = minecraft.gameMode == null ? "" : Tr.s("pause.mode." + minecraft.gameMode.getPlayerMode().getName());
        g.drawString(font, player.getName().getString(), x + 14, y + 24, FordixPalette.TEXT_BRIGHT, false);
        g.drawString(font, " \u00B7 " + mode, x + 14 + font.width(player.getName().getString()), y + 24, Glass.dim(), false);
        String position = String.format("X %d  Y %d  Z %d", player.getBlockX(), player.getBlockY(), player.getBlockZ());
        g.drawString(font, position, x + 14, y + 42, FordixPalette.TEXT, false);
        long day = minecraft.level.getDayTime() / 24000L + 1L;
        g.drawString(font, Tr.s("pause.day", day), x + 14, y + 58, Glass.dim(), false);
        int cursor = x + 14;
        PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(player.getUUID());
        if (info != null && info.getLatency() > 0) {
            int latency = info.getLatency();
            int color = latency < 80 ? 0xFF5ED68A : latency < 160 ? 0xFFE8A64A : 0xFFE5534B;
            cursor += Glass.pill(g, font, "\u25CF " + latency + " ms", cursor, y + 86, color) + 4;
        }
        Glass.pill(g, font, Tr.s(minecraft.getSingleplayerServer() != null ? "pause.solo" : "pause.online"), cursor, y + 86, FordixPalette.ACCENT);
    }
}
