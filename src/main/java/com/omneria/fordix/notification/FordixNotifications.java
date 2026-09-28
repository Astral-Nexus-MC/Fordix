package com.omneria.fordix.notification;

import com.omneria.fordix.notification.network.ClientboundNotificationPayload;
import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Server-side entry point for other mods: send a notification to a player. Title
 * and message are Components, so they can be translated or colored. Durations are
 * clamped to a sane range so a typo can't pin a notification on screen forever.
 */
public final class FordixNotifications {
    public static final int DEFAULT_SECONDS = 5;
    public static final int MAX_SECONDS = 60;

    private FordixNotifications() {
    }

    public static void send(ServerPlayer player, NotificationStyle style, Component title, Component message) {
        send(player, style, title, message, DEFAULT_SECONDS);
    }

    public static void send(ServerPlayer player, NotificationStyle style, Component title, Component message, int seconds) {
        int clamped = Math.max(1, Math.min(seconds, MAX_SECONDS));
        ClientboundNotificationPayload payload = new ClientboundNotificationPayload(style, title, message, clamped * 20);
        PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
