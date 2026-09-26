package com.omneria.fordix.panel.network;

import com.omneria.fordix.Fordix;
import com.omneria.fordix.client.panel.ClientPanelPayloadHandler;
import com.omneria.fordix.panel.server.ServerPanelPayloadHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * SimpleChannel setup - Forge 1.20.1 predates the CustomPacketPayload/StreamCodec
 * API used from 1.20.5 onward, so panel packets are hand-encoded FriendlyByteBuf
 * messages instead.
 */
public final class PanelNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Fordix.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private PanelNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ClientboundOpenPanelPayload.class,
                ClientboundOpenPanelPayload::encode,
                ClientboundOpenPanelPayload::decode,
                PanelNetwork::handleOpenPanel);
        CHANNEL.registerMessage(id++, ServerboundPanelActionPayload.class,
                ServerboundPanelActionPayload::encode,
                ServerboundPanelActionPayload::decode,
                ServerPanelPayloadHandler::handle);
    }

    private static void handleOpenPanel(ClientboundOpenPanelPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPanelPayloadHandler.handle(payload))
        );
        ctx.get().setPacketHandled(true);
    }
}
