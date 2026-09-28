package com.omneria.fordix.notification.network;

import com.omneria.fordix.notification.NotificationStyle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** Server to client: show a notification in the corner of the screen. */
public record ClientboundNotificationPayload(NotificationStyle style, Component title, Component message, int durationTicks) {

    public static void encode(ClientboundNotificationPayload payload, FriendlyByteBuf buf) {
        buf.writeEnum(payload.style());
        buf.writeComponent(payload.title());
        buf.writeComponent(payload.message());
        buf.writeVarInt(payload.durationTicks());
    }

    public static ClientboundNotificationPayload decode(FriendlyByteBuf buf) {
        NotificationStyle style = buf.readEnum(NotificationStyle.class);
        Component title = buf.readComponent();
        Component message = buf.readComponent();
        int durationTicks = buf.readVarInt();
        return new ClientboundNotificationPayload(style, title, message, durationTicks);
    }
}
