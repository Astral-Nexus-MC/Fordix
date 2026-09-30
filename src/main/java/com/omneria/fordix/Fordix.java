package com.omneria.fordix;

import com.mojang.logging.LogUtils;
import com.omneria.fordix.guide.server.GuideBroadcaster;
import com.omneria.fordix.notification.server.AutoNotifications;
import com.omneria.fordix.notification.server.NotificationScheduler;
import com.omneria.fordix.notification.server.NotifyCommand;
import com.omneria.fordix.welcome.FordixItems;
import com.omneria.fordix.welcome.WelcomeCommand;
import com.omneria.fordix.welcome.WelcomeService;
import com.omneria.fordix.panel.network.PanelNetwork;
import com.omneria.fordix.panel.server.PanelCommand;
import com.omneria.fordix.panel.server.PanelRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FordixServerConfig.SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(AutoNotifications::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(AutoNotifications::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(AutoNotifications::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(NotificationScheduler::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(NotificationScheduler::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(NotificationScheduler::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(GuideBroadcaster::onPlayerLoggedIn);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(GuideBroadcaster::onConfigReloading);
        FordixItems.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(WelcomeService::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(WelcomeService::onCommand);
        MinecraftForge.EVENT_BUS.addListener(WelcomeService::onAdvancement);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PanelCommand.register(event.getDispatcher());
        NotifyCommand.register(event.getDispatcher());
        WelcomeCommand.register(event.getDispatcher());
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PanelRegistry.forgetPlayer(event.getEntity().getUUID());
    }
}
