package com.omneria.fordix;

import com.mojang.logging.LogUtils;
import com.omneria.fordix.client.panel.ClientPanelPayloadHandler;
import com.omneria.fordix.panel.network.ClientboundOpenPanelPayload;
import com.omneria.fordix.panel.network.ServerboundPanelActionPayload;
import com.omneria.fordix.panel.server.PanelCommand;
import com.omneria.fordix.panel.server.ServerPanelPayloadHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

// The value here must match an entry in the META-INF/neoforge.mods.toml file.
@Mod(Fordix.MODID)
public class Fordix {
    public static final String MODID = "fordix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Fordix(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Fordix UI library loaded");
        modEventBus.addListener(this::onRegisterPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                ClientboundOpenPanelPayload.TYPE,
                ClientboundOpenPanelPayload.STREAM_CODEC,
                ClientPanelPayloadHandler::handle
        );
        registrar.playToServer(
                ServerboundPanelActionPayload.TYPE,
                ServerboundPanelActionPayload.STREAM_CODEC,
                ServerPanelPayloadHandler::handle
        );
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PanelCommand.register(event.getDispatcher());
    }
}
