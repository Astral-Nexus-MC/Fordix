package com.omneria.fordix.panel.network;

import com.omneria.fordix.Fordix;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: a player clicked button buttonIndex on the panel panelId. */
public record ServerboundPanelActionPayload(String panelId, int buttonIndex) implements CustomPacketPayload {

    public static final Type<ServerboundPanelActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fordix.MODID, "panel_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundPanelActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ServerboundPanelActionPayload::panelId,
            ByteBufCodecs.VAR_INT, ServerboundPanelActionPayload::buttonIndex,
            ServerboundPanelActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
