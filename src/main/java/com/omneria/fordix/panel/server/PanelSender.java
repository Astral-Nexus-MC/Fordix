package com.omneria.fordix.panel.server;

import com.omneria.fordix.panel.PanelSpec;
import com.omneria.fordix.panel.network.ClientboundOpenPanelPayload;
import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.List;

/** Opens a panel on players' screens and remembers the snapshot their button clicks are resolved against. */
public final class PanelSender {
    private PanelSender() {
    }

    public static void send(PanelSpec spec, Collection<ServerPlayer> players) {
        List<String> labels = spec.buttons().stream().map(PanelSpec.PanelButton::label).toList();
        ClientboundOpenPanelPayload payload = new ClientboundOpenPanelPayload(spec.id(), spec.title(), spec.description(), labels);
        for (ServerPlayer player : players) {
            PanelRegistry.markSent(player.getUUID(), spec);
            PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
    }
}
