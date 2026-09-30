package com.omneria.fordix.notification.server;

import com.omneria.fordix.Fordix;
import com.omneria.fordix.notification.AdminStore;
import com.omneria.fordix.notification.NotificationStyle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Holds the {@link AdminStore} of the running world (fordix-admin.json in the world folder),
 * sends the notifications that come due and saves after every change. Server thread only.
 */
public final class NotificationScheduler {
    private static final int CHECK_EVERY_TICKS = 20;
    private static AdminStore store = new AdminStore();
    private static Path file;
    private static int ticks;

    private NotificationScheduler() {
    }

    public static AdminStore store() {
        return store;
    }

    public static void onServerStarted(ServerStartedEvent event) {
        file = event.getServer().getWorldPath(LevelResource.ROOT).resolve("fordix-admin.json");
        store = new AdminStore();
        if (Files.isRegularFile(file)) {
            try {
                store = AdminStore.fromJson(Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException e) {
                Fordix.LOGGER.warn("Could not read {}", file, e);
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        store = new AdminStore();
        file = null;
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks < CHECK_EVERY_TICKS) {
            return;
        }
        ticks = 0;
        if (store.schedules.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        var due = store.due(System.currentTimeMillis());
        if (due.isEmpty()) {
            return;
        }
        for (AdminStore.Scheduled entry : due) {
            NotificationStyle style = NotificationStyle.byId(entry.style);
            if (style == null) {
                continue;
            }
            NotificationTargets.Resolved target = NotificationTargets.byKind(server, entry.kind, entry.value);
            NotificationSender.send("programme #" + entry.id, style, entry.seconds, entry.title, entry.message,
                    target.label(), target.players());
        }
        save();
    }

    public static void save() {
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file, store.toJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Fordix.LOGGER.warn("Could not save {}", file, e);
        }
    }
}
