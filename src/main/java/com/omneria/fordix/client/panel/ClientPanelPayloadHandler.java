package com.omneria.fordix.client.panel;

import com.omneria.fordix.client.gui.GeneratedPanelScreen;
import com.omneria.fordix.panel.network.ClientboundOpenPanelPayload;
import net.minecraft.client.Minecraft;

/**
 * Only ever invoked from inside PanelNetwork's DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)
 * guard, never called directly - that's what keeps this class from loading on a
 * dedicated server.
 */
public final class ClientPanelPayloadHandler {

    private ClientPanelPayloadHandler() {
    }

    public static void handle(ClientboundOpenPanelPayload payload) {
        Minecraft.getInstance().setScreen(new GeneratedPanelScreen(
                payload.panelId(), payload.title(), payload.description(), payload.buttonLabels()
        ));
    }
}
