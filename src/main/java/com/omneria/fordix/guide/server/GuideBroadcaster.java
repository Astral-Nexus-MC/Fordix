package com.omneria.fordix.guide.server;

import com.omneria.fordix.FordixServerConfig;
import com.omneria.fordix.guide.ClientboundGuidePagesPayload;
import com.omneria.fordix.guide.ClientboundGuidesPayload;
import com.omneria.fordix.guide.GuidePage;
import com.omneria.fordix.notification.server.NotificationScheduler;
import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

/**
 * Sends the server's guide tab list ({@link FordixServerConfig#GUIDES}) to players when
 * they join and to everyone when the config file changes, so the tabs can be edited in
 * one place instead of on every player's machine.
 */
public final class GuideBroadcaster {
    private GuideBroadcaster() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            send(player);
        }
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != FordixServerConfig.SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> server.getPlayerList().getPlayers().forEach(GuideBroadcaster::send));
        }
    }

    /** Sends every guide page to one player, or to everyone when an admin changed them. */
    public static void sendPages(ServerPlayer player) {
        List<GuidePage> pages = List.copyOf(NotificationScheduler.store().pages.values());
        PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundGuidePagesPayload(pages));
    }

    public static void broadcastPages(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(GuideBroadcaster::sendPages);
    }

    private static void send(ServerPlayer player) {
        sendPages(player);
        PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new com.omneria.fordix.hud.ClientboundHudRulesPayload(
                List.copyOf(FordixServerConfig.HUD_REQUIRED.get()), List.copyOf(FordixServerConfig.HUD_FORBIDDEN.get()),
                FordixServerConfig.HUD_PROPOSED.get().length() <= com.omneria.fordix.hud.ClientboundHudRulesPayload.MAX_CODE
                        ? FordixServerConfig.HUD_PROPOSED.get() : ""));
        com.omneria.fordix.wiki.WikiService.sendList(player);
        List<String> lines = List.copyOf(FordixServerConfig.GUIDES.get());
        List<String> rules = List.copyOf(FordixServerConfig.RARITY_RULES.get());
        PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundGuidesPayload(lines, rules));
    }
}
