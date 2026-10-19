package com.omneria.fordix.console;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** A click on a console screen: the kind of console and what was asked ({@code pos}, {@code row}, {@code bonus}). */
public record ServerboundConsolePayload(String kind, CompoundTag action) {
    public static void encode(ServerboundConsolePayload payload, FriendlyByteBuf buf) {
        buf.writeUtf(payload.kind(), 128);
        buf.writeNbt(payload.action());
    }

    public static ServerboundConsolePayload decode(FriendlyByteBuf buf) {
        String kind = buf.readUtf(128);
        CompoundTag tag = buf.readNbt();
        return new ServerboundConsolePayload(kind, tag == null ? new CompoundTag() : tag);
    }
}
