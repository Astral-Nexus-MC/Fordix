package com.omneria.fordix.client.hud;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.Collection;
import java.util.List;

/**
 * The sidebar scoreboard in Voidframe 2: a glass panel with the objective's title in capitals above a rule, names on the
 * left and values aligned on the right. It replaces the game's overlay (same objective, same team color rules, same 15
 * lines) at the same place, so the layout editor still moves and sizes it like any other overlay.
 */
public final class ScoreboardHud {
    private static final int LINE = 10;
    private static final int PAD = 7;

    private ScoreboardHud() {
    }

    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.SCOREBOARD.type() || !FordixConfig.voidframeHud()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Scoreboard scoreboard = minecraft.level.getScoreboard();
        Objective objective = null;
        PlayerTeam team = scoreboard.getPlayersTeam(minecraft.player.getScoreboardName());
        if (team != null && team.getColor().getId() >= 0) {
            objective = scoreboard.getDisplayObjective(3 + team.getColor().getId());
        }
        if (objective == null) {
            objective = scoreboard.getDisplayObjective(1);
        }
        if (objective == null) {
            return;
        }
        event.setCanceled(true);
        draw(event.getGuiGraphics(), minecraft, scoreboard, objective, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
    }

    private static void draw(GuiGraphics g, Minecraft minecraft, Scoreboard scoreboard, Objective objective, int screenWidth, int screenHeight) {
        Font font = minecraft.font;
        Collection<Score> all = scoreboard.getPlayerScores(objective);
        List<Score> scores = Lists.newArrayList(Iterables.filter(all, score -> score.getOwner() != null && !score.getOwner().startsWith("#")));
        if (scores.size() > 15) {
            scores = Lists.newArrayList(Iterables.skip(scores, scores.size() - 15));
        }
        Component title = objective.getDisplayName();
        String heading = title.getString().toUpperCase(java.util.Locale.ROOT);
        int width = font.width(heading);
        int valueWidth = 0;
        for (Score score : scores) {
            PlayerTeam team = scoreboard.getPlayersTeam(score.getOwner());
            Component name = PlayerTeam.formatNameForTeam(team, Component.literal(score.getOwner()));
            valueWidth = Math.max(valueWidth, font.width(String.valueOf(score.getScore())));
            width = Math.max(width, font.width(name) + 12 + font.width(String.valueOf(score.getScore())));
        }
        int panelWidth = width + PAD * 2;
        int panelHeight = PAD + 12 + scores.size() * LINE + PAD - 2;
        int x = screenWidth - panelWidth - 4;
        int y = screenHeight / 2 - panelHeight / 2;
        HudStyle.panel(g, x, y, panelWidth, panelHeight, FordixPalette.ACCENT & 0xFFFFFF, 200);
        g.drawString(font, heading, x + (panelWidth - font.width(heading)) / 2, y + PAD - 1,
                HudTokens.mix(FordixPalette.ACCENT, 0xFFFFFFFF, 0.35F), false);
        Glass.rule(g, x + PAD, y + PAD + 9, panelWidth - PAD * 2);
        int line = y + PAD + 13;
        for (Score score : Lists.reverse(scores)) {
            PlayerTeam team = scoreboard.getPlayersTeam(score.getOwner());
            Component name = PlayerTeam.formatNameForTeam(team, Component.literal(score.getOwner()));
            g.drawString(font, name, x + PAD, line, 0xFFFFFFFF, false);
            String value = String.valueOf(score.getScore());
            g.drawString(font, value, x + panelWidth - PAD - font.width(value), line, 0xFFFFD36A, false);
            line += LINE;
        }
    }
}
