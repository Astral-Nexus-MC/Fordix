package com.omneria.fordix.panel.network;

import com.omneria.fordix.Fordix;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Server to client: open a panel built from an admin's /fordix panel draft. */
public record ClientboundOpenPanelPayload(String panelId, String title, String description, List<String> buttonLabels)
        implements CustomPacketPayload {

    public static final Type<ClientboundOpenPanelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fordix.MODID, "open_panel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenPanelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClientboundOpenPanelPayload::panelId,
            ByteBufCodecs.STRING_UTF8, ClientboundOpenPanelPayload::title,
            ByteBufCodecs.STRING_UTF8, ClientboundOpenPanelPayload::description,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), ClientboundOpenPanelPayload::buttonLabels,
            ClientboundOpenPanelPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
