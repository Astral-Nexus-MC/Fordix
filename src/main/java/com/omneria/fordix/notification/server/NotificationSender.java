package com.omneria.fordix.notification.server;

import com.google.gson.JsonObject;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.notification.FordixNotifications;
import com.omneria.fordix.notification.NotificationHistory;
import com.omneria.fordix.notification.NotificationStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;

/**
 * Sends a notification to a group of players and records it: in the in-memory
 * {@link NotificationHistory} and as one JSON line in logs/fordix-notifications.jsonl.
 */
public final class NotificationSender {
    public static final NotificationHistory HISTORY = new NotificationHistory();
    private static final int MAX_LABEL_NAMES = 4;

    private NotificationSender() {
    }

    public static int send(String sender, NotificationStyle style, int seconds, String title, String message,
                           String recipients, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            FordixNotifications.send(player, style, Component.literal(title), Component.literal(message), seconds);
        }
        record(sender, style, title, message, recipients, players);
        return players.size();
    }

    /** Records a notification the server sent on its own (welcome, announcement). */
    public static void record(String sender, NotificationStyle style, String title, String message, String recipients,
                              Collection<ServerPlayer> players) {
        List<String> names = players.stream().map(player -> player.getGameProfile().getName()).toList();
        String label = names.size() <= MAX_LABEL_NAMES || !recipients.equals("tous")
                ? recipients : "tous";
        NotificationHistory.Entry entry = new NotificationHistory.Entry(System.currentTimeMillis(), sender, style.id(),
                title, message, label, players.size());
        HISTORY.add(entry);
        JsonObject json = new JsonObject();
        json.addProperty("time", entry.time());
        json.addProperty("sender", sender);
        json.addProperty("style", entry.style());
        json.addProperty("title", title);
        json.addProperty("message", message);
        json.addProperty("recipients", recipients);
        json.addProperty("players", String.join(",", names));
        try {
            Path file = FMLPaths.GAMEDIR.get().resolve("logs").resolve("fordix-notifications.jsonl");
            Files.createDirectories(file.getParent());
            Files.writeString(file, json + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            Fordix.LOGGER.warn("Could not log the notification", e);
        }
    }
}
