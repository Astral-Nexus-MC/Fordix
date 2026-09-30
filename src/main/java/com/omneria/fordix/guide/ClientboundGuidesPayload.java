package com.omneria.fordix.guide;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * Server to client: the settings the server wants every player to share: the guide tab lines ("modid | Label | icon item | action") the server
 * wants every player to see in their inventory. An empty list means the server has no
 * opinion and each client keeps its own config; the rarity rules likewise, with the built-in defaults as fallback.
 */
public record ClientboundGuidesPayload(List<String> lines, List<String> rarityRules) {
    /** Sanity cap on what a client will accept. */
    public static final int MAX_LINES = 32;
    public static final int MAX_RULES = 256;

    public static void encode(ClientboundGuidesPayload payload, FriendlyByteBuf buf) {
        buf.writeCollection(payload.lines(), FriendlyByteBuf::writeUtf);
        buf.writeCollection(payload.rarityRules(), FriendlyByteBuf::writeUtf);
    }

    public static ClientboundGuidesPayload decode(FriendlyByteBuf buf) {
        List<String> lines = buf.readList(FriendlyByteBuf::readUtf);
        List<String> rules = buf.readList(FriendlyByteBuf::readUtf);
        return new ClientboundGuidesPayload(lines.size() > MAX_LINES ? lines.subList(0, MAX_LINES) : lines,
                rules.size() > MAX_RULES ? rules.subList(0, MAX_RULES) : rules);
    }
}
