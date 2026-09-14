package com.omneria.fordix.client.panel;

import com.omneria.fordix.client.gui.GeneratedPanelScreen;
import com.omneria.fordix.panel.network.ClientboundOpenPanelPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPanelPayloadHandler {

    private ClientPanelPayloadHandler() {
    }

    public static void handle(ClientboundOpenPanelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new GeneratedPanelScreen(
                payload.panelId(), payload.title(), payload.description(), payload.buttonLabels()
        )));
    }
}
