package com.omneria.fordix.hud;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * Server to client: the HUD rules of this server. {@code required} elements can never be hidden, {@code forbidden} ones are
 * always hidden, and {@code proposed} is a share code of a layout the server suggests (empty: none).
 */
public record ClientboundHudRulesPayload(List<String> required, List<String> forbidden, String proposed,
                                         List<String> situationsTable, List<String> situationsProfiles) {
    public static final int MAX_IDS = 64;
    public static final int MAX_CODE = 30_000;

    public static void encode(ClientboundHudRulesPayload payload, FriendlyByteBuf buf) {
        buf.writeCollection(payload.required(), (b, id) -> b.writeUtf(id, 96));
        buf.writeCollection(payload.forbidden(), (b, id) -> b.writeUtf(id, 96));
        buf.writeUtf(payload.proposed(), MAX_CODE);
        buf.writeCollection(payload.situationsTable(), (b, line) -> b.writeUtf(line, 256));
        buf.writeCollection(payload.situationsProfiles(), (b, line) -> b.writeUtf(line, MAX_CODE + 64));
    }

    public static ClientboundHudRulesPayload decode(FriendlyByteBuf buf) {
        List<String> required = buf.readList(b -> b.readUtf(96));
        List<String> forbidden = buf.readList(b -> b.readUtf(96));
        String proposed = buf.readUtf(MAX_CODE);
        List<String> table = buf.readList(b -> b.readUtf(256));
        List<String> profiles = buf.readList(b -> b.readUtf(MAX_CODE + 64));
        return new ClientboundHudRulesPayload(cap(required), cap(forbidden), proposed, cap(table), cap(profiles));
    }

    private static List<String> cap(List<String> ids) {
        return ids.size() > MAX_IDS ? ids.subList(0, MAX_IDS) : ids;
    }
}
