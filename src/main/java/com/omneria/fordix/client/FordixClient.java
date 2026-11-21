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
import com.omneria.fordix.client.gui.vanilla.PauseSkin;
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
    /**
     * Where the guide key works: in screens (over an item) and in the world (aimed at a creature). It only clashes with the
     * keys that also work in screens, so Shift + G is not reported against plain Shift (sneak) or plain G of the game, which
     * is what the "universal" context of Forge does to every combination (Controlling and the controls screen then flag it).
     */
    private static final net.minecraftforge.client.settings.IKeyConflictContext GUIDE_CONTEXT = new net.minecraftforge.client.settings.IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean conflicts(net.minecraftforge.client.settings.IKeyConflictContext other) {
            return other == this || other == net.minecraftforge.client.settings.KeyConflictContext.GUI
                    || other == net.minecraftforge.client.settings.KeyConflictContext.UNIVERSAL;
        }
    };

    public static final KeyMapping TOGGLE_DEMO = new FordixKeyMapping(
            "key.fordix.toggle_demo",
            net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            "key.categories.fordix"
    );

    /** No default key: settings are meant to be reached from the Mods list, this is a shortcut for those who want one. */
    public static final KeyMapping OPEN_SETTINGS = new FordixKeyMapping(
            "key.fordix.open_settings",
            net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.fordix"
    );

    /**
     * This key over an item opens the sheet or guide of its mod, and aimed at a creature opens its sheet. Shift + G by default, and
     * like any key of the game it can be set to another key with Shift, Ctrl or Alt, or to none (unbound).
     */
    public static final KeyMapping OPEN_GUIDE = new FordixKeyMapping(
            "key.fordix.open_guide",
            GUIDE_CONTEXT,
            net.minecraftforge.client.settings.KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.fordix"
    );

    /** Focuses the search field of the chat and of the inventories (Ctrl + F by default, changeable or unbound like the others). */
    public static final KeyMapping SEARCH_FIELD = new FordixKeyMapping(
            "key.fordix.search_field",
            net.minecraftforge.client.settings.KeyConflictContext.GUI,
            net.minecraftforge.client.settings.KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.categories.fordix"
    );

    /** Hold while looking at a tamed creature of yours: its interaction wheel opens (slide to a sector, let go). */
    public static final KeyMapping PET_WHEEL = new FordixKeyMapping(
            "key.fordix.pet_wheel",
            net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
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

    /** Whether this key press is the key of the mapping, with the Shift / Ctrl / Alt it was set with (never when unbound). */
    public static boolean pressed(KeyMapping mapping, int keyCode, int scanCode) {
        return !mapping.isUnbound() && mapping.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
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
            event.register(PET_WHEEL);
            event.register(SEARCH_FIELD);
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
            event.enqueueWork(() -> BorderlessFullscreen.install(Minecraft.getInstance()));
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
            Safe.run("FordixClient.clientTick", ForgeBusEvents::clientTick, event);
        }

        private static void clientTick(TickEvent.ClientTickEvent event) {
            BorderlessFullscreen.onClientTick(event);
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            TextureRecolor.onClientTick(minecraft);
            HudAlerts.tick(minecraft);
            DamageNumbers.tick(minecraft);
            com.omneria.fordix.client.hud.HudContextTicker.tick(minecraft);
            while (OPEN_GUIDE.consumeClick()) {
                net.minecraft.world.phys.EntityHitResult hit = minecraft.screen == null ? com.omneria.fordix.client.hud.FarTarget.entity(minecraft) : null;
                if (hit != null && !(hit.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
                    minecraft.setScreen(new com.omneria.fordix.client.gui.EntitySheetScreen(null, hit.getEntity().getType()));
                }
            }
            while (PET_WHEEL.consumeClick()) {
                if (minecraft.screen == null && minecraft.player != null && !minecraft.player.isSpectator()
                        && !com.omneria.fordix.client.gui.vanilla.HudSlots.hidden(com.omneria.fordix.client.pet.PetWheelScreen.ID)
                        && minecraft.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                        && com.omneria.fordix.client.pet.PetWheelScreen.opensOn(hit.getEntity(), minecraft.player)) {
                    minecraft.setScreen(new com.omneria.fordix.client.pet.PetWheelScreen((net.minecraft.world.entity.LivingEntity) hit.getEntity()));
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
            com.omneria.fordix.client.hud.ClientHudRules.clear();
            com.omneria.fordix.client.hud.HudContextTicker.reset();
        }

        @SubscribeEvent
        static void onScreenOpening(ScreenEvent.Opening event) {
            Safe.run("VanillaScreens.onScreenOpening", VanillaScreens::onScreenOpening, event);
            Safe.run("OptionsScreens.onOpening", com.omneria.fordix.client.gui.options.OptionsScreens::onOpening, event);
            Safe.run("ContainerMemory.onScreenOpening", ContainerMemory::onScreenOpening, event);
        }

        @SubscribeEvent
        static void onScreenClosing(ScreenEvent.Closing event) {
            Safe.run("ContainerMemory.onScreenClosing", ContainerMemory::onScreenClosing, event);
        }

        @SubscribeEvent
        static void onInventoryRender(ScreenEvent.Render.Post event) {
            Safe.run("InventoryTools.onScreenRenderPost", InventoryTools::onScreenRenderPost, event);
        }

        @SubscribeEvent
        static void onInventoryForeground(ContainerScreenEvent.Render.Foreground event) {
            Safe.run("InventoryTools.onForeground", InventoryTools::onForeground, event);
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        static void onInventoryMouse(ScreenEvent.MouseButtonPressed.Pre event) {
            Safe.run("InventoryTools.onMousePressed", InventoryTools::onMousePressed, event);
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        static void onInventoryKey(ScreenEvent.KeyPressed.Pre event) {
            Safe.run("InventoryTools.onKeyPressed", InventoryTools::onKeyPressed, event);
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        static void onInventoryChar(ScreenEvent.CharacterTyped.Pre event) {
            Safe.run("InventoryTools.onCharTyped", InventoryTools::onCharTyped, event);
        }

        @SubscribeEvent
        static void onInventoryTick(TickEvent.ClientTickEvent event) {
            Safe.run("InventoryTools.onClientTick", InventoryTools::onClientTick, event);
        }

        @SubscribeEvent
        static void onRenderLevel(RenderLevelStageEvent event) {
            Safe.run("DamageNumbers.onRenderLevel", DamageNumbers::onRenderLevel, event);
        }

        /** The effects list takes the place of vanilla's potion icons. */
        @SubscribeEvent
        static void onEffectsOverlayPre(RenderGuiOverlayEvent.Pre event) {
            Safe.run("InfoHud.onOverlayPre", InfoHud::onOverlayPre, event);
            Safe.run("PlayerListHud.onOverlayPre", PlayerListHud::onOverlayPre, event);
            Safe.run("ScoreboardHud.onOverlayPre", com.omneria.fordix.client.hud.ScoreboardHud::onOverlayPre, event);
            Safe.run("TitleHud.onOverlayPre", com.omneria.fordix.client.hud.TitleHud::onOverlayPre, event);
            Safe.run("SubtitleHud.onOverlayPre", com.omneria.fordix.client.hud.SubtitleHud::onOverlayPre, event);
            Safe.run("DebugHud.onOverlayPre", com.omneria.fordix.client.hud.DebugHud::onOverlayPre, event);
            Safe.run("ActionBarHud.onOverlayPre", com.omneria.fordix.client.hud.ActionBarHud::onOverlayPre, event);
        }

        @SubscribeEvent
        static void onPlaySound(net.minecraftforge.client.event.sound.PlaySoundEvent event) {
            Safe.run("MusicToast.onPlaySound", MusicToast::onPlaySound, event);
        }

        @SubscribeEvent
        static void onScreenInit(ScreenEvent.Init.Post event) {
            Safe.run("InventoryTools.onScreenInit", InventoryTools::onScreenInit, event);
            Safe.run("GuideTabs.onScreenInit", GuideTabs::onScreenInit, event);
        }

        /** After every other handler has added or moved its widgets (the pause menu, the guide tabs): lay out and skin them. */
        @SubscribeEvent(priority = EventPriority.LOW)
        static void onScreenInitSkin(ScreenEvent.Init.Post event) {
            com.omneria.fordix.client.gui.vanilla.RowButtons.clear();
            Safe.run("PauseSkin.onScreenInit", PauseSkin::onScreenInit, event);
            Safe.run("OptionsSkin.onScreenInit", com.omneria.fordix.client.gui.vanilla.OptionsSkin::onScreenInit, event);
            Safe.run("SubOptionsSkin.onScreenInit", com.omneria.fordix.client.gui.vanilla.SubOptionsSkin::onScreenInit, event);
            Safe.run("VoidframeButtons.onScreenInit", VoidframeButtons::onScreenInit, event);
        }

        @SubscribeEvent
        static void onBackgroundRendered(ScreenEvent.BackgroundRendered event) {
            Safe.run("ScreenBackdrop.onBackgroundRendered", com.omneria.fordix.client.gui.vanilla.ScreenBackdrop::onBackgroundRendered, event);
            Safe.run("PauseSkin.onBackgroundRendered", PauseSkin::onBackgroundRendered, event);
            Safe.run("OptionsSkin.onBackgroundRendered", com.omneria.fordix.client.gui.vanilla.OptionsSkin::onBackgroundRendered, event);
            Safe.run("SubOptionsSkin.onBackgroundRendered", com.omneria.fordix.client.gui.vanilla.SubOptionsSkin::onBackgroundRendered, event);
            Safe.run("VoidframeChrome.onBackgroundRendered", VoidframeChrome::onBackgroundRendered, event);
        }

        /** Highest priority: the opening "pop" scale must be undone before anything else draws after the screen. */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onScreenRenderPost(ScreenEvent.Render.Post event) {
            Safe.run("VoidframeChrome.onRenderPost", VoidframeChrome::onRenderPost, event);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onScreenRenderPostNotifications(ScreenEvent.Render.Post event) {
            Safe.run("NotificationOverlay.onScreenRenderPost", NotificationOverlay::onScreenRenderPost, event);
        }

        /** Lowest priority: notifications go over every other HUD overlay, JourneyMap's minimap included. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onRenderGui(RenderGuiEvent.Post event) {
            Safe.run("NotificationOverlay.onRenderGui", NotificationOverlay::onRenderGui, event);
        }

        /** End of the frame: make sure the attack indicator setting was given back. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onFrameEnd(RenderGuiEvent.Post event) {
            VoidframeGauges.restoreAttackIndicator(Minecraft.getInstance());
        }

        @SubscribeEvent
        static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
            Safe.run("RarityFrames.onOverlayPost", RarityFrames::onOverlayPost, event);
            Safe.run("VoidframeGauges.onOverlayPost", VoidframeGauges::onOverlayPost, event);
        }

        @SubscribeEvent
        static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
            Safe.run("VoidframeHud.onOverlayPre", VoidframeHud::onOverlayPre, event);
        }

        /** Highest priority: pushes the pose before any other listener can cancel the overlay (the Post event pops it). */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onExtraOverlayPre(RenderGuiOverlayEvent.Pre event) {
            Safe.run("ExtraHud.onOverlayPre", ExtraHud::onOverlayPre, event);
        }

        /** Normal priority: after the layout shift of {@link ExtraHud}, the plate goes under what another mod's overlay draws. */
        @SubscribeEvent
        static void onModPlates(RenderGuiOverlayEvent.Pre event) {
            Safe.run("ModPlates.onOverlayPre", com.omneria.fordix.client.hud.ModPlates::onOverlayPre, event);
        }

        /** Lowest priority, cancelled events included: balances the pose pushed for an overlay that got cancelled. */
        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        static void onExtraOverlayPreCancelled(RenderGuiOverlayEvent.Pre event) {
            Safe.run("ExtraHud.onOverlayPreCancelled", ExtraHud::onOverlayPreCancelled, event);
        }

        /** Normal priority: after the layout shift, replaces vanilla's chat overlay with the Voidframe chat. */
        @SubscribeEvent
        static void onChatOverlayPre(RenderGuiOverlayEvent.Pre event) {
            Safe.run("ChatPanel.onOverlayPre", ChatPanel::onOverlayPre, event);
        }

        @SubscribeEvent
        static void onChatScreenRenderPre(ScreenEvent.Render.Pre event) {
            Safe.run("ChatPanel.onScreenRenderPre", ChatPanel::onScreenRenderPre, event);
        }

        @SubscribeEvent
        static void onChatScreenInit(ScreenEvent.Init.Post event) {
            Safe.run("ChatPanel.onScreenInit", ChatPanel::onScreenInit, event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onChatScreenMouse(ScreenEvent.MouseButtonPressed.Pre event) {
            Safe.run("ChatPanel.onMousePressed", ChatPanel::onMousePressed, event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onChatScreenScroll(ScreenEvent.MouseScrolled.Pre event) {
            Safe.run("ChatPanel.onMouseScrolled", ChatPanel::onMouseScrolled, event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onChatScreenChar(ScreenEvent.CharacterTyped.Pre event) {
            Safe.run("ChatPanel.onCharTyped", ChatPanel::onCharTyped, event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onChatScreenKey(ScreenEvent.KeyPressed.Pre event) {
            Safe.run("ChatPanel.onKeyPressed", ChatPanel::onKeyPressed, event);
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
            Safe.run("InfoHud.onLoggingOut", InfoHud::onLoggingOut, event);
        }

        @SubscribeEvent
        static void onInfoHudLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            Safe.run("InfoHud.onLoggingIn", InfoHud::onLoggingIn, event);
        }

        /** Highest priority: the chat screen is shifted before anything else draws on it. */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void onChatInputRenderPre(ScreenEvent.Render.Pre event) {
            Safe.run("ExtraHud.onChatRenderPre", ExtraHud::onChatRenderPre, event);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onChatInputRenderPost(ScreenEvent.Render.Post event) {
            Safe.run("ExtraHud.onChatRenderPost", ExtraHud::onChatRenderPost, event);
        }

        @SubscribeEvent
        static void onHeldItemTick(TickEvent.ClientTickEvent event) {
            Safe.run("HeldItemHud.onClientTick", HeldItemHud::onClientTick, event);
        }

        /** Normal priority, so it runs before the lowest-priority pop: the model moves with the held item's name line. */
        @SubscribeEvent
        static void onHeldItemOverlayPost(RenderGuiOverlayEvent.Post event) {
            Safe.run("HeldItemHud.onOverlayPost", HeldItemHud::onOverlayPost, event);
        }

        /** Lowest priority: the rarity frames drawn after the hotbar follow it when it was moved. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onExtraOverlayPost(RenderGuiOverlayEvent.Post event) {
            Safe.run("ExtraHud.onOverlayPost", ExtraHud::onOverlayPost, event);
        }

        @SubscribeEvent
        static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
            Safe.run("VoidframeHud.onScreenRenderPre", VoidframeHud::onScreenRenderPre, event);
        }

        @SubscribeEvent
        static void onScreenRenderPostChat(ScreenEvent.Render.Post event) {
            Safe.run("VoidframeHud.onScreenRenderPost", VoidframeHud::onScreenRenderPost, event);
        }

        @SubscribeEvent
        static void onTooltipPre(RenderTooltipEvent.Pre event) {
            Safe.run("ItemTooltipRenderer.onPre", ItemTooltipRenderer::onPre, event);
        }

        @SubscribeEvent
        static void onSystemChat(net.minecraftforge.client.event.ClientChatReceivedEvent event) {
            Safe.run("ActionBarHud.onChat", com.omneria.fordix.client.hud.ActionBarHud::onChat, event);
        }

        @SubscribeEvent
        static void onItemTooltip(ItemTooltipEvent event) {
            Safe.run("TooltipStyle.onItemTooltip", TooltipStyle::onItemTooltip, event);
        }

        /** After everything else, so the guide line ends the tooltip. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onItemTooltipGuideHint(ItemTooltipEvent event) {
            Safe.run("GuideHints.onItemTooltip", GuideHints::onItemTooltip, event);
        }

        /** Fordix screens that need more room than the interface has shrink it for as long as they are open. */
        @SubscribeEvent
        static void onScreenInitFit(ScreenEvent.Init.Pre event) {
            com.omneria.fordix.client.gui.vanilla.QuarkButtons.allow(event.getScreen());
            if (event.getScreen() instanceof com.omneria.fordix.client.gui.FordixScreen screen) {
                screen.fitInterface(Minecraft.getInstance());
            }
        }

        /** Operators get a "Guides" button, lit when a server page changed since it was read. */
        @SubscribeEvent
        static void onPauseMenuInit(ScreenEvent.Init.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            boolean operator = minecraft.player != null && minecraft.player.hasPermissions(2);
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
                // Operators get a Guides row under the first button (the pause skin lays everything out again when it is on).
                if (!operator) {
                    return;
                }
                for (net.minecraft.client.gui.components.AbstractWidget widget : widgets) {
                    if (widget != back) {
                        widget.setY(widget.getY() + step);
                    }
                }
                event.addListener(new GuidesPauseButton(back.getX(), back.getY() + step, back.getWidth(),
                        ServerPages.hasUnread(), () -> Minecraft.getInstance().setScreen(new GuidesScreen(screen))));
            }
        }

        @SubscribeEvent
        static void onGuideKey(ScreenEvent.KeyPressed.Pre event) {
            Safe.run("GuideHints.onKeyPressed", GuideHints::onKeyPressed, event);
        }

        @SubscribeEvent
        static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            Safe.run("AnalysisCommand.onRegister", AnalysisCommand::onRegister, event);
        }

        @SubscribeEvent
        static void onContainerBackground(ContainerScreenEvent.Render.Background event) {
            Safe.run("RarityFrames.onContainerBackground", RarityFrames::onContainerBackground, event);
        }

        @SubscribeEvent
        static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
            Safe.run("RarityFrames.onContainerForeground", RarityFrames::onContainerForeground, event);
            Safe.run("VoidframeChrome.onContainerForeground", VoidframeChrome::onContainerForeground, event);
        }

        @SubscribeEvent
        static void onTooltipColor(RenderTooltipEvent.Color event) {
            Safe.run("VoidframeChrome.onTooltipColor", VoidframeChrome::onTooltipColor, event);
        }
    }

    /** Glass pause-menu button carrying a small "New" flag. */
    private static final class GuidesPauseButton extends net.minecraft.client.gui.components.Button {
        private final boolean unread;
        private final com.omneria.fordix.client.gui.Glass.Fade fade = new com.omneria.fordix.client.gui.Glass.Fade();

        GuidesPauseButton(int x, int y, int width, boolean unread, Runnable onPress) {
            super(x, y, width, 20, Tr.c("guides.button"), b -> onPress.run(), DEFAULT_NARRATION);
            this.unread = unread;
        }

        @Override
        public void renderWidget(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
            var font = Minecraft.getInstance().font;
            com.omneria.fordix.client.gui.vanilla.RowButtons.draw(g, font, this, getMessage().getString(), getX(), getY(), getWidth(), getHeight(),
                    fade.update(isHoveredOrFocused()), true);
        }
    }
}
