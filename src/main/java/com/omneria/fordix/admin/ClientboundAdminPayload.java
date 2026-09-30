package com.omneria.fordix.admin;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * Server to client: an answer for the admin screen. {@code kind} is "panels" (ids), "panel" (id,
 * title, description, then one "label TAB command" per button), "history", "status" (one message)
 * or "denied" (the player isn't allowed).
 */
public record ClientboundAdminPayload(String kind, List<String> lines) {

    public static void encode(ClientboundAdminPayload payload, FriendlyByteBuf buf) {
        buf.writeUtf(payload.kind(), 64);
        buf.writeCollection(payload.lines(), (b, text) -> b.writeUtf(text, ServerboundAdminPayload.MAX_LENGTH));
    }

    public static ClientboundAdminPayload decode(FriendlyByteBuf buf) {
        String kind = buf.readUtf(64);
        List<String> lines = buf.readList(b -> b.readUtf(ServerboundAdminPayload.MAX_LENGTH));
        return new ClientboundAdminPayload(kind, lines.size() > 200 ? lines.subList(0, 200) : lines);
    }
}
