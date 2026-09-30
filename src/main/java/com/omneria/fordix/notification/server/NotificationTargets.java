package com.omneria.fordix.notification.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Who a notification goes to, beyond a player selector: everyone, one player by name, a
 * scoreboard team ("groupe"), an entity tag, or everyone with at least a permission level.
 * Each lookup returns the players and a short label for the history.
 */
public final class NotificationTargets {
    public record Resolved(String label, List<ServerPlayer> players) {
    }

    private NotificationTargets() {
    }

    public static Resolved all(MinecraftServer server) {
        return new Resolved("tous", List.copyOf(server.getPlayerList().getPlayers()));
    }

    public static Resolved player(MinecraftServer server, String name) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        return new Resolved("joueur " + name, player == null ? List.of() : List.of(player));
    }

    public static Resolved team(MinecraftServer server, String team) {
        return new Resolved("equipe " + team, server.getPlayerList().getPlayers().stream()
                .filter(player -> player.getTeam() != null && player.getTeam().getName().equals(team)).toList());
    }

    public static Resolved tag(MinecraftServer server, String tag) {
        return new Resolved("tag " + tag, server.getPlayerList().getPlayers().stream()
                .filter(player -> player.getTags().contains(tag)).toList());
    }

    /** Players whose permission level is at least {@code level} (2 is the level of ops who can use /give). */
    public static Resolved permission(MinecraftServer server, int level) {
        return new Resolved("permission >= " + level, server.getPlayerList().getPlayers().stream()
                .filter(player -> player.hasPermissions(level)).toList());
    }

    /** The target named by the admin screen's kinds: tous, joueur, equipe, tag, permission. */
    public static Resolved byKind(MinecraftServer server, String kind, String value) {
        return switch (kind) {
            case "joueur" -> player(server, value);
            case "equipe" -> team(server, value);
            case "tag" -> tag(server, value);
            case "permission" -> {
                int level;
                try {
                    level = Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    level = 2;
                }
                yield permission(server, Math.max(0, Math.min(4, level)));
            }
            default -> all(server);
        };
    }
}
