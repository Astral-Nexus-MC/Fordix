package com.omneria.fordix.guide;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * Server to client: the guide tab lines ("modid | Label | icon item | action") the server
 * wants every player to see in their inventory. An empty list means the server has no
 * opinion and each client keeps its own config.
 */
public record ClientboundGuidesPayload(List<String> lines) {
    /** Sanity cap on what a client will accept. */
    public static final int MAX_LINES = 32;

    public static void encode(ClientboundGuidesPayload payload, FriendlyByteBuf buf) {
        buf.writeCollection(payload.lines(), FriendlyByteBuf::writeUtf);
    }

    public static ClientboundGuidesPayload decode(FriendlyByteBuf buf) {
        List<String> lines = buf.readList(FriendlyByteBuf::readUtf);
        return new ClientboundGuidesPayload(lines.size() > MAX_LINES ? lines.subList(0, MAX_LINES) : lines);
    }
}
