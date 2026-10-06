package com.omneria.fordix.pet;

import net.minecraft.network.FriendlyByteBuf;

/** Client to server: an action of the interaction wheel on a tamed creature (by its entity id). */
public record ServerboundPetActionPayload(int entityId, int action) {
    public static void encode(ServerboundPetActionPayload payload, FriendlyByteBuf buf) {
        buf.writeVarInt(payload.entityId());
        buf.writeByte(payload.action());
    }

    public static ServerboundPetActionPayload decode(FriendlyByteBuf buf) {
        return new ServerboundPetActionPayload(buf.readVarInt(), buf.readUnsignedByte());
    }
}
