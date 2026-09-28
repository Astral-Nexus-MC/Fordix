package com.omneria.fordix.notification.server;

import com.omneria.fordix.FordixServerConfig;
import com.omneria.fordix.notification.FordixNotifications;
import com.omneria.fordix.notification.NotificationTemplate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notifications the server sends on its own, driven by {@link FordixServerConfig}:
 * a welcome a few seconds after joining (a different one on a player's very first
 * join) and announcements rotated to everyone on an interval. Runs on the server
 * thread, from Forge events, so no locking is needed.
 */
public final class AutoNotifications {
    private static final String JOINED_TAG = "fordix_joined";

    /** Players waiting for their welcome: uuid -> server tick to send at, plus whether it's their first join. */
    private static final Map<UUID, PendingWelcome> PENDING = new HashMap<>();
    private static int ticksSinceAnnouncement;
    private static int nextAnnouncement;

    private record PendingWelcome(long sendAtTick, boolean firstJoin) {
    }

    private AutoNotifications() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !FordixServerConfig.WELCOME_ENABLED.get()) {
            return;
        }
        boolean firstJoin = markJoined(player);
        long sendAt = player.server.getTickCount() + FordixServerConfig.WELCOME_DELAY_SECONDS.get() * 20L;
        PENDING.put(player.getUUID(), new PendingWelcome(sendAt, firstJoin));
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        sendDueWelcomes(server);
        tickAnnouncements(server);
    }

    private static void sendDueWelcomes(MinecraftServer server) {
        long now = server.getTickCount();
        for (Iterator<Map.Entry<UUID, PendingWelcome>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, PendingWelcome> entry = it.next();
            if (now < entry.getValue().sendAtTick()) {
                continue;
            }
            it.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            String line = FordixServerConfig.WELCOME.get();
            String firstJoinLine = FordixServerConfig.FIRST_JOIN.get();
            if (entry.getValue().firstJoin() && !firstJoinLine.isEmpty()) {
                line = firstJoinLine;
            }
            send(player, line, FordixServerConfig.WELCOME_DURATION_SECONDS.get());
        }
    }

    private static void tickAnnouncements(MinecraftServer server) {
        int intervalMinutes = FordixServerConfig.ANNOUNCEMENT_INTERVAL_MINUTES.get();
        List<? extends String> messages = FordixServerConfig.ANNOUNCEMENTS.get();
        if (intervalMinutes <= 0 || messages.isEmpty()) {
            ticksSinceAnnouncement = 0;
            return;
        }
        if (++ticksSinceAnnouncement < intervalMinutes * 60 * 20) {
            return;
        }
        ticksSinceAnnouncement = 0;
        String line = messages.get(nextAnnouncement % messages.size());
        nextAnnouncement = (nextAnnouncement + 1) % messages.size();
        int seconds = FordixServerConfig.ANNOUNCEMENT_DURATION_SECONDS.get();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, line, seconds);
        }
    }

    private static void send(ServerPlayer player, String line, int seconds) {
        NotificationTemplate template = NotificationTemplate.parse(line);
        if (template == null) {
            return;
        }
        NotificationTemplate filled = template.filled(placeholders(player));
        FordixNotifications.send(player, filled.style(), Component.literal(filled.title()),
                Component.literal(filled.message()), seconds);
    }

    private static Map<String, String> placeholders(ServerPlayer player) {
        MinecraftServer server = player.server;
        return Map.of(
                "player", player.getGameProfile().getName(),
                "server", FordixServerConfig.SERVER_NAME.get(),
                "version", FordixServerConfig.PACK_VERSION.get(),
                "online", Integer.toString(server.getPlayerCount()),
                "max", Integer.toString(server.getMaxPlayers())
        );
    }

    /**
     * Records the join in the player's persisted data (kept across deaths) and
     * returns whether this was their first one.
     */
    private static boolean markJoined(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        boolean firstJoin = !persisted.getBoolean(JOINED_TAG);
        if (firstJoin) {
            persisted.putBoolean(JOINED_TAG, true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        return firstJoin;
    }
}
