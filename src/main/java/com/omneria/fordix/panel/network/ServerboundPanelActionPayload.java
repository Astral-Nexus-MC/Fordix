package com.omneria.fordix.panel.network;

import net.minecraft.network.FriendlyByteBuf;

/** Client to server: a player clicked button buttonIndex on the panel panelId. */
public record ServerboundPanelActionPayload(String panelId, int buttonIndex) {

    public static void encode(ServerboundPanelActionPayload payload, FriendlyByteBuf buf) {
        buf.writeUtf(payload.panelId());
        buf.writeVarInt(payload.buttonIndex());
    }

    public static ServerboundPanelActionPayload decode(FriendlyByteBuf buf) {
        return new ServerboundPanelActionPayload(buf.readUtf(), buf.readVarInt());
    }
}
