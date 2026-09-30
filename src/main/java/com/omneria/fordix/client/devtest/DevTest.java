package com.omneria.fordix.client.devtest;

import com.mojang.blaze3d.platform.NativeImage;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.client.gui.FordixAdminScreen;
import com.omneria.fordix.client.gui.FordixSettingsScreen;
import com.omneria.fordix.client.gui.HudLayoutScreen;
import com.omneria.fordix.client.notification.NotificationOverlay;
import com.omneria.fordix.notification.NotificationStyle;
import com.omneria.fordix.notification.network.ClientboundNotificationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Development-only harness, active only when the environment variable FORDIX_DEVTEST is set: it
 * opens a test world, walks through a list of scenes (HUD states, screens, tooltips) and saves a
 * screenshot of each in run/fordix-shots, so the interface can be checked without a person at the
 * keyboard. Uses mapped (dev) names for a few private fields, which only exist in a dev environment.
 */
public final class DevTest {
    private record Step(String name, Runnable setup, int frames, boolean hoverSlot) {
    }

    private final List<Step> steps = new ArrayList<>();
    private int index = -1;
    private int frame;
    private int worldTicks;
    private boolean worldRequested;
    private boolean started;
    private Path outDir;

    private int idleTicks;

    public static boolean enabled() {
        return System.getenv("FORDIX_DEVTEST") != null;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (++idleTicks % 200 == 0 && !started) {
            Fordix.LOGGER.info("[devtest] waiting, screen={} level={}", mc.screen, mc.level != null);
        }
        if (mc.level == null) {
            if (!worldRequested && mc.screen instanceof TitleScreen) {
                worldRequested = true;
                openWorld(mc);
            }
            return;
        }
        if (mc.player != null && ++worldTicks == 100 && !started) {
            started = true;
            buildSteps(mc);
            next(mc);
        }
    }

