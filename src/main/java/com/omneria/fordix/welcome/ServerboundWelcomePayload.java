package com.omneria.fordix.welcome;

import net.minecraft.network.FriendlyByteBuf;

/** Client to server: "guides_opened" (the Guides screen was opened), "toggle" (step {@code index}) or "refresh". */
public record ServerboundWelcomePayload(String action, int index) {
    public static void encode(ServerboundWelcomePayload payload, FriendlyByteBuf buf) {
        buf.writeUtf(payload.action(), 32);
        buf.writeVarInt(payload.index());
    }

    public static ServerboundWelcomePayload decode(FriendlyByteBuf buf) {
        return new ServerboundWelcomePayload(buf.readUtf(32), buf.readVarInt());
    }
}
