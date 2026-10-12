package com.omneria.fordix.debug;

import net.minecraft.network.FriendlyByteBuf;

/** The F3 screen asks what the server knows about where the player stands; {@code slimes} adds the slime chunks around. */
public record ServerboundDebugInfoPayload(boolean slimes) {
    public static void encode(ServerboundDebugInfoPayload payload, FriendlyByteBuf buf) {
        buf.writeBoolean(payload.slimes());
    }

    public static ServerboundDebugInfoPayload decode(FriendlyByteBuf buf) {
        return new ServerboundDebugInfoPayload(buf.readBoolean());
    }
}
