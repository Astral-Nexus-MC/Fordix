package com.omneria.fordix.client;

import com.omneria.fordix.Tr;
import com.mojang.blaze3d.platform.InputConstants;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Theme;
import com.omneria.fordix.client.devtest.DevTest;
import com.omneria.fordix.client.gui.FordixDemoScreen;
import com.omneria.fordix.client.gui.FordixSettingsScreen;
import com.omneria.fordix.client.gui.vanilla.RarityFrames;
import com.omneria.fordix.client.gui.vanilla.TextureRecolor;
import com.omneria.fordix.client.gui.vanilla.VanillaScreens;
import com.omneria.fordix.client.gui.vanilla.VoidframeButtons;
import com.omneria.fordix.client.gui.vanilla.VoidframeChrome;
import com.omneria.fordix.client.gui.vanilla.VoidframeDurability;
import com.omneria.fordix.client.gui.vanilla.VoidframeGauges;
import com.omneria.fordix.client.gui.vanilla.VoidframeHud;
import com.omneria.fordix.client.guide.GuideTabs;
import com.omneria.fordix.client.guide.ServerGuides;
import com.omneria.fordix.client.guide.GuideHints;
import com.omneria.fordix.client.guide.ServerPages;
import com.omneria.fordix.client.chat.ChatLog;
import com.omneria.fordix.client.chat.ChatPanel;
import com.omneria.fordix.client.hud.ContainerMemory;
import com.omneria.fordix.client.gui.vanilla.InventoryTools;
import com.omneria.fordix.client.hud.DamageNumbers;
import com.omneria.fordix.client.hud.HudAlerts;
import com.omneria.fordix.client.hud.InfoHud;
import com.omneria.fordix.client.hud.PlayerListHud;
import com.omneria.fordix.client.gui.vanilla.HudProfileActions;
import com.omneria.fordix.client.gui.vanilla.ExtraHud;
import com.omneria.fordix.client.gui.vanilla.HeldItemHud;
import com.omneria.fordix.client.notification.NotificationOverlay;
import com.omneria.fordix.client.tooltip.AnalysisCommand;
import com.omneria.fordix.client.tooltip.ItemTooltipRenderer;
import com.omneria.fordix.client.tooltip.ServerRarities;
import com.omneria.fordix.client.tooltip.TooltipStyle;
import net.minecraft.client.KeyMapping;
import com.omneria.fordix.client.gui.GuidesScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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

    /** No default key: settings are meant to be reached from the Mods list, this is a shortcut for those who want one. */
    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.fordix.open_settings",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.fordix"
    );

    /** Shift + this key over an item opens the guide of its mod (default G; Shift keeps it clear of text fields). */
    public static final KeyMapping OPEN_GUIDE = new KeyMapping(
            "key.fordix.open_guide",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.fordix"
    );

    /** Loads the next saved HUD layout (profiles are filled from the HUD layout screen). */
    public static final KeyMapping NEXT_PROFILE = new KeyMapping(
            "key.fordix.next_hud_profile",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "key.categories.fordix"
    );

    static {
        // Dev-only screenshot harness (see DevTest): only with FORDIX_DEVTEST set in the environment.
        if (DevTest.enabled()) {
            MinecraftForge.EVENT_BUS.register(new DevTest());
        }
    }

    private FordixClient() {
    }

    /** Pushes the client config's theme and accent color into {@link FordixPalette}. */
    public static void applyConfig() {
        Theme theme = Theme.byName(FordixConfig.THEME.get());
        Integer accent = Theme.parseHex(FordixConfig.ACCENT_COLOR.get());
        FordixPalette.apply(accent != null ? theme.withAccent(accent) : theme);
        TextureRecolor.refresh();
    }

    @Mod.EventBusSubscriber(modid = Fordix.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            InfoHud.registerOverlays(event);
        }

        @SubscribeEvent
        static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_DEMO);
            event.register(OPEN_SETTINGS);
            event.register(OPEN_GUIDE);
            event.register(NEXT_PROFILE);
        }

        /** Recolors the recipe book and advancement textures once resources are loaded, and again after each reload. */
        @SubscribeEvent
        static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(TextureRecolor.RELOAD_LISTENER);
        }

        /** Durability bars under damaged items, for every item that has durability. */
        @SubscribeEvent
        static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
            VoidframeDurability.onRegister(event);
        }

        /** The "Config" button of the Mods list opens Fordix's settings screen. */
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            com.omneria.fordix.client.tooltip.AttributeMath.separator = () ->
                    Minecraft.getInstance().getLanguageManager().getSelected().startsWith("fr") ? ',' : '.';
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new FordixSettingsScreen(parent)));
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
            applyConfig();
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
            TextureRecolor.onClientTick(minecraft);
            HudAlerts.tick(minecraft);
            DamageNumbers.tick(minecraft);
            HudProfileActions.autoTick(minecraft);
            while (TOGGLE_DEMO.consumeClick()) {
                if (minecraft.screen == null) {
                    minecraft.setScreen(new FordixDemoScreen());
                }
            }
            while (NEXT_PROFILE.consumeClick()) {
                if (minecraft.screen == null) {
                    HudProfileActions.loadNext(minecraft);
                }
            }
            while (OPEN_GUIDE.consumeClick()) {
                if (minecraft.screen == null && net.minecraft.client.gui.screens.Screen.hasShiftDown()
                        && minecraft.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                        && !(hit.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
                    minecraft.setScreen(new com.omneria.fordix.client.gui.EntitySheetScreen(null, hit.getEntity().getType()));
                }
            }
            while (OPEN_SETTINGS.consumeClick()) {
                if (minecraft.screen == null) {
                    minecraft.setScreen(new FordixSettingsScreen(null));
                }
            }
        }

        /** A server's guide list applies to that server only. */
        @SubscribeEvent
        static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            ServerGuides.clear();
            ServerPages.clear();
            ServerRarities.clear();
            HudAlerts.reset();
            com.omneria.fordix.client.wiki.ClientWiki.clear();
            DamageNumbers.reset();
            InventoryTools.reset();
            ContainerMemory.clear();
            HudProfileActions.onLoggingOut();
        }

        @SubscribeEvent
        static void onScreenOpening(ScreenEvent.Opening event) {
            VanillaScreens.onScreenOpening(event);
            ContainerMemory.onScreenOpening(event);
        }

        @SubscribeEvent
        static void onScreenClosing(ScreenEvent.Closing event) {
            ContainerMemory.onScreenClosing(event);
        }

        @SubscribeEvent
        static void onInventoryRender(ScreenEvent.Render.Post event) {
            InventoryTools.onScreenRenderPost(event);
        }

        @SubscribeEvent
        static void onInventoryForeground(ContainerScreenEvent.Render.Foreground event) {
            InventoryTools.onForeground(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        static void onInventoryMouse(ScreenEvent.MouseButtonPressed.Pre event) {
            InventoryTools.onMousePressed(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        static void onInventoryKey(ScreenEvent.KeyPressed.Pre event) {
            InventoryTools.onKeyPressed(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        static void onInventoryChar(ScreenEvent.CharacterTyped.Pre event) {
            InventoryTools.onCharTyped(event);
        }

        @SubscribeEvent
        static void onInventoryTick(TickEvent.ClientTickEvent event) {
            InventoryTools.onClientTick(event);
        }

        @SubscribeEvent
        static void onRenderLevel(RenderLevelStageEvent event) {
            DamageNumbers.onRenderLevel(event);
        }

        /** The effects list takes the place of vanilla's potion icons. */
        @SubscribeEvent
        static void onEffectsOverlayPre(RenderGuiOverlayEvent.Pre event) {
            InfoHud.onOverlayPre(event);
            PlayerListHud.onOverlayPre(event);
        }

        @SubscribeEvent
        static void onScreenInit(ScreenEvent.Init.Post event) {
            VoidframeButtons.onScreenInit(event);
            InventoryTools.onScreenInit(event);
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

        /** Lowest priority: notifications go over every other HUD overlay, JourneyMap's minimap included. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onRenderGui(RenderGuiEvent.Post event) {
            NotificationOverlay.onRenderGui(event);
        }

        /** End of the frame: make sure the attack indicator setting was given back. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onFrameEnd(RenderGuiEvent.Post event) {
            VoidframeGauges.restoreAttackIndicator(Minecraft.getInstance());
        }

        @SubscribeEvent
        static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
            RarityFrames.onOverlayPost(event);
            VoidframeGauges.onOverlayPost(event);
        }

        @SubscribeEvent
        static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
            VoidframeHud.onOverlayPre(event);
        }

        /** Highest priority: pushes the pose before any other listener can cancel the overlay (the Post event pops it). */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onExtraOverlayPre(RenderGuiOverlayEvent.Pre event) {
            ExtraHud.onOverlayPre(event);
        }

        /** Lowest priority, cancelled events included: balances the pose pushed for an overlay that got cancelled. */
        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        static void onExtraOverlayPreCancelled(RenderGuiOverlayEvent.Pre event) {
            ExtraHud.onOverlayPreCancelled(event);
        }

        /** Normal priority: after the layout shift, replaces vanilla's chat overlay with the Voidframe chat. */
        @SubscribeEvent
        static void onChatOverlayPre(RenderGuiOverlayEvent.Pre event) {
            ChatPanel.onOverlayPre(event);
        }

        @SubscribeEvent
        static void onChatScreenRenderPre(ScreenEvent.Render.Pre event) {
            ChatPanel.onScreenRenderPre(event);
        }

        @SubscribeEvent
        static void onChatScreenInit(ScreenEvent.Init.Post event) {
            ChatPanel.onScreenInit(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onChatScreenMouse(ScreenEvent.MouseButtonPressed.Pre event) {
            ChatPanel.onMousePressed(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onChatScreenScroll(ScreenEvent.MouseScrolled.Pre event) {
            ChatPanel.onMouseScrolled(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onChatScreenChar(ScreenEvent.CharacterTyped.Pre event) {
            ChatPanel.onCharTyped(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onChatScreenKey(ScreenEvent.KeyPressed.Pre event) {
            ChatPanel.onKeyPressed(event);
        }

        @SubscribeEvent
        static void onChatTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                ChatLog.tick(Minecraft.getInstance());
            }
        }

        @SubscribeEvent
        static void onChatLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ChatLog.clear();
            InfoHud.onLoggingOut(event);
        }

        @SubscribeEvent
        static void onInfoHudLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            InfoHud.onLoggingIn(event);
        }

        /** Highest priority: the chat screen is shifted before anything else draws on it. */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onChatInputRenderPre(ScreenEvent.Render.Pre event) {
            ExtraHud.onChatRenderPre(event);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onChatInputRenderPost(ScreenEvent.Render.Post event) {
            ExtraHud.onChatRenderPost(event);
        }

        @SubscribeEvent
        static void onHeldItemTick(TickEvent.ClientTickEvent event) {
            HeldItemHud.onClientTick(event);
        }

        /** Normal priority, so it runs before the lowest-priority pop: the model moves with the held item's name line. */
        @SubscribeEvent
        static void onHeldItemOverlayPost(RenderGuiOverlayEvent.Post event) {
            HeldItemHud.onOverlayPost(event);
        }

        /** Lowest priority: the rarity frames drawn after the hotbar follow it when it was moved. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onExtraOverlayPost(RenderGuiOverlayEvent.Post event) {
            ExtraHud.onOverlayPost(event);
        }

        @SubscribeEvent
        static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
            VoidframeHud.onScreenRenderPre(event);
        }

        @SubscribeEvent
        static void onScreenRenderPostChat(ScreenEvent.Render.Post event) {
            VoidframeHud.onScreenRenderPost(event);
        }

        @SubscribeEvent
        static void onTooltipPre(RenderTooltipEvent.Pre event) {
            ItemTooltipRenderer.onPre(event);
        }

        @SubscribeEvent
        static void onItemTooltip(ItemTooltipEvent event) {
            TooltipStyle.onItemTooltip(event);
        }

        /** After everything else, so the guide line ends the tooltip. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onItemTooltipGuideHint(ItemTooltipEvent event) {
            GuideHints.onItemTooltip(event);
        }

        /** A "Guides" button in the pause menu, lit when a server page changed since it was read. */
        @SubscribeEvent
        static void onPauseMenuInit(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof net.minecraft.client.gui.screens.PauseScreen screen) {
                net.minecraft.client.gui.components.AbstractWidget back = null;
                net.minecraft.client.gui.components.AbstractWidget advancements = null;
                net.minecraft.client.gui.components.AbstractWidget stats = null;
                java.util.List<net.minecraft.client.gui.components.AbstractWidget> widgets = new java.util.ArrayList<>();
                for (net.minecraft.client.gui.components.events.GuiEventListener child : event.getListenersList()) {
                    if (child instanceof net.minecraft.client.gui.components.Button widget) {
                        widgets.add(widget);
                        String key = widget.getMessage().getContents() instanceof
                                net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "";
                        switch (key) {
                            case "menu.returnToGame" -> back = widget;
                            case "gui.advancements" -> advancements = widget;
                            case "gui.stats" -> stats = widget;
                            default -> { }
                        }
                    }
                }
                if (back == null) {
                    return;
                }
                int step = back.getHeight() + 4;
                event.removeListener(advancements);
                for (net.minecraft.client.gui.components.AbstractWidget widget : widgets) {
                    if (widget != back && widget != advancements) {
                        widget.setY(widget.getY() + step);
                    }
                }
                if (advancements != null && stats != null) {
                    stats.setX(back.getX());
                    stats.setWidth(back.getWidth());
                }
                event.addListener(new GuidesPauseButton(back.getX(), back.getY() + step, back.getWidth(),
                        ServerPages.hasUnread(), () -> Minecraft.getInstance().setScreen(new GuidesScreen(screen))));
            }
        }

        @SubscribeEvent
        static void onGuideKey(ScreenEvent.KeyPressed.Pre event) {
            GuideHints.onKeyPressed(event);
        }

        @SubscribeEvent
        static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            AnalysisCommand.onRegister(event);
        }

        @SubscribeEvent
        static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
            RarityFrames.onContainerForeground(event);
            VoidframeChrome.onContainerForeground(event);
        }

        @SubscribeEvent
        static void onTooltipColor(RenderTooltipEvent.Color event) {
            VoidframeChrome.onTooltipColor(event);
        }
    }

    /** Vanilla-styled pause-menu button carrying a small "New" flag. */
    private static final class GuidesPauseButton extends net.minecraft.client.gui.components.Button {
        private final boolean unread;

        GuidesPauseButton(int x, int y, int width, boolean unread, Runnable onPress) {
            super(x, y, width, 20, Tr.c("guides.button"), b -> onPress.run(), DEFAULT_NARRATION);
            this.unread = unread;
        }

        @Override
        public void renderWidget(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
            super.renderWidget(g, mx, my, pt);
            if (unread) {
                var font = Minecraft.getInstance().font;
                int w = font.width(Tr.s("guides.new_badge")) + 6;
                int x = getX() + getWidth() - w - 4;
                int y = getY() + 5;
                g.fill(x, y, x + w, y + 10, com.omneria.fordix.client.gui.FordixPalette.ACCENT | 0xFF000000);
                g.drawString(font, Tr.s("guides.new_badge"), x + 3, y + 1, 0xFF101018, false);
            }
        }
    }
}
