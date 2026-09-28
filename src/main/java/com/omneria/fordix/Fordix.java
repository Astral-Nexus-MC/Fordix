package com.omneria.fordix;

import com.mojang.logging.LogUtils;
import com.omneria.fordix.notification.server.NotifyCommand;
import com.omneria.fordix.panel.network.PanelNetwork;
import com.omneria.fordix.panel.server.PanelCommand;
import com.omneria.fordix.panel.server.PanelRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

// The value here must match an entry in the META-INF/mods.toml file.
@Mod(Fordix.MODID)
public class Fordix {
    public static final String MODID = "fordix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Fordix() {
        LOGGER.info("Fordix UI library loaded");
        PanelNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, FordixConfig.CLIENT_SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PanelCommand.register(event.getDispatcher());
        NotifyCommand.register(event.getDispatcher());
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PanelRegistry.forgetPlayer(event.getEntity().getUUID());
    }
}
