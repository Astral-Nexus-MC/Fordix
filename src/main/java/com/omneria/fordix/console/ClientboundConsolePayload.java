package com.omneria.fordix.console;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** The data of a console screen, sent when it opens and again after each action (see {@link ConsoleData} for the layout). */
public record ClientboundConsolePayload(CompoundTag data) {
    public static void encode(ClientboundConsolePayload payload, FriendlyByteBuf buf) {
        buf.writeNbt(payload.data());
    }

    public static ClientboundConsolePayload decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new ClientboundConsolePayload(tag == null ? new CompoundTag() : tag);
    }
}
