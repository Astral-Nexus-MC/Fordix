package com.omneria.fordix.wiki;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Server to client: one part of an answer. {@code kind}, {@code wiki} and {@code key} repeat the request it answers;
 * the data is UTF-8 text (lists, pages, search results) or PNG bytes (pictures), cut in {@code parts} pieces.
 */
public record ClientboundWikiPayload(ServerboundWikiPayload.Kind kind, String wiki, String key, int part, int parts, byte[] data) {
    public static void encode(ClientboundWikiPayload payload, FriendlyByteBuf buf) {
        buf.writeByte(payload.kind().ordinal());
        buf.writeUtf(payload.wiki(), 64);
        buf.writeUtf(payload.key(), 200);
        buf.writeVarInt(payload.part());
        buf.writeVarInt(payload.parts());
        buf.writeByteArray(payload.data());
    }

    public static ClientboundWikiPayload decode(FriendlyByteBuf buf) {
        int ordinal = Math.max(0, Math.min(ServerboundWikiPayload.Kind.values().length - 1, buf.readUnsignedByte()));
        return new ClientboundWikiPayload(ServerboundWikiPayload.Kind.values()[ordinal], buf.readUtf(64), buf.readUtf(200),
                buf.readVarInt(), buf.readVarInt(), buf.readByteArray(WikiChunks.SIZE + 1024));
    }
}
