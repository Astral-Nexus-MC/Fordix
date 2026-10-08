package com.omneria.fordix.panel.network;

import com.omneria.fordix.Fordix;
import com.omneria.fordix.admin.AdminHandler;
import com.omneria.fordix.admin.ClientboundAdminPayload;
import com.omneria.fordix.admin.ServerboundAdminPayload;
import com.omneria.fordix.client.admin.ClientAdminData;
import com.omneria.fordix.client.guide.ServerGuides;
import com.omneria.fordix.client.wiki.ClientWiki;
import com.omneria.fordix.pet.PetService;
import com.omneria.fordix.pet.ServerboundPetActionPayload;
import com.omneria.fordix.wiki.ClientboundWikiPayload;
import com.omneria.fordix.wiki.ServerboundWikiPayload;
import com.omneria.fordix.wiki.WikiService;
import com.omneria.fordix.client.guide.ServerPages;
import com.omneria.fordix.client.tooltip.ServerRarities;
import com.omneria.fordix.client.notification.NotificationOverlay;
import com.omneria.fordix.client.panel.ClientPanelPayloadHandler;
import com.omneria.fordix.client.welcome.ClientWelcome;
import com.omneria.fordix.guide.ClientboundGuidePagesPayload;
import com.omneria.fordix.welcome.ClientboundWelcomePayload;
import com.omneria.fordix.welcome.ServerboundWelcomePayload;
import com.omneria.fordix.welcome.WelcomeService;
import com.omneria.fordix.guide.ClientboundGuidesPayload;
import com.omneria.fordix.notification.network.ClientboundNotificationPayload;
import com.omneria.fordix.panel.server.ServerPanelPayloadHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * Fordix's network channel (panels and notifications). SimpleChannel setup - Forge 1.20.1 predates the CustomPacketPayload/StreamCodec
 * API used from 1.20.5 onward, so panel packets are hand-encoded FriendlyByteBuf
 * messages instead.
 */
public final class PanelNetwork {
    private static final String PROTOCOL_VERSION = "10";

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
        CHANNEL.registerMessage(id++, ClientboundNotificationPayload.class,
                ClientboundNotificationPayload::encode,
                ClientboundNotificationPayload::decode,
                PanelNetwork::handleNotification);
        CHANNEL.registerMessage(id++, ClientboundGuidesPayload.class,
                ClientboundGuidesPayload::encode,
                ClientboundGuidesPayload::decode,
                PanelNetwork::handleGuides);
        CHANNEL.registerMessage(id++, ServerboundAdminPayload.class,
                ServerboundAdminPayload::encode,
                ServerboundAdminPayload::decode,
                AdminHandler::handle);
        CHANNEL.registerMessage(id++, ClientboundAdminPayload.class,
                ClientboundAdminPayload::encode,
                ClientboundAdminPayload::decode,
                PanelNetwork::handleAdmin);
        CHANNEL.registerMessage(id++, ClientboundWelcomePayload.class,
                ClientboundWelcomePayload::encode,
                ClientboundWelcomePayload::decode,
                PanelNetwork::handleWelcome);
        CHANNEL.registerMessage(id++, ServerboundWelcomePayload.class,
                ServerboundWelcomePayload::encode,
                ServerboundWelcomePayload::decode,
                WelcomeService::handle);
        CHANNEL.registerMessage(id++, ClientboundGuidePagesPayload.class,
                ClientboundGuidePagesPayload::encode,
                ClientboundGuidePagesPayload::decode,
                PanelNetwork::handleGuidePages);
        CHANNEL.registerMessage(id++, ServerboundWikiPayload.class,
                ServerboundWikiPayload::encode,
                ServerboundWikiPayload::decode,
                WikiService::handle);
        CHANNEL.registerMessage(id++, ClientboundWikiPayload.class,
                ClientboundWikiPayload::encode,
                ClientboundWikiPayload::decode,
                PanelNetwork::handleWiki);
        CHANNEL.registerMessage(id++, com.omneria.fordix.hud.ClientboundHudRulesPayload.class,
                com.omneria.fordix.hud.ClientboundHudRulesPayload::encode,
                com.omneria.fordix.hud.ClientboundHudRulesPayload::decode,
                PanelNetwork::handleHudRules);
        CHANNEL.registerMessage(id++, ServerboundPetActionPayload.class,
                ServerboundPetActionPayload::encode,
                ServerboundPetActionPayload::decode,
                PetService::handle);
    }

    private static void handleHudRules(com.omneria.fordix.hud.ClientboundHudRulesPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.omneria.fordix.client.hud.ClientHudRules.receive(payload))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleWiki(ClientboundWikiPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWiki.receive(payload))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleAdmin(ClientboundAdminPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAdminData.receive(payload.kind(), payload.lines()))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleWelcome(ClientboundWelcomePayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWelcome.receive(payload))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleGuidePages(ClientboundGuidePagesPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ServerPages.set(payload.pages()))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleGuides(ClientboundGuidesPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ServerGuides.set(payload.lines());
                    ServerRarities.set(payload.rarityRules());
                })
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleOpenPanel(ClientboundOpenPanelPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPanelPayloadHandler.handle(payload))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleNotification(ClientboundNotificationPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NotificationOverlay.receive(payload))
        );
        ctx.get().setPacketHandled(true);
    }
}
