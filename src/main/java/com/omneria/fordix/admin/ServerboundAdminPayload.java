package com.omneria.fordix.admin;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/** Client to server: an admin screen request (list panels, save a panel, send a notification...). Ops only. */
public record ServerboundAdminPayload(String action, List<String> args) {
    public static final int MAX_ARGS = 64;
    public static final int MAX_LENGTH = 1024;

    public static void encode(ServerboundAdminPayload payload, FriendlyByteBuf buf) {
        buf.writeUtf(payload.action(), 64);
        buf.writeCollection(payload.args(), (b, text) -> b.writeUtf(text, MAX_LENGTH));
    }

    public static ServerboundAdminPayload decode(FriendlyByteBuf buf) {
        String action = buf.readUtf(64);
        List<String> args = buf.readList(b -> b.readUtf(MAX_LENGTH));
        return new ServerboundAdminPayload(action, args.size() > MAX_ARGS ? args.subList(0, MAX_ARGS) : args);
    }
}