    private void openWorld(Minecraft mc) {
        Fordix.LOGGER.info("[devtest] creating the test world");
        LevelSettings settings = new LevelSettings("fordix-test-" + System.currentTimeMillis() % 100000, GameType.CREATIVE, false,
                Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, WorldOptions.defaultWithRandomSeed(),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    private void cmd(Minecraft mc, String command) {
        mc.player.connection.sendCommand(command);
    }

    private void buildSteps(Minecraft mc) {
        outDir = mc.gameDirectory.toPath().resolve("fordix-shots");
        try {
            Files.createDirectories(outDir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        mc.options.guiScale().set(3);
        mc.resizeDisplay();
        steps.add(new Step("00-setup", () -> {
            cmd(mc, "gamemode survival");
            cmd(mc, "clear @s");
            cmd(mc, "give @s minecraft:diamond_sword{Enchantments:[{id:\"minecraft:sharpness\",lvl:5},{id:\"minecraft:unbreaking\",lvl:3},{id:\"minecraft:mending\",lvl:1}]}");
            cmd(mc, "give @s minecraft:netherite_chestplate{Enchantments:[{id:\"minecraft:protection\",lvl:4},{id:\"minecraft:thorns\",lvl:3}],Damage:300}");
            cmd(mc, "give @s minecraft:iron_pickaxe{Enchantments:[{id:\"minecraft:efficiency\",lvl:3},{id:\"minecraft:fortune\",lvl:2}]}");
            cmd(mc, "give @s minecraft:golden_apple 8");
            cmd(mc, "give @s minecraft:diamond 12");
            cmd(mc, "give @s minecraft:iron_ingot 30");
            cmd(mc, "give @s minecraft:emerald 5");
            cmd(mc, "give @s minecraft:enchanted_golden_apple 2");
            cmd(mc, "give @s minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:mending\",lvl:1}]}");
            cmd(mc, "give @s minecraft:diamond_helmet");
            cmd(mc, "give @s minecraft:elytra");
            cmd(mc, "xp set @s 27 levels");
            cmd(mc, "effect give @s minecraft:instant_damage 1 0");
            cmd(mc, "bossbar add fordix:test \"Le Wither\"");
            cmd(mc, "bossbar set fordix:test players @a");
            cmd(mc, "bossbar set fordix:test color purple");
            cmd(mc, "bossbar set fordix:test style notched_10");
            cmd(mc, "bossbar set fordix:test value 64");
        }, 60, false));
        steps.add(new Step("01-hud", () -> mc.setScreen(null), 40, false));
        steps.add(new Step("02-notification", () -> NotificationOverlay.receive(new ClientboundNotificationPayload(
                NotificationStyle.SUCCESS, Component.literal("Bienvenue sur Astral Nexus v0.1.5"),
                Component.literal("Bon jeu ! Ceci est une notification de test plus longue pour verifier la taille du fond."), 400)), 30, false));
        steps.add(new Step("03-inventory", () -> mc.setScreen(new InventoryScreen(mc.player)), 30, false));
        steps.add(new Step("04-tooltip-sword", () -> mc.setScreen(new InventoryScreen(mc.player)), 30, true));
        steps.add(new Step("05-advancements", () -> mc.setScreen(new AdvancementsScreen(mc.player.connection.getAdvancements())), 40, false));
        steps.add(new Step("06-loom", () -> mc.setScreen(new LoomScreen(new LoomMenu(1, mc.player.getInventory()),
                mc.player.getInventory(), Component.translatable("container.loom"))), 30, false));
        steps.add(new Step("07-cartography", () -> mc.setScreen(new CartographyTableScreen(new CartographyTableMenu(1, mc.player.getInventory()),
                mc.player.getInventory(), Component.translatable("container.cartography_table"))), 30, false));
        steps.add(new Step("08-creative", () -> {
            cmd(mc, "gamemode creative");
            mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), false));
        }, 40, false));
        steps.add(new Step("09-settings", () -> {
            cmd(mc, "gamemode survival");
            mc.setScreen(new FordixSettingsScreen(null));
        }, 30, false));
        steps.add(new Step("10-hud-layout", () -> mc.setScreen(new HudLayoutScreen(null)), 30, false));
        steps.add(new Step("11-admin", () -> mc.setScreen(new FordixAdminScreen(null)), 50, false));
        steps.add(new Step("12-chat", () -> mc.setScreen(new ChatScreen("/fordix notify")), 30, false));
        steps.add(new Step("13-pause", () -> mc.setScreen(null), 10, false));
        steps.add(new Step("14-pause-menu", () -> mc.pauseGame(false), 30, false));
        steps.add(new Step("99-done", () -> {
            Fordix.LOGGER.info("[devtest] finished, shots in {}", outDir);
            mc.stop();
        }, 5, false));
    }

    private void next(Minecraft mc) {
        index++;
        frame = 0;
        if (index < steps.size()) {
            Step step = steps.get(index);
            Fordix.LOGGER.info("[devtest] step {}", step.name());
            step.setup().run();
        }
    }

    @SubscribeEvent
    public void onFrame(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            advance(mc);
        }
    }

    @SubscribeEvent
    public void onScreenFrame(ScreenEvent.Render.Post event) {
        advance(Minecraft.getInstance());
    }

    private void advance(Minecraft mc) {
        if (!started || index < 0 || index >= steps.size()) {
            return;
        }
        Step step = steps.get(index);
        if (step.hoverSlot() && mc.screen instanceof InventoryScreen screen) {
            hover(mc, screen, 0);
        }
        if (++frame == step.frames()) {
            if (!step.name().startsWith("00") && !step.name().startsWith("13") && !step.name().startsWith("99")) {
                shot(mc, step.name());
            }
            next(mc);
        }
    }

    /** Puts the mouse over the first hotbar slot's item (sword) - the slots are laid out at fixed offsets. */
    private void hover(Minecraft mc, InventoryScreen screen, int hotbarIndex) {
        try {
            var slot = screen.getMenu().slots.stream()
                    .filter(s -> s.getContainerSlot() == hotbarIndex && s.container == mc.player.getInventory()).findFirst().orElse(null);
            if (slot == null) {
                return;
            }
            double scale = mc.getWindow().getGuiScale();
            double x = (screen.getGuiLeft() + slot.x + 8) * scale;
            double y = (screen.getGuiTop() + slot.y + 8) * scale;
            set(mc.mouseHandler, "xpos", x);
            set(mc.mouseHandler, "ypos", y);
        } catch (ReflectiveOperationException e) {
            Fordix.LOGGER.warn("[devtest] could not move the mouse", e);
        }
    }

    private static void set(Object target, String name, double value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setDouble(target, value);
    }

    private void shot(Minecraft mc, String name) {
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(outDir.resolve(name + ".png"));
        } catch (IOException e) {
            Fordix.LOGGER.warn("[devtest] screenshot failed", e);
        }
    }
}
