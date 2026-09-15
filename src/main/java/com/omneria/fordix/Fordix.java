package com.omneria.fordix;

import com.mojang.logging.LogUtils;
import com.omneria.fordix.panel.network.PanelNetwork;
import com.omneria.fordix.panel.server.PanelCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

// The value here must match an entry in the META-INF/mods.toml file.
@Mod(Fordix.MODID)
public class Fordix {
    public static final String MODID = "fordix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Fordix() {
        LOGGER.info("Fordix UI library loaded");
        PanelNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PanelCommand.register(event.getDispatcher());
    }
}
