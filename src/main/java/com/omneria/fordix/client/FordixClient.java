package com.omneria.fordix.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Theme;
import com.omneria.fordix.client.gui.FordixDemoScreen;
import com.omneria.fordix.client.gui.vanilla.VanillaScreens;
import com.omneria.fordix.client.gui.vanilla.VoidframeButtons;
import com.omneria.fordix.client.gui.vanilla.VoidframeChrome;
import com.omneria.fordix.client.guide.GuideTabs;
import com.omneria.fordix.client.notification.NotificationOverlay;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Split into two @EventBusSubscriber classes because a class-level subscriber
 * annotation only targets one bus (MOD or FORGE) at a time - key mapping
 * registration fires on the mod bus, tick events on the game (forge) bus.
 */
public final class FordixClient {
    public static final KeyMapping TOGGLE_DEMO = new KeyMapping(
            "key.fordix.toggle_demo",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            "key.categories.fordix"
    );

    private FordixClient() {
    }

    @Mod.EventBusSubscriber(modid = Fordix.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_DEMO);
        }

        @SubscribeEvent
        static void onConfigLoading(ModConfigEvent.Loading event) {
            applyTheme(event);
        }

        @SubscribeEvent
        static void onConfigReloading(ModConfigEvent.Reloading event) {
            applyTheme(event);
        }

        private static void applyTheme(ModConfigEvent event) {
            if (event.getConfig().getSpec() != FordixConfig.CLIENT_SPEC) {
                return;
            }
            Theme theme = Theme.byName(FordixConfig.THEME.get());
            Integer accent = Theme.parseHex(FordixConfig.ACCENT_COLOR.get());
            FordixPalette.apply(accent != null ? theme.withAccent(accent) : theme);
        }
    }

    @Mod.EventBusSubscriber(modid = Fordix.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() {
        }

        @SubscribeEvent
        static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            while (TOGGLE_DEMO.consumeClick()) {
                if (minecraft.screen == null) {
                    minecraft.setScreen(new FordixDemoScreen());
                }
            }
        }

        @SubscribeEvent
        static void onScreenOpening(ScreenEvent.Opening event) {
            VanillaScreens.onScreenOpening(event);
        }

        @SubscribeEvent
        static void onScreenInit(ScreenEvent.Init.Post event) {
            VoidframeButtons.onScreenInit(event);
            GuideTabs.onScreenInit(event);
        }

        @SubscribeEvent
        static void onBackgroundRendered(ScreenEvent.BackgroundRendered event) {
            VoidframeChrome.onBackgroundRendered(event);
        }

        /** Highest priority: the opening "pop" scale must be undone before anything else draws after the screen. */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onScreenRenderPost(ScreenEvent.Render.Post event) {
            VoidframeChrome.onRenderPost(event);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onScreenRenderPostNotifications(ScreenEvent.Render.Post event) {
            NotificationOverlay.onScreenRenderPost(event);
        }

        @SubscribeEvent
        static void onRenderGui(RenderGuiEvent.Post event) {
            NotificationOverlay.onRenderGui(event);
        }

        @SubscribeEvent
        static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
            VoidframeChrome.onContainerForeground(event);
        }

        @SubscribeEvent
        static void onTooltipColor(RenderTooltipEvent.Color event) {
            VoidframeChrome.onTooltipColor(event);
        }
    }
}
