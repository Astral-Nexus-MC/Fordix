package com.omneria.fordix.client.devtest;

import com.mojang.blaze3d.platform.NativeImage;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.client.gui.FordixAdminScreen;
import com.omneria.fordix.client.gui.FordixSettingsScreen;
import com.omneria.fordix.client.gui.GuidesScreen;
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
    private HudLayoutScreen layoutForExtras;
    private int index = -1;
    private int frame;
    private int worldTicks;
    private boolean worldRequested;
    private boolean started;
    private Path outDir;

    private int hoverIndex;
    private int idleTicks;
    private String pendingShot;

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

    /** A real click on the recipe book toggle, as a player would do it. */
    private static void clickBook(Minecraft mc) {
        for (net.minecraft.client.gui.components.events.GuiEventListener child : mc.screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget w && child.getClass().getSimpleName().equals("StandIn")) {
                child.mouseClicked(w.getX() + 3, w.getY() + 3, 0);
                if (!((net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener) mc.screen).getRecipeBookComponent().isVisible()) {
                    // The book setting is shared by the inventory and the crafting table: open it either way.
                    child.mouseClicked(w.getX() + 3, w.getY() + 3, 0);
                }
                return;
            }
        }
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
        String scaleVariable = System.getenv("FORDIX_DEVTEST_SCALE");
        mc.options.guiScale().set(scaleVariable == null ? 3 : Integer.parseInt(scaleVariable));
        String sizeVariable = System.getenv("FORDIX_DEVTEST_SIZE");
        if (sizeVariable != null && sizeVariable.contains("x")) {
            String[] size = sizeVariable.split("x");
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
        mc.resizeDisplay();
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        steps.add(new Step("00-setup", () -> {
            // The guided tour of the HUD editor starts by itself on a first open: the other steps need the editor free.
            try {
                java.nio.file.Files.writeString(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("fordix-hud-editor-seen.txt"), "1");
            } catch (java.io.IOException ignored) {
                // The steps below would then start with the tour on: only the HUD editor ones are affected.
            }
            cmd(mc, "gamemode survival");
            mc.player.getInventory().setItem(10, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPASS));
            mc.player.getInventory().setItem(11, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CLOCK));
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
                Component.literal("Bon jeu ! Ceci est une notification de test plus longue pour verifier la taille du fond."), 80)), 30, false));
        steps.add(new Step("03-inventory", () -> mc.setScreen(new InventoryScreen(mc.player)), 30, false));
        steps.add(new Step("03b-inventory-book", () -> {
            mc.setScreen(new InventoryScreen(mc.player));
            clickBook(mc);
        }, 40, false));
        steps.add(new Step("03f-book-return", () -> {
            mc.setScreen(new InventoryScreen(mc.player));
            clickBook(mc);
            net.minecraft.client.gui.screens.Screen back = mc.screen;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.CraftingScreen(
                    new net.minecraft.world.inventory.CraftingMenu(1, mc.player.getInventory()), mc.player.getInventory(),
                    Component.literal("x")));
            mc.setScreen(back);
        }, 40, false));
        steps.add(new Step("03d-crafting-book", () -> {
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.CraftingScreen(
                    new net.minecraft.world.inventory.CraftingMenu(1, mc.player.getInventory()), mc.player.getInventory(),
                    Component.translatable("container.crafting")));
            clickBook(mc);
        }, 40, false));
        steps.add(new Step("03e-furnace-book", () -> {
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.FurnaceScreen(
                    new net.minecraft.world.inventory.FurnaceMenu(1, mc.player.getInventory()), mc.player.getInventory(),
                    Component.translatable("container.furnace")));
            clickBook(mc);
        }, 40, false));
        steps.add(new Step("03c-inventory-book-closed", () -> {
            mc.player.getRecipeBook().getBookSettings().setOpen(net.minecraft.world.inventory.RecipeBookType.CRAFTING, false);
            mc.setScreen(new InventoryScreen(mc.player));
        }, 40, false));
        steps.add(new Step("03g-inventory-curios", () -> {
            com.omneria.fordix.client.gui.vanilla.CuriosView.force(java.util.List.of(
                    new com.omneria.fordix.client.gui.vanilla.CuriosView.Cell(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND), "ring"),
                    new com.omneria.fordix.client.gui.vanilla.CuriosView.Cell(net.minecraft.world.item.ItemStack.EMPTY, "ring"),
                    new com.omneria.fordix.client.gui.vanilla.CuriosView.Cell(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR), "charm"),
                    new com.omneria.fordix.client.gui.vanilla.CuriosView.Cell(net.minecraft.world.item.ItemStack.EMPTY, "necklace")));
            mc.setScreen(new InventoryScreen(mc.player));
        }, 40, false));
        steps.add(new Step("03h-inventory-curios-off", () -> {
            com.omneria.fordix.client.gui.vanilla.CuriosView.force(null);
            mc.setScreen(new InventoryScreen(mc.player));
        }, 30, false));
        steps.add(new Step("07h-flower-pouch", () -> mc.setScreen(com.omneria.fordix.client.gui.vanilla.ModContainerScreen.forTest(
                com.omneria.fordix.client.gui.vanilla.ModContainerScreen.Kind.POUCH, fakeMenu(mc, 32), mc.player.getInventory(),
                Component.literal("Flower Pouch"))), 30, false));
        steps.add(new Step("07i-bauble-box", () -> mc.setScreen(com.omneria.fordix.client.gui.vanilla.ModContainerScreen.forTest(
                com.omneria.fordix.client.gui.vanilla.ModContainerScreen.Kind.BOX, fakeMenu(mc, 24), mc.player.getInventory(),
                Component.literal("Bauble Box"))), 30, false));
        steps.add(new Step("04-tooltip-sword", () -> mc.setScreen(new InventoryScreen(mc.player)), 30, true));
        steps.add(new Step("05-advancements", () -> mc.setScreen(new AdvancementsScreen(mc.player.connection.getAdvancements())), 40, false));
        steps.add(new Step("06-loom", () -> mc.setScreen(new LoomScreen(new LoomMenu(1, mc.player.getInventory()),
                mc.player.getInventory(), Component.translatable("container.loom"))), 30, false));
        steps.add(new Step("07-cartography", () -> mc.setScreen(new CartographyTableScreen(new CartographyTableMenu(1, mc.player.getInventory()),
                mc.player.getInventory(), Component.translatable("container.cartography_table"))), 30, false));
        steps.add(new Step("07b-anvil", () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.AnvilScreen(
                new net.minecraft.world.inventory.AnvilMenu(1, mc.player.getInventory()), mc.player.getInventory(),
                Component.translatable("container.repair"))), 30, false));
        steps.add(new Step("07c-brewing", () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.BrewingStandScreen(
                new net.minecraft.world.inventory.BrewingStandMenu(1, mc.player.getInventory()), mc.player.getInventory(),
                Component.translatable("container.brewing"))), 30, false));
        steps.add(new Step("07d-enchanting", () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.EnchantmentScreen(
                new net.minecraft.world.inventory.EnchantmentMenu(1, mc.player.getInventory()), mc.player.getInventory(),
                Component.translatable("container.enchant"))), 30, false));
        steps.add(new Step("07e-furnace", () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.FurnaceScreen(
                new net.minecraft.world.inventory.FurnaceMenu(1, mc.player.getInventory()), mc.player.getInventory(),
                Component.translatable("container.furnace"))), 30, false));
        steps.add(new Step("07f-chest", () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.ContainerScreen(
                net.minecraft.world.inventory.ChestMenu.threeRows(1, mc.player.getInventory()), mc.player.getInventory(),
                Component.translatable("container.chest"))), 30, false));
        steps.add(new Step("07g-title", () -> {
            mc.setScreen(null);
            mc.gui.setTimes(0, 400, 20);
            mc.gui.setSubtitle(Component.literal("Niveau de danger : eleve"));
            mc.gui.setTitle(Component.literal("La foret d'emeraude"));
        }, 25, false));
        steps.add(new Step("08-creative", () -> {
            cmd(mc, "gamemode creative");
            mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), false));
        }, 40, false));
        for (String[] tab : new String[][]{{"08b-creative-inventory", "inventory"}, {"08c-creative-search", "search"}}) {
            steps.add(new Step(tab[0], () -> {
                cmd(mc, "gamemode creative");
                mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), false));
                try {
                    net.minecraft.world.item.CreativeModeTab target = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get(
                            tab[1].equals("search") ? net.minecraft.world.item.CreativeModeTabs.SEARCH : net.minecraft.world.item.CreativeModeTabs.INVENTORY);
                    for (java.lang.reflect.Method m : CreativeModeInventoryScreen.class.getDeclaredMethods()) {
                        if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == net.minecraft.world.item.CreativeModeTab.class
                                && m.getReturnType() == void.class) {
                            m.setAccessible(true);
                            m.invoke(mc.screen, target);
                            break;
                        }
                    }
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }, 40, false));
        }
        for (int tourStep : new int[]{0, 1, 2, 4}) {
            steps.add(new Step("10t" + tourStep + "-hud-tour", () -> {
                HudLayoutScreen layout = new HudLayoutScreen(null);
                mc.setScreen(layout);
                try {
                    java.lang.reflect.Field f = HudLayoutScreen.class.getDeclaredField("tour");
                    f.setAccessible(true);
                    f.setInt(layout, tourStep);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }, 30, false));
        }
        for (int[] pv : new int[][]{{0, 0}, {3, 3}, {7, 7}}) {
            steps.add(new Step("10p" + pv[0] + "-hud-preview", () -> {
                HudLayoutScreen layout = new HudLayoutScreen(null);
                mc.setScreen(layout);
                try {
                    java.lang.reflect.Field f = HudLayoutScreen.class.getDeclaredField("preview");
                    f.setAccessible(true);
                    f.setInt(layout, pv[1]);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }, 30, false));
        }
        steps.add(new Step("09-settings", () -> {
            cmd(mc, "gamemode survival");
            mc.setScreen(new FordixSettingsScreen(null));
        }, 30, false));
        steps.add(new Step("10-hud-layout", () -> {
            HudLayoutScreen layout = new HudLayoutScreen(null);
            mc.setScreen(layout);
            // Grab the hunger bar and drag it up and to the left, toward the center line.
            double grabX = layout.width / 2.0 + 50;
            double grabY = layout.height - 35;
            layout.mouseClicked(grabX, grabY, 0);
            layout.mouseDragged(grabX - 30, grabY - 30, 0, -30, -30);
        }, 30, false));
        steps.add(new Step("10b-hud-extras", () -> {
            // Chat, hotbar and the other overlays are listed too: send the chat to the top right, the effects to the left.
            HudLayoutScreen layout = new HudLayoutScreen(null);
            mc.setScreen(layout);
            layoutForExtras = layout;
        }, 30, false));
        steps.add(new Step("10c-hud-extras-moved", () -> {
            HudLayoutScreen layout = layoutForExtras;
            com.omneria.fordix.Fordix.LOGGER.info("HUD layout lists {}", layout.listed());
            layout.nudge("minecraft:chat_panel", layout.width - 330, -(layout.height - 160));
            layout.nudge("minecraft:hotbar", 0, -60);
            com.omneria.fordix.client.gui.vanilla.HudSlots.setScale("minecraft:hotbar", 1.5F);
            com.omneria.fordix.client.gui.vanilla.HudSlots.setScale("minecraft:chat_panel", 0.7F);
            com.omneria.fordix.client.gui.vanilla.HudSlots.setScale("health", 1.6F);
            com.omneria.fordix.client.gui.vanilla.HudSlots.setScale("experience", 1.5F);
            com.omneria.fordix.client.gui.vanilla.HudSlots.setScale("boss", 1.4F);
        }, 30, false));
        steps.add(new Step("10d-hud-extras-live", () -> mc.setScreen(null), 30, false));
        steps.add(new Step("10d2-chat-input-moved", () -> mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen("bonjour")), 20, false));
        steps.add(new Step("10e-hud-extras-reset", () -> {
            mc.setScreen(null);
            for (String id : layoutForExtras.listed()) {
                com.omneria.fordix.client.gui.vanilla.HudSlots.reset(id);
            }
            com.omneria.fordix.client.gui.vanilla.ExtraHud.save();
            com.omneria.fordix.FordixConfig.hudOffsetX("health").save();
        }, 10, false));
        steps.add(new Step("10j-hud-held-item", () -> {
            // Switching the held item shows its name line with the turning 3D model beside it.
            mc.player.getInventory().selected = 3;
        }, 8, false));
        steps.add(new Step("12c-chat-closed", () -> {
            cmd(mc, "say Redemarrage dans 10 minutes");
            cmd(mc, "tellraw @a {\"translate\":\"chat.type.text\",\"with\":[\"Ana\",\"on se retrouve au spawn ? Dev tu as le kit ?\"]}");
            cmd(mc, "tellraw @a {\"translate\":\"chat.type.text\",\"with\":[\"Kiro\",\"ok, j'arrive\"]}");
            cmd(mc, "gamemode creative");
            mc.player.connection.sendChat("salut tout le monde");
        }, 25, false));
        steps.add(new Step("12d-chat-open", () -> mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen("/tp An")), 25, false));
        steps.add(new Step("12d2-chat-send", () -> {
            // Type and send a line the way a player does: Enter in the chat screen.
            if (mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen chat) {
                chat.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0);
            }
        }, 20, false));
        steps.add(new Step("12d3-chat-reopen", () -> mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen("coucou")), 15, false));
        steps.add(new Step("12d4-chat-send2", () -> {
            if (mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen chat) {
                chat.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0);
            }
        }, 20, false));
        steps.add(new Step("12d5-chat-open-again", () -> mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen("")), 15, false));
        steps.add(new Step("12e-chat-tab-server", () ->
                com.omneria.fordix.client.chat.ChatPanel.selectTab(com.omneria.fordix.client.chat.ChatTab.SERVER), 15, false));
        steps.add(new Step("12f-chat-tab-reset", () -> {
            com.omneria.fordix.client.chat.ChatPanel.selectTab(com.omneria.fordix.client.chat.ChatTab.ALL);
            mc.setScreen(null);
        }, 10, false));
        steps.add(new Step("12h-info-hud", () -> mc.player.setXRot(55F), 30, false));
        steps.add(new Step("12h2-info-hud-layout", () -> {
            HudLayoutScreen layout = new HudLayoutScreen(null);
            mc.setScreen(layout);
            layoutForExtras = layout;
        }, 30, false));
        steps.add(new Step("12h3-info-hud-moved", () -> {
            com.omneria.fordix.Fordix.LOGGER.info("HUD layout lists {}", layoutForExtras.listed());
            layoutForExtras.nudge("fordix:coordinates", 300, 120);
            com.omneria.fordix.client.gui.vanilla.HudSlots.setScale("fordix:target_block", 1.5F);
        }, 20, false));
        steps.add(new Step("12h4-info-hud-live", () -> mc.setScreen(null), 20, false));
        steps.add(new Step("12h5-info-hud-reset", () -> {
            com.omneria.fordix.client.gui.vanilla.HudSlots.reset("fordix:coordinates");
            com.omneria.fordix.client.gui.vanilla.HudSlots.reset("fordix:target_block");
            com.omneria.fordix.client.gui.vanilla.ExtraHud.save();
            mc.player.setXRot(0F);
        }, 5, false));
        steps.add(new Step("12i-hud-gear", () -> {
            // Survival, a worn pickaxe and a bow with some arrows in the bag: the gear panel shows both rows.
            cmd(mc, "gamemode survival");
            cmd(mc, "item replace entity @s hotbar.3 with minecraft:iron_pickaxe{Damage:180}");
            cmd(mc, "item replace entity @s hotbar.4 with minecraft:bow{Damage:60}");
            mc.player.getInventory().setItem(9, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 23));
            cmd(mc, "item replace entity @s weapon.offhand with minecraft:shield{Damage:200}");
            mc.player.getInventory().selected = 4;
        }, 30, false));
        steps.add(new Step("12i2-hud-clock-compass", () -> {
            cmd(mc, "time set 15000");
            cmd(mc, "weather rain");
            cmd(mc, "tp @s ~25 ~ ~-30");
            mc.player.getInventory().selected = 3;
        }, 40, false));
        steps.add(new Step("12i3-hud-target-entity", () -> {
            cmd(mc, "weather clear");
            cmd(mc, "execute at @s run summon minecraft:cow ^ ^ ^2.5 {Health:6f}");
            mc.player.setXRot(22F);
        }, 40, false));
        steps.add(new Step("12i3b-hud-target-far", () -> {
            // A hostile creature twelve blocks away, in the line of sight: its panel and the sheet key already work.
            cmd(mc, "kill @e[type=minecraft:cow]");
            cmd(mc, "execute at @s run summon minecraft:villager ~ ~ ~12 {NoAI:1b}");
            mc.player.setXRot(0F);
        }, 40, false));
        steps.add(new Step("12i3c-hud-target-block-long", () -> {
            cmd(mc, "kill @e[type=minecraft:villager]");
            cmd(mc, "execute at @s run setblock ~ ~ ~3 minecraft:polished_blackstone_brick_stairs");
            mc.player.setXRot(28F);
        }, 40, false));
        steps.add(new Step("12i3d-block-left-stairs", () -> {
            // The block panel moved to the left edge: a long name must not push it out of the screen.
            com.omneria.fordix.client.gui.vanilla.HudSlots.moveTo("fordix:target_block", 4, 80,
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }, 30, false));
        steps.add(new Step("12i3e-block-left-stone", () -> {
            cmd(mc, "execute at @s run setblock ~ ~ ~3 minecraft:stone");
        }, 30, false));
        steps.add(new Step("12i3f-block-left-reset", () ->
                com.omneria.fordix.client.gui.vanilla.HudSlots.reset("fordix:target_block"), 5, false));
        steps.add(new Step("12i4-hud-lines-off", () -> {
            com.omneria.fordix.FordixConfig.hudLineValue("coordinates_biome").set(false);
            com.omneria.fordix.FordixConfig.hudLineValue("performance_ping").set(false);
            com.omneria.fordix.FordixConfig.hudLineValue("clock_weather").set(false);
            mc.player.setXRot(55F);
        }, 25, false));
        steps.add(new Step("12i5-hud-lines-screen", () -> {
            for (String line : com.omneria.fordix.FordixConfig.HUD_LINES) {
                com.omneria.fordix.FordixConfig.hudLineValue(line).set(true);
            }
            mc.setScreen(new com.omneria.fordix.client.gui.HudLinesScreen(null));
        }, 15, false));
        steps.add(new Step("12i7-hud-alerts-screen", () -> mc.setScreen(new com.omneria.fordix.client.gui.HudAlertsScreen(null)), 15, false));
        steps.add(new Step("12j-chat-private", () -> {
            mc.setScreen(null);
            cmd(mc, "tellraw @a {\"translate\":\"commands.message.display.incoming\",\"with\":[\"Ana\",\"Salut Dev, tu viens au spawn ?\"]}");
            cmd(mc, "tellraw @a {\"translate\":\"chat.type.text\",\"with\":[\"Bob\",\"quelqu'un a du fer ?\"]}");
        }, 30, false));
        steps.add(new Step("12j2-chat-private-tab", () -> {
            mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));
            com.omneria.fordix.client.chat.ChatPanel.selectTab(com.omneria.fordix.client.chat.ChatTab.PRIVATE);
        }, 20, false));
        steps.add(new Step("12j3-chat-search", () -> {
            com.omneria.fordix.client.chat.ChatPanel.selectTab(com.omneria.fordix.client.chat.ChatTab.ALL);
            com.omneria.fordix.client.chat.ChatPanel.setSearch("fer");
        }, 20, false));
        steps.add(new Step("12k-hud-profile-layout", () -> {
            com.omneria.fordix.client.chat.ChatPanel.setSearch("");
            mc.setScreen(null);
            com.omneria.fordix.client.gui.vanilla.HudSlots.setScale("fordix:coordinates", 1.4F);
            com.omneria.fordix.client.gui.vanilla.HudSlots.reset("fordix:coordinates");
            mc.setScreen(new HudLayoutScreen(null));
        }, 25, false));
        steps.add(new Step("12k3-hud-profile-reset", () -> {
            com.omneria.fordix.client.gui.vanilla.HudSlots.reset("fordix:coordinates");
            com.omneria.fordix.client.gui.vanilla.ExtraHud.save();
            cmd(mc, "gamemode creative");
            cmd(mc, "kill @e[type=minecraft:cow]");
            mc.player.setXRot(0F);
        }, 8, false));
        steps.add(new Step("12m-hud-effects-armor", () -> {
            cmd(mc, "gamemode survival");
            cmd(mc, "effect give @s minecraft:speed 600 1");
            cmd(mc, "effect give @s minecraft:poison 8");
            cmd(mc, "effect give @s minecraft:night_vision 300");
            wear(mc, 3, net.minecraft.world.item.Items.DIAMOND_HELMET, 200);
            wear(mc, 2, net.minecraft.world.item.Items.NETHERITE_CHESTPLATE, 250);
            wear(mc, 0, net.minecraft.world.item.Items.IRON_BOOTS, 150);
            mc.player.getInventory().selected = 3;
            mc.player.setXRot(0F);
        }, 40, false));
        steps.add(new Step("12m2-hud-alert", () -> {
            wear(mc, 3, net.minecraft.world.item.Items.DIAMOND_HELMET, 360);
        }, 20, false));
        steps.add(new Step("12m3-hud-container", () -> {
            cmd(mc, "effect clear @s");
            cmd(mc, "execute at @s run setblock ^ ^ ^2 minecraft:chest");
            mc.player.setXRot(29F);
        }, 25, false));
        steps.add(new Step("12m4-hud-container-seen", () -> {
            if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
                com.omneria.fordix.client.hud.ContainerMemory.put(mc, hit.getBlockPos(), mc.level.getBlockState(hit.getBlockPos()), java.util.List.of(
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 12),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 7),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 20)));
            }
        }, 15, false));
        steps.add(new Step("12m5-hud-stretch", () -> {
            cmd(mc, "setblock ~ ~ ~ minecraft:air");
            mc.player.setXRot(0F);
            com.omneria.fordix.client.gui.vanilla.HudSlots.setScales("fordix:coordinates", 1.4F, 0.8F);
            com.omneria.fordix.client.gui.vanilla.HudSlots.setScales("fordix:performance", 0.8F, 1.0F);
            HudLayoutScreen layout = new HudLayoutScreen(null);
            mc.setScreen(layout);
        }, 25, false));
        steps.add(new Step("12m6-chat-coords", () -> {
            com.omneria.fordix.client.gui.vanilla.HudSlots.reset("fordix:coordinates");
            com.omneria.fordix.client.gui.vanilla.HudSlots.reset("fordix:performance");
            com.omneria.fordix.client.gui.vanilla.ExtraHud.save();
            mc.setScreen(null);
            cmd(mc, "tellraw @a {\"translate\":\"chat.type.text\",\"with\":[\"Ana\",\"retrouve-moi a 120 64 -300 pour le portail\"]}");
        }, 25, false));
        steps.add(new Step("12m7-chat-coords-open", () -> mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen("")), 20, false));
        steps.add(new Step("12m8-settings-hud", () -> mc.setScreen(new FordixSettingsScreen(null, 0)), 15, false));
        steps.add(new Step("12m9b-settings-game", () -> mc.setScreen(new FordixSettingsScreen(null, 1)), 15, false));
        steps.add(new Step("12m9c-settings-admin", () -> mc.setScreen(new FordixSettingsScreen(null, 2)), 15, false));
        steps.add(new Step("12m9-reset", () -> {
            mc.setScreen(null);
            for (int piece = 0; piece < 4; piece++) {
                mc.player.getInventory().armor.set(piece, net.minecraft.world.item.ItemStack.EMPTY);
            }
            cmd(mc, "gamemode creative");
        }, 8, false));
        steps.add(new Step("12n-tab-list", () -> {
            com.omneria.fordix.client.hud.PlayerListHud.testForce = true;
            mc.setScreen(null);
        }, 20, false));
        steps.add(new Step("12n2-damage-numbers", () -> {
            com.omneria.fordix.client.hud.PlayerListHud.testForce = false;
            cmd(mc, "execute at @s run summon minecraft:zombie ^ ^ ^3 {NoAI:1b,Health:30f,Silent:1b}");
            mc.player.setXRot(10F);
        }, 30, false));
        steps.add(new Step("12n3-damage-hit", () -> {
            cmd(mc, "damage @e[type=minecraft:zombie,limit=1,sort=nearest] 6 minecraft:generic");
            cmd(mc, "damage @s 3 minecraft:generic");
        }, 6, false));
        steps.add(new Step("12n4-damage-big", () -> {
            cmd(mc, "damage @e[type=minecraft:zombie,limit=1,sort=nearest] 12 minecraft:generic");
        }, 5, false));
        steps.add(new Step("12o-inventory-tools", () -> {
            cmd(mc, "kill @e[type=minecraft:villager]");
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        }, 20, false));
        steps.add(new Step("12o2-inventory-search", () -> com.omneria.fordix.client.gui.vanilla.InventoryTools.setSearch("iron"), 15, false));
        steps.add(new Step("12o3-inventory-closed", () -> {
            com.omneria.fordix.client.gui.vanilla.InventoryTools.setSearch("");
            mc.setScreen(null);
        }, 5, false));
        steps.add(new Step("12p-wiki-import", () -> cmd(mc, "fordix wiki importlinks minecraft https://minecraft.wiki/w/Iron_Sword"), 700, false));
        steps.add(new Step("12p2-wiki-page", () -> mc.setScreen(new com.omneria.fordix.client.gui.WikiScreen(null, "minecraft", "Iron_Sword")), 80, false));
        steps.add(new Step("12p2b-wiki-import-more", () -> {
            cmd(mc, "fordix wiki import minecraft Creeper");
            cmd(mc, "fordix wiki import minecraft Stone");
        }, 500, false));
        steps.add(new Step("12p2c-wiki-creeper", () -> mc.setScreen(new com.omneria.fordix.client.gui.WikiScreen(null, "minecraft", "Creeper")), 90, false));
        steps.add(new Step("12p2d-wiki-folded", () -> {
            com.omneria.fordix.client.gui.WikiScreen wiki = (com.omneria.fordix.client.gui.WikiScreen) mc.screen;
            wiki.testFavorite();
            wiki.testFold(0);
            wiki.testFold(2);
            wiki.testScroll(470);
        }, 20, false));
        steps.add(new Step("12p2e-wiki-category", () -> {
            com.omneria.fordix.client.gui.WikiScreen wiki = (com.omneria.fordix.client.gui.WikiScreen) mc.screen;
            wiki.testCategory("mob");
            wiki.testHome();
        }, 20, false));
        steps.add(new Step("12p3-wiki-search", () -> {
            com.omneria.fordix.client.gui.WikiScreen.nextQuery = "iron";
            mc.setScreen(new com.omneria.fordix.client.gui.WikiScreen(null, "minecraft", null));
        }, 60, false));
        steps.add(new Step("12p4-wiki-closed", () -> mc.setScreen(null), 5, false));
        steps.add(new Step("12q-borderless-on", () -> com.omneria.fordix.client.BorderlessFullscreen.toggle(mc), 30, false));
        steps.add(new Step("12q2-borderless-off", () -> com.omneria.fordix.client.BorderlessFullscreen.toggle(mc), 30, false));
        if (net.minecraftforge.fml.ModList.get().isLoaded("journeymap")) {
            steps.add(new Step("10f-hud-journeymap", () -> {
                HudLayoutScreen layout = new HudLayoutScreen(null);
                mc.setScreen(layout);
                layoutForExtras = layout;
            }, 40, false));
            steps.add(new Step("10g-hud-journeymap-moved", () -> {
                HudLayoutScreen layout = layoutForExtras;
                com.omneria.fordix.Fordix.LOGGER.info("HUD layout lists {}", layout.listed());
                com.omneria.fordix.Fordix.LOGGER.info("JM rect before {}",
                        com.omneria.fordix.client.gui.vanilla.JourneyMapHud.rect(layout.width, layout.height));
                layout.nudge("journeymap", -120, 120);
                com.omneria.fordix.client.gui.vanilla.HudSlots.setScale("journeymap", 1.5F);
                com.omneria.fordix.Fordix.LOGGER.info("JM rect after {}",
                        com.omneria.fordix.client.gui.vanilla.JourneyMapHud.rect(layout.width, layout.height));
            }, 40, false));
            steps.add(new Step("10h-hud-journeymap-live", () -> mc.setScreen(null), 40, false));
            steps.add(new Step("10i-hud-journeymap-reset", () ->
                    com.omneria.fordix.client.gui.vanilla.HudSlots.reset("journeymap"), 20, false));
        }
        steps.add(new Step("11-admin", () -> {
            for (String element : com.omneria.fordix.FordixConfig.HUD_ELEMENTS) {
                com.omneria.fordix.FordixConfig.hudOffsetX(element).set(0);
                com.omneria.fordix.FordixConfig.hudOffsetY(element).set(0);
            }
            mc.setScreen(new FordixAdminScreen(null));
        }, 50, false));
        steps.add(new Step("11b-admin-schedule", () -> {
            // The server is this very game: seed a schedule, a template and a panel through the real protocol.
            FordixAdminScreen admin = new FordixAdminScreen(null);
            mc.setScreen(admin);
            var channel = com.omneria.fordix.panel.network.PanelNetwork.CHANNEL;
            channel.sendToServer(new com.omneria.fordix.admin.ServerboundAdminPayload("schedule_add",
                    java.util.List.of("warning", "8", "Redemarrage du serveur", "Dans 10 minutes", "tous", "", "once", "10")));
            channel.sendToServer(new com.omneria.fordix.admin.ServerboundAdminPayload("schedule_add",
                    java.util.List.of("info", "5", "Pensez a voter !", "", "tous", "", "every", "120")));
            channel.sendToServer(new com.omneria.fordix.admin.ServerboundAdminPayload("template_save",
                    java.util.List.of("Annonce evenement", "success", "15", "Evenement ce soir", "20h sur le serveur")));
            channel.sendToServer(new com.omneria.fordix.admin.ServerboundAdminPayload("save_panel",
                    java.util.List.of("demo", "Menu du spawn", "Bienvenue", "Retour spawn\t/spawn", "Kit de depart\t/kit start", "Regles\t/rules")));
            admin.switchTab(1);
        }, 60, false));
        steps.add(new Step("11c-admin-templates", () -> ((FordixAdminScreen) mc.screen).switchTab(2), 30, false));
        steps.add(new Step("11d-admin-panels", () -> {
            FordixAdminScreen admin = (FordixAdminScreen) mc.screen;
            admin.switchTab(3);
            com.omneria.fordix.panel.network.PanelNetwork.CHANNEL.sendToServer(
                    new com.omneria.fordix.admin.ServerboundAdminPayload("panels", java.util.List.of()));
            admin.selectPanel("demo");
        }, 40, false));
        steps.add(new Step("11e-admin-pages", () -> {
            FordixAdminScreen admin = new FordixAdminScreen(null);
            mc.setScreen(admin);
            var channel = com.omneria.fordix.panel.network.PanelNetwork.CHANNEL;
            channel.sendToServer(new com.omneria.fordix.admin.ServerboundAdminPayload("page_save", java.util.List.of("regles", "Regles du serveur",
                    "# Respect\nPas d'insultes ni de harcelement. Les conflits se reglent avec un moderateur.\n\n# Pillage\nAutorise en zone de guerre uniquement, les coffres sont proteges ailleurs.\n\n# Commandes utiles\n@button Retour au spawn | command:/spawn\n@button Wiki du serveur | url:https://wiki.exemple.fr")));
            channel.sendToServer(new com.omneria.fordix.admin.ServerboundAdminPayload("page_save", java.util.List.of("demarrage", "Bien demarrer",
                    "# Premiers pas\nRejoins une equipe puis pose ton foyer.")));
            admin.switchTab(4);
        }, 50, false));
        steps.add(new Step("11f-admin-page-edit", () -> ((FordixAdminScreen) mc.screen).pickPage("regles"), 40, false));
        steps.add(new Step("11f2-admin-book", () -> {
            FordixAdminScreen admin = (FordixAdminScreen) mc.screen;
            admin.pickStep(1);
        }, 40, false));
        steps.add(new Step("11g-guides-screen", () -> {
            GuidesScreen guides = new GuidesScreen(null);
            mc.setScreen(guides);
            guides.selectRow(Integer.MAX_VALUE - 1);
            guides.selectRow(2);
        }, 30, false));
        steps.add(new Step("11h-guides-mod", () -> ((GuidesScreen) mc.screen).selectRow(0), 25, false));
        steps.add(new Step("11i0-new-page", () -> {
            mc.setScreen(null);
            com.omneria.fordix.panel.network.PanelNetwork.CHANNEL.sendToServer(new com.omneria.fordix.admin.ServerboundAdminPayload("page_save",
                    java.util.List.of("evenement", "Evenement du week-end", "# Evenement\nDouble xp ce week-end.")));
        }, 40, false));
        steps.add(new Step("11i-pause-guides", () -> mc.pauseGame(false), 30, false));
        steps.add(new Step("11j-welcome-book-give", () -> {
            mc.setScreen(null);
            cmd(mc, "clear @s");
            cmd(mc, "advancement grant @s only minecraft:story/mine_stone");
            cmd(mc, "give @s fordix:welcome_book");
        }, 40, false));
        steps.add(new Step("11k-welcome-book-open", () -> {
            mc.player.getInventory().selected = 0;
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        }, 40, false));
        steps.add(new Step("12-chat", () -> mc.setScreen(new ChatScreen("/fordix notify")), 30, false));
        steps.add(new Step("13-pause", () -> mc.setScreen(null), 10, false));
        steps.add(new Step("14-pause-menu", () -> mc.pauseGame(false), 30, false));
        steps.add(new Step("15-air", () -> {
            cmd(mc, "execute at @s run fill ~-1 ~ ~-1 ~1 ~3 ~1 minecraft:water");
            mc.setScreen(null);
        }, 90, false));
        steps.add(new Step("16-mount", () -> {
            cmd(mc, "execute at @s run fill ~-1 ~ ~-1 ~1 ~3 ~1 minecraft:air");
            cmd(mc, "summon minecraft:horse ~ ~ ~ {Tame:1b,SaddleItem:{id:\"minecraft:saddle\",Count:1b}}");
        }, 40, false));
        steps.add(new Step("16b-mount-ride", () -> cmd(mc, "ride @s mount @e[type=minecraft:horse,limit=1,sort=nearest]"), 40, false));
        steps.add(new Step("17-rarity", () -> {
            cmd(mc, "ride @s dismount");
            cmd(mc, "clear @s");
            cmd(mc, "give @s minecraft:dragon_head");
            cmd(mc, "give @s minecraft:netherite_sword{Enchantments:[{id:\"minecraft:sharpness\",lvl:7}]}");
            cmd(mc, "give @s minecraft:iron_chestplate");
            cmd(mc, "give @s minecraft:stone_pickaxe");
            cmd(mc, "give @s minecraft:golden_helmet");
            cmd(mc, "give @s minecraft:emerald_block");
            cmd(mc, "give @s minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:protection\",lvl:4},{id:\"minecraft:silk_touch\",lvl:1}]}");
            mc.setScreen(new InventoryScreen(mc.player));
        }, 40, false));
        // Items of Twilight Forest and Create (only there when those mods sit in run/mods; /give just fails otherwise).
        steps.add(new Step("17b-modded", () -> {
            cmd(mc, "clear @s");
            cmd(mc, "give @s twilightforest:giant_pickaxe");
            cmd(mc, "give @s twilightforest:naga_trophy");
            cmd(mc, "give @s twilightforest:fiery_ingot");
            cmd(mc, "give @s twilightforest:steeleaf_ingot");
            cmd(mc, "give @s twilightforest:ironwood_ingot");
            cmd(mc, "give @s twilightforest:naga_banner_pattern");
            cmd(mc, "give @s twilightforest:knightmetal_chestplate{Enchantments:[{id:\"twilightforest:chill_aura\",lvl:2},{id:\"twilightforest:fire_react\",lvl:1}]}");
            cmd(mc, "give @s create:cogwheel 16");
            cmd(mc, "give @s create:copper_backtank{Enchantments:[{id:\"create:capacity\",lvl:2}]}");
            cmd(mc, "give @s create:potato_cannon{Enchantments:[{id:\"create:potato_recovery\",lvl:3}]}");
            mc.setScreen(new InventoryScreen(mc.player));
        }, 40, true));
        steps.add(new Step("17b2-modded-armor", () -> hoverIndex = 6, 25, true));
        steps.add(new Step("17b3-modded-backtank", () -> hoverIndex = 8, 25, true));
        steps.add(new Step("17b3b-modded-cogwheel", () -> hoverIndex = 7, 25, true));
        steps.add(new Step("17b4-modded-trophy", () -> hoverIndex = 1, 25, true));
        steps.add(new Step("17b5-modded-ponder", () -> {
            try {
                var all = (java.util.Map<?, ?>) Class.forName("com.simibubi.create.foundation.ponder.PonderRegistry").getField("ALL").get(null);
                Fordix.LOGGER.info("[devtest] ponder registry: {} items, backtank={}, cogwheel={}", all.size(),
                        all.containsKey(new net.minecraft.resources.ResourceLocation("create:copper_backtank")),
                        all.containsKey(new net.minecraft.resources.ResourceLocation("create:cogwheel")));
            } catch (ReflectiveOperationException e) {
                Fordix.LOGGER.info("[devtest] ponder registry unreadable: {}", e.toString());
            }
            if (!com.omneria.fordix.client.guide.PonderScenes.open(new net.minecraft.world.item.ItemStack(
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("create:cogwheel"))))) {
                Fordix.LOGGER.info("[devtest] no Ponder scene (Create not installed?)");
            }
        }, 80, false));
        steps.add(new Step("17c-modded-advancements", () -> {
            hoverIndex = 0;
            mc.setScreen(new AdvancementsScreen(mc.player.connection.getAdvancements()));
        }, 40, false));
        steps.add(new Step("17c2-advancement-card", () -> {
            com.omneria.fordix.client.gui.FordixAdvancementsScreen screen = new com.omneria.fordix.client.gui.FordixAdvancementsScreen(
                    mc.player.connection.getAdvancements());
            screen.setTestHover(3);
            mc.setScreen(screen);
        }, 40, false));
        steps.add(new Step("17d-item-sheet", () -> mc.setScreen(new com.omneria.fordix.client.gui.ItemSheetScreen(null,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE))), 40, false));
        steps.add(new Step("17e-item-sheet-stick", () -> mc.setScreen(new com.omneria.fordix.client.gui.ItemSheetScreen(null,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK))), 40, false));
        steps.add(new Step("17f-item-sheet-furnace", () -> mc.setScreen(new com.omneria.fordix.client.gui.ItemSheetScreen(null,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT))), 40, false));
        steps.add(new Step("17g-entity-zombie", () -> mc.setScreen(new com.omneria.fordix.client.gui.EntitySheetScreen(null,
                net.minecraft.world.entity.EntityType.ZOMBIE)), 60, false));
        steps.add(new Step("17h-entity-dragon", () -> mc.setScreen(new com.omneria.fordix.client.gui.EntitySheetScreen(null,
                net.minecraft.world.entity.EntityType.ENDER_DRAGON)), 60, false));
        steps.add(new Step("17i-item-sheet-tall", () -> mc.setScreen(new com.omneria.fordix.client.gui.ItemSheetScreen(null,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD))), 40, false));
        steps.add(new Step("17j-pet-wheel", () -> {
            net.minecraft.world.entity.animal.Wolf wolf = new net.minecraft.world.entity.animal.Wolf(net.minecraft.world.entity.EntityType.WOLF, mc.level);
            wolf.setTame(true);
            wolf.setOwnerUUID(mc.player.getUUID());
            wolf.setCustomName(net.minecraft.network.chat.Component.literal("Rex"));
            wolf.setHealth(14);
            wolf.setOrderedToSit(true);
            wolf.setPos(mc.player.position());
            com.omneria.fordix.client.pet.PetWheelScreen.testKeepOpen = true;
            mc.setScreen(new com.omneria.fordix.client.pet.PetWheelScreen(wolf));
        }, 40, false));
        steps.add(new Step("17k-pet-wheel-moved", () -> {
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();
            com.omneria.fordix.client.gui.vanilla.HudSlots.setScale(com.omneria.fordix.client.pet.PetWheelScreen.ID, 0.7F);
            com.omneria.fordix.client.gui.vanilla.HudSlots.moveTo(com.omneria.fordix.client.pet.PetWheelScreen.ID, 20, 40, w, h);
            net.minecraft.world.entity.animal.Wolf wolf = new net.minecraft.world.entity.animal.Wolf(net.minecraft.world.entity.EntityType.WOLF, mc.level);
            wolf.setTame(true);
            wolf.setOwnerUUID(mc.player.getUUID());
            wolf.setPos(mc.player.position());
            mc.setScreen(new com.omneria.fordix.client.pet.PetWheelScreen(wolf));
        }, 30, false));
        steps.add(new Step("17l-pet-wheel-reset", () -> {
            com.omneria.fordix.client.gui.vanilla.HudSlots.reset(com.omneria.fordix.client.pet.PetWheelScreen.ID);
            com.omneria.fordix.client.pet.PetWheelScreen.testKeepOpen = false;
            mc.setScreen(null);
        }, 5, false));
        steps.add(new Step("12g-chat-error", () -> mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen("/fordix wiki improt minecraft")), 30, false));
        steps.add(new Step("12h-chat-history-open", () -> {
            mc.gui.getChat().addRecentChat("/fordix notify all Redemarrage dans 5 minutes");
            mc.gui.getChat().addRecentChat("/fordix panel list");
            mc.gui.getChat().addRecentChat("/fordix wiki import minecraft https://fr.minecraft.wiki");
            mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));
        }, 15, false));
        steps.add(new Step("12i-chat-history", () -> {
            if (mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen chat) {
                chat.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_UP, 0, 0);
            }
        }, 20, false));
        steps.add(new Step("12j-chat-close", () -> mc.setScreen(null), 5, false));
        steps.add(new Step("18-options", () -> mc.setScreen(new net.minecraft.client.gui.screens.OptionsScreen(null, mc.options)), 30, false));
        steps.add(new Step("19-video", () -> mc.setScreen(new net.minecraft.client.gui.screens.VideoSettingsScreen(optionsParent(mc), mc.options)), 30, false));
        steps.add(new Step("19a-video-quality", () -> mc.setScreen(new com.omneria.fordix.client.gui.options.VideoScreen(optionsParent(mc), 1)), 30, false));
        steps.add(new Step("19a2-video-interface", () -> mc.setScreen(new com.omneria.fordix.client.gui.options.VideoScreen(optionsParent(mc), 2)), 30, false));
        steps.add(new Step("19a3-video-open", () -> mc.setScreen(new com.omneria.fordix.client.gui.options.VideoScreen(optionsParent(mc))), 10, false));
        steps.add(new Step("19a4-video-tab-clicks", () -> clickEveryTab(mc), 20, false));
        steps.add(new Step("19a5-sounds-tab-clicks", () -> {
            mc.setScreen(new com.omneria.fordix.client.gui.options.SoundScreen(optionsParent(mc)));
            clickEveryTab(mc);
        }, 15, false));
        steps.add(new Step("19b-language", () -> mc.setScreen(new net.minecraft.client.gui.screens.LanguageSelectScreen(null, mc.options, mc.getLanguageManager())), 30, false));
        steps.add(new Step("19c-accessibility", () -> mc.setScreen(new net.minecraft.client.gui.screens.AccessibilityOptionsScreen(null, mc.options)), 30, false));
        steps.add(new Step("19d-chat-options", () -> mc.setScreen(new net.minecraft.client.gui.screens.ChatOptionsScreen(null, mc.options)), 30, false));
        steps.add(new Step("19e-mouse", () -> mc.setScreen(new net.minecraft.client.gui.screens.MouseSettingsScreen(null, mc.options)), 30, false));
        steps.add(new Step("19f-skin", () -> mc.setScreen(new net.minecraft.client.gui.screens.SkinCustomizationScreen(null, mc.options)), 30, false));
        steps.add(new Step("19g-keybinds", () -> mc.setScreen(new net.minecraft.client.gui.screens.controls.KeyBindsScreen(null, mc.options)), 30, false));
        steps.add(new Step("20-controls", () -> mc.setScreen(new net.minecraft.client.gui.screens.controls.ControlsScreen(optionsParent(mc), mc.options)), 30, false));
        steps.add(new Step("21-singleplayer", () -> mc.setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(null)), 40, false));
        steps.add(new Step("22-stats", () -> mc.setScreen(new net.minecraft.client.gui.screens.achievement.StatsScreen(null, mc.player.getStats())), 30, false));
        steps.add(new Step("23-multiplayer", () -> mc.setScreen(new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(null)), 30, false));
        steps.add(new Step("24-sounds", () -> mc.setScreen(new net.minecraft.client.gui.screens.SoundOptionsScreen(optionsParent(mc), mc.options)), 30, false));
        steps.add(new Step("25-death", () -> mc.setScreen(new net.minecraft.client.gui.screens.DeathScreen(net.minecraft.network.chat.Component.literal("Tu es tombe de haut"), false)), 30, false));
        steps.add(new Step("26-title", () -> mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen()), 40, false));
        steps.add(new Step("26a-title-fields", () -> {
            // The game's title is read by field layout: a game version that changes it must fail here, not bring the vanilla title back.
            if (!com.omneria.fordix.client.hud.TitleHud.selfCheck()) {
                throw new IllegalStateException("TitleHud: the game's title fields were not found (the class layout changed)");
            }
            mc.setScreen(null);
        }, 5, false));
        steps.add(new Step("26b-subtitles", () -> {
            mc.options.showSubtitles().set(true);
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.CHEST_OPEN, 1.0F));
        }, 25, false));
        steps.add(new Step("26c-subtitles-off", () -> mc.options.showSubtitles().set(false), 3, false));
        steps.add(new Step("26d-debug-text", () -> mc.options.renderDebug = true, 25, false));
        steps.add(new Step("26d2-debug-chunks", () -> com.omneria.fordix.client.hud.DebugHud.setChunkBorders(mc, true), 25, false));
        steps.add(new Step("26e-debug-text-off", () -> {
            mc.options.renderDebug = false;
            com.omneria.fordix.client.hud.DebugHud.setChunkBorders(mc, false);
            if (!com.omneria.fordix.client.hud.DebugHud.takenOver()) {
                throw new IllegalStateException("F3 was not drawn by Fordix (see the warnings in the log)");
            }
            var info = com.omneria.fordix.client.hud.DebugInfoClient.data();
            if (info == null || !info.enabled() || info.ores().isEmpty() || info.friendly().isEmpty()) {
                throw new IllegalStateException("The server's answer for F3 is missing or empty: " + info);
            }
            Fordix.LOGGER.info("[devtest] F3 server data: biome {}, {} ores, {} friendly, {} hostile, slimes known {}",
                    info.biome(), info.ores().size(), info.friendly().size(), info.hostile().size(), info.slimeKnown());
        }, 3, false));
        steps.add(new Step("27a-console", () -> mc.setScreen(new com.omneria.fordix.client.console.ConsoleScreen(sampleConsole())), 20, false));
        steps.add(new Step("27b-console-closed", () -> mc.setScreen(null), 3, false));
        steps.add(new Step("27-create-world", () -> net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.openFresh(mc, null), 40, false));
        steps.add(new Step("99-done", () -> {
            Fordix.LOGGER.info("[devtest] finished, shots in {}", outDir);
            mc.stop();
        }, 5, false));
        // FORDIX_DEVTEST_ONLY="14-pause-menu,18-options": only those screens (and the set-up), for size sweeps.
        String only = System.getenv("FORDIX_DEVTEST_ONLY");
        if (only != null && !only.isBlank()) {
            java.util.Set<String> keep = new java.util.HashSet<>(java.util.Arrays.asList(only.split(",")));
            steps.removeIf(step -> !keep.contains(step.name()) && !step.name().equals("00-setup")
                    && !step.name().equals("01-hud") && !step.name().equals("99-done"));
            // Long enough for the toasts of the set-up (recipes, advancements) to leave the screen.
            steps.replaceAll(step -> step.name().equals("01-hud") ? new Step("01-hud", step.setup(), 700, false) : step);
        }
    }

    /** Clicks every tab of the options page on screen, the way a player does (the click rebuilds the page), then goes back to the first one. */
    private static void clickEveryTab(Minecraft mc) {
        if (mc.screen instanceof com.omneria.fordix.client.gui.options.OptionsPageScreen page) {
            for (int i = 2; i >= 0; i--) {
                int[] point = page.tabPoint(i);
                if (point != null) {
                    page.mouseClicked(point[0], point[1], 0);
                    page.mouseReleased(point[0], point[1], 0);
                }
            }
            int[] first = page.tabPoint(0);
            if (first != null) {
                page.mouseClicked(first[0], first[1], 0);
                page.mouseReleased(first[0], first[1], 0);
            }
        }
    }

    /** An options screen that has been laid out once: the sub-screens read the game from their parent. */
    private static net.minecraft.client.gui.screens.Screen optionsParent(Minecraft mc) {
        net.minecraft.client.gui.screens.Screen parent = new net.minecraft.client.gui.screens.OptionsScreen(null, mc.options);
        parent.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        return parent;
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
            hover(mc, screen, hoverIndex);
        } else if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) {
            // Parked in the corner so no tooltip from the previous step covers the screen being captured.
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 2.0D, 2.0D);
        }
        if (++frame == step.frames()) {
            pendingShot = step.name();
        }
    }

    /** End of the frame: everything, notifications included, has been drawn by now. */
    @SubscribeEvent
    public void onRenderTickEnd(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingShot == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String name = pendingShot;
        pendingShot = null;
        if (!name.startsWith("00") && !name.startsWith("13") && !name.startsWith("99")) {
            shot(mc, name);
        }
        next(mc);
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

    /** Puts a worn armor piece on the client player directly (commands don't refresh these slots in the harness). */
    private static void wear(Minecraft mc, int piece, net.minecraft.world.item.Item item, int damage) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
        stack.setDamageValue(damage);
        mc.player.getInventory().armor.set(piece, stack);
    }

    private void shot(Minecraft mc, String name) {
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(outDir.resolve(name + ".png"));
        } catch (IOException e) {
            Fordix.LOGGER.warn("[devtest] screenshot failed", e);
        }
    }

    /** A console with the settings of a spawner console, to see the screen. */
    private static com.omneria.fordix.console.ConsoleData sampleConsole() {
        net.minecraft.network.chat.Component none = net.minecraft.network.chat.Component.empty();
        java.util.List<com.omneria.fordix.console.ConsoleData.Row> rows = java.util.List.of(
                row("cadence", "Cadence", "delai x0,75 par cran", 2, "200-800 > 113-450 ticks", "minecraft:cobweb", "minecraft:quartz"),
                row("count", "Quantite", "+1 monstre par apparition", 1, "4 > 5 monstres", "minecraft:iron_bars", "minecraft:lightning_rod"),
                row("population", "Population max", "x4 puis x2 par cran", 3, "6 > 96 monstres", "minecraft:lead", "minecraft:bone_block"),
                row("range", "Portee d'activation", "+8 blocs par cran", 0, "16 blocs", "minecraft:slime_ball", "minecraft:ender_eye"),
                row("radius", "Rayon d'apparition", "+2 blocs par cran", -1, "4 > 2 blocs", "minecraft:string", "minecraft:compass"),
                row("health", "Vie des monstres", "+50 % par cran", 5, "x3,5", "minecraft:pointed_dripstone", "#minecraft:fishes"));
        return new com.omneria.fordix.console.ConsoleData("devtest:console", net.minecraft.core.BlockPos.ZERO,
                net.minecraft.network.chat.Component.literal("Console des spawners"), net.minecraft.network.chat.Component.literal("Chunk 0 / 0"),
                net.minecraft.network.chat.Component.literal("3 spawners"), net.minecraft.network.chat.Component.literal("Un objet par cran"), 5, rows,
                java.util.List.of(net.minecraft.network.chat.Component.literal("Zombie"), net.minecraft.network.chat.Component.literal("Squelette")));
    }

    private static com.omneria.fordix.console.ConsoleData.Row row(String id, String name, String effect, int level, String value, String malus, String bonus) {
        return new com.omneria.fordix.console.ConsoleData.Row(id, net.minecraft.network.chat.Component.literal(name), net.minecraft.network.chat.Component.literal(effect),
                level, net.minecraft.network.chat.Component.literal(value),
                new com.omneria.fordix.console.ConsoleData.Slot(malus, net.minecraft.network.chat.Component.literal(malus)),
                new com.omneria.fordix.console.ConsoleData.Slot(bonus, net.minecraft.network.chat.Component.literal(bonus)));
    }

    /** A menu of {@code own} empty slots followed by the player's inventory, standing in for a mod's container. */
    private static net.minecraft.world.inventory.AbstractContainerMenu fakeMenu(Minecraft mc, int own) {
        net.minecraft.world.entity.player.Inventory inventory = mc.player.getInventory();
        net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(own);
        return new net.minecraft.world.inventory.AbstractContainerMenu(null, 1) {
            {
                for (int i = 0; i < own; i++) {
                    addSlot(new net.minecraft.world.inventory.Slot(container, i, 0, 0));
                }
                for (int i = 9; i < 36; i++) {
                    addSlot(new net.minecraft.world.inventory.Slot(inventory, i, 0, 0));
                }
                for (int i = 0; i < 9; i++) {
                    addSlot(new net.minecraft.world.inventory.Slot(inventory, i, 0, 0));
                }
            }

            @Override
            public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return true;
            }
        };
    }
}
