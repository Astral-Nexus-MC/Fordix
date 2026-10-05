package com.omneria.fordix.wiki;

import net.minecraft.network.FriendlyByteBuf;

/** Client to server: asks for something from the in-game wiki (the list of wikis, a page index, a page, a picture, a search). */
public record ServerboundWikiPayload(Kind kind, String wiki, String key) {
    public enum Kind { LIST, INDEX, PAGE, IMAGE, SEARCH }

    public static void encode(ServerboundWikiPayload payload, FriendlyByteBuf buf) {
        buf.writeByte(payload.kind().ordinal());
        buf.writeUtf(payload.wiki(), 64);
        buf.writeUtf(payload.key(), 200);
    }

    public static ServerboundWikiPayload decode(FriendlyByteBuf buf) {
        int ordinal = Math.max(0, Math.min(Kind.values().length - 1, buf.readUnsignedByte()));
        return new ServerboundWikiPayload(Kind.values()[ordinal], buf.readUtf(64), buf.readUtf(200));
    }
}
