package com.omneria.fordix.panel.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Server to client: open a panel built from an admin's /fordix panel draft. */
public record ClientboundOpenPanelPayload(String panelId, String title, String description, List<String> buttonLabels) {

    public static void encode(ClientboundOpenPanelPayload payload, FriendlyByteBuf buf) {
        buf.writeUtf(payload.panelId());
        buf.writeUtf(payload.title());
        buf.writeUtf(payload.description());
        buf.writeVarInt(payload.buttonLabels().size());
        for (String label : payload.buttonLabels()) {
            buf.writeUtf(label);
        }
    }

    public static ClientboundOpenPanelPayload decode(FriendlyByteBuf buf) {
        String panelId = buf.readUtf();
        String title = buf.readUtf();
        String description = buf.readUtf();
        int count = buf.readVarInt();
        List<String> buttonLabels = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            buttonLabels.add(buf.readUtf());
        }
        return new ClientboundOpenPanelPayload(panelId, title, description, buttonLabels);
    }
}
