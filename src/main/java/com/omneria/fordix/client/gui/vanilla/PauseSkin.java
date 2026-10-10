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
    private static final int COLUMN_X = 32;
    private static final int COLUMN_WIDTH = 214;
    private static final int PAD = 12;
    private static final int TOP = 84;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_GAP = 6;
    private static final int CARD_WIDTH = 190;
    private static final int CARD_HEIGHT = 96;

    private static final Field TITLE = Reflect.uniqueField(net.minecraft.client.gui.screens.Screen.class, Component.class);

    private static int panelHeight;
    private static PauseScreen laidOut;

    private PauseSkin() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!FordixConfig.VOIDFRAME_VANILLA_SCREENS.get() || event.getScreen().getClass() != PauseScreen.class) {
            return;
        }
        List<AbstractWidget> widgets = new ArrayList<>();
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button button && button.visible) {
                widgets.add(button);
            }
        }
        if (widgets.isEmpty()) {
            return;
        }
        // Rows as the game laid them out: same y means the same row (Options and Open to LAN sit side by side).
        widgets.sort(Comparator.<AbstractWidget>comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getX));
        List<List<AbstractWidget>> rows = new ArrayList<>();
        for (AbstractWidget widget : widgets) {
            List<AbstractWidget> last = rows.isEmpty() ? null : rows.get(rows.size() - 1);
            if (last != null && Math.abs(last.get(0).getY() - widget.getY()) < 4) {
                last.add(widget);
            } else {
                List<AbstractWidget> row = new ArrayList<>();
                row.add(widget);
                rows.add(row);
            }
        }
        int inner = COLUMN_WIDTH - PAD * 2;
        int y = TOP + PAD;
        for (int i = 0; i < rows.size(); i++) {
            List<AbstractWidget> row = rows.get(i);
            boolean first = i == 0;
            boolean last = i == rows.size() - 1;
            if (last && rows.size() > 2) {
                y += 8;
            }
            int height = first ? 28 : ROW_HEIGHT;
            int each = (inner - (row.size() - 1) * 4) / row.size();
            for (int k = 0; k < row.size(); k++) {
                AbstractWidget widget = row.get(k);
                widget.setX(COLUMN_X + PAD + k * (each + 4));
                widget.setY(y);
                widget.setWidth(each);
                widget.setHeight(height);
            }
            y += height + (first ? ROW_GAP + 4 : ROW_GAP);
        }
        panelHeight = y - TOP + PAD - ROW_GAP;
        laidOut = (PauseScreen) event.getScreen();
        if (TITLE != null) {
            try {
                TITLE.set(event.getScreen(), Component.empty());
            } catch (IllegalAccessException | RuntimeException ignored) {
                // The vanilla title stays: harmless.
            }
        }
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
        Glass.panel(g, COLUMN_X, TOP, COLUMN_WIDTH, panelHeight);
        Glass.label(g, font, Tr.s("pause.heading"), COLUMN_X + 2, 30);
        g.pose().pushPose();
        g.pose().translate(COLUMN_X + 2, 42, 0);
        g.pose().scale(1.8F, 1.8F, 1.0F);
        g.drawString(font, worldName(minecraft), 0, 0, FordixPalette.TEXT_BRIGHT, true);
        g.pose().popPose();
        Glass.rule(g, COLUMN_X + 2, 66, COLUMN_WIDTH - 4);
        sessionCard(g, font, minecraft, width - CARD_WIDTH - 24, TOP);
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
        Glass.label(g, font, Tr.s("pause.session"), x + 12, y + 9);
        g.drawString(font, player.getName().getString(), x + 12, y + 22, FordixPalette.TEXT_BRIGHT, false);
        String position = String.format("X %d  Y %d  Z %d", player.getBlockX(), player.getBlockY(), player.getBlockZ());
        g.drawString(font, position, x + 12, y + 38, FordixPalette.TEXT, false);
        long day = minecraft.level.getDayTime() / 24000L + 1L;
        g.drawString(font, Tr.s("pause.day", day), x + 12, y + 52, FordixPalette.TEXT, false);
        int cursor = x + 12;
        PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(player.getUUID());
        if (info != null && info.getLatency() > 0) {
            int latency = info.getLatency();
            int color = latency < 80 ? 0xFF5ED68A : latency < 160 ? 0xFFE8A64A : 0xFFE5534B;
            cursor += Glass.pill(g, font, latency + " ms", cursor, y + 72, color) + 4;
        }
        Glass.pill(g, font, Tr.s(minecraft.getSingleplayerServer() != null ? "pause.solo" : "pause.online"), cursor, y + 72, FordixPalette.ACCENT);
    }
}
