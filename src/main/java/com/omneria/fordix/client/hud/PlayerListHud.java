package com.omneria.fordix.client.hud;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The Tab player list as a Voidframe panel: a title with the player count, the server's own header, the players in
 * columns (head, name colored by team, ping) and the server's footer. It replaces vanilla's list while Tab is held and is
 * a Forge overlay of its own, so the HUD layout screen can move it. Header and footer come from the tab list packet,
 * which vanilla keeps in private fields.
 */
public final class PlayerListHud {
    public static final String ID = "player_list";

    private static final int ROW = 11;
    private static final int PAD = 6;
    private static final int COLUMN_WIDTH = 150;
    private static final int ROWS_PER_COLUMN = 20;
    private static final int MAX_COLUMNS = 4;
    private static final int FILL = 0xE00B1118;
    private static final int LABEL = 0xFF8AA0AD;
    private static final int GOOD = 0xFF7DFFB0;
    private static final int WARN = 0xFFFFB347;
    private static final int BAD = 0xFFFF6B6B;

    private static Component header = Component.empty();
    private static Component footer = Component.empty();
    /** For the in-game test: draw the list without Tab being held and when alone. */
    public static boolean testForce;

    private PlayerListHud() {
    }

    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.PLAYER_LIST.id(), ID, PlayerListHud::draw);
    }

    /** Called from the connection's packet listener (see {@link InfoHud}). */
    public static void onTabListPacket(ClientboundTabListPacket packet) {
        header = packet.getHeader();
        footer = packet.getFooter();
    }

    public static void reset() {
        header = Component.empty();
        footer = Component.empty();
    }

    public static boolean replacesVanilla() {
        try {
            return FordixConfig.voidframeHud() && FordixConfig.voidframePlayerList();
        } catch (IllegalStateException notLoadedYet) {
            return false;
        }
    }

    /** Vanilla's list gives way to this one. */
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_LIST.type() && replacesVanilla()) {
            event.setCanceled(true);
        }
    }

    private static boolean shown(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null
                || minecraft.options.hideGui) {
            return false;
        }
        if (testForce) {
            return true;
        }
        // Like vanilla: the list needs Tab held, and in a world of your own it only shows once somebody else is there.
        return minecraft.options.keyPlayerList.isDown()
                && (!minecraft.isLocalServer() || minecraft.getConnection().getOnlinePlayers().size() > 1);
    }

    private static List<PlayerInfo> players(Minecraft minecraft) {
        List<PlayerInfo> list = new ArrayList<>(minecraft.getConnection().getOnlinePlayers());
        list.sort(Comparator.comparing((PlayerInfo info) -> info.getTeam() == null ? "" : info.getTeam().getName())
                .thenComparing(info -> info.getProfile().getName().toLowerCase(Locale.ROOT)));
        return list;
    }

    private static int pingColor(int latency) {
        return latency <= 0 ? LABEL : latency <= 100 ? GOOD : latency <= 250 ? WARN : BAD;
    }

    private static void draw(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!replacesVanilla() || !shown(minecraft)) {
            return;
        }
        Font font = minecraft.font;
        List<PlayerInfo> players = players(minecraft);
        int columns = Math.max(1, Math.min(MAX_COLUMNS, Math.min((players.size() + ROWS_PER_COLUMN - 1) / ROWS_PER_COLUMN,
                Math.max(1, (width - 20) / COLUMN_WIDTH))));
        int perColumn = Math.max(1, Math.min(ROWS_PER_COLUMN, (players.size() + columns - 1) / columns));
        int shownCount = Math.min(players.size(), columns * perColumn);
        int panelWidth = columns * COLUMN_WIDTH + PAD * 2;
        List<FormattedCharSequence> headLines = header.getString().isBlank() ? List.of() : font.split(header, panelWidth - PAD * 2);
        List<FormattedCharSequence> footLines = footer.getString().isBlank() ? List.of() : font.split(footer, panelWidth - PAD * 2);
        int titleHeight = 16;
        int headHeight = headLines.isEmpty() ? 0 : headLines.size() * 10 + 4;
        int footHeight = footLines.isEmpty() ? 0 : footLines.size() * 10 + 4;
        int hidden = players.size() - shownCount;
        int panelHeight = titleHeight + headHeight + perColumn * ROW + (hidden > 0 ? 10 : 0) + footHeight + PAD;
        int x = width / 2 - panelWidth / 2;
        int y = 28;
        int accent = 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF);

        graphics.fill(x, y, x + panelWidth, y + panelHeight, FILL);
        graphics.fill(x, y, x + 2, y + panelHeight, accent);
        graphics.fill(x, y + titleHeight - 1, x + panelWidth, y + titleHeight, 0x40FFFFFF);
        graphics.drawString(font, Tr.s("playerlist.title"), x + PAD + 2, y + 4, accent, false);
        String count = Tr.s("playerlist.count", players.size());
        graphics.drawString(font, count, x + panelWidth - PAD - font.width(count), y + 4, LABEL, false);

        int cursor = y + titleHeight + 2;
        for (FormattedCharSequence line : headLines) {
            graphics.drawString(font, line, x + panelWidth / 2 - font.width(line) / 2, cursor, 0xFFFFFFFF, true);
            cursor += 10;
        }
        if (!headLines.isEmpty()) {
            cursor += 2;
        }
        for (int i = 0; i < shownCount; i++) {
            PlayerInfo info = players.get(i);
            int column = i / perColumn;
            int row = i % perColumn;
            int rx = x + PAD + column * COLUMN_WIDTH;
            int ry = cursor + row * ROW;
            if (info.getProfile().getId().equals(minecraft.player.getUUID())) {
                graphics.fill(rx, ry - 1, rx + COLUMN_WIDTH - 4, ry + ROW - 1, 0x30FFFFFF);
            }
            PlayerFaceRenderer.draw(graphics, info.getSkinLocation(), rx + 2, ry, 8);
            Component name = minecraft.gui.getTabList().getNameForDisplay(info);
            String ping = info.getLatency() > 0 ? info.getLatency() + " ms" : "--";
            int nameRoom = COLUMN_WIDTH - 4 - 14 - font.width(ping) - 6;
            graphics.drawString(font, font.plainSubstrByWidth(name.getString(), nameRoom), rx + 14, ry + 1, nameColor(name), true);
            graphics.drawString(font, ping, rx + COLUMN_WIDTH - 6 - font.width(ping), ry + 1, pingColor(info.getLatency()), false);
        }
        cursor += perColumn * ROW;
        if (hidden > 0) {
            graphics.drawString(font, Tr.s("playerlist.more", hidden), x + PAD + 2, cursor + 1, LABEL, false);
            cursor += 10;
        }
        cursor += 2;
        for (FormattedCharSequence line : footLines) {
            graphics.drawString(font, line, x + panelWidth / 2 - font.width(line) / 2, cursor, 0xFFFFFFFF, true);
            cursor += 10;
        }
    }

    /** The team color of a name when it has one, white otherwise. */
    private static int nameColor(Component name) {
        var color = name.getStyle().getColor();
        return 0xFF000000 | (color == null ? 0xFFFFFF : color.getValue());
    }
}
