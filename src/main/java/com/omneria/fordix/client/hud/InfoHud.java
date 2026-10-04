package com.omneria.fordix.client.hud;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.Locale;
import com.omneria.fordix.client.FordixClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.fml.loading.FMLPaths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The information panels of the HUD, each a Forge overlay of its own (so the HUD layout screen lists, moves, sizes and
 * hides them like any other): coordinates with facing and biome, FPS / TPS / ping, the block or creature under the
 * crosshair, the durability and ammunition of what is in hand, the clocks and the compass. Every line can be switched
 * off ({@link FordixConfig#hudLine}). They stay out of the way of the F3 screen, which shows much of the same.
 */
public final class InfoHud {
    public static final String COORDINATES = "coordinates";
    public static final String PERFORMANCE = "performance";
    public static final String TARGET_BLOCK = "target_block";
    public static final String GEAR = "gear";
    public static final String CLOCK = "clock";
    public static final String COMPASS = "compass";

    private static final int PAD = 4;
    private static final int LINE = 10;
    private static final int GAP = 4;
    private static final int TARGET_WIDTH = 160;
    private static final int TARGET_HEIGHT = 28;
    private static final int TARGET_MAX_WIDTH = 230;
    private static final int LEFT_COLUMN_WIDTH = 160;
    private static final int PANEL_FILL = 0xB00B1118;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int LABEL = 0xFF8AA0AD;
    private static final int GOOD = 0xFF7DFFB0;
    private static final int WARN = 0xFFFFB347;
    private static final int BAD = 0xFFFF6B6B;
    private static final int BLUE = 0xFF6FB4FF;
    private static final int[] LEVEL_COLORS = {GOOD, WARN, BAD};
    private static final String[] ARROWS = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final TpsMeter METER = new TpsMeter();
    private static MarkerStore markers;

    private InfoHud() {
    }

    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), COORDINATES, InfoHud::drawCoordinates);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), PERFORMANCE, InfoHud::drawPerformance);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), CLOCK, InfoHud::drawClock);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), COMPASS, InfoHud::drawCompass);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), TARGET_BLOCK, InfoHud::drawTarget);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), GEAR, InfoHud::drawGear);
    }

    // ---- TPS: the game time packets, watched on their way to the game

    /** Joining a server: listen to the game time it sends every second. */
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        METER.reset();
        try {
            ChannelPipeline pipeline = event.getConnection().channel().pipeline();
            if (pipeline.get("fordix_tps") == null) {
                ChannelDuplexHandler handler = new ChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                        if (message instanceof ClientboundSetTimePacket packet) {
                            METER.onGameTime(packet.getGameTime(), System.nanoTime());
                        }
                        super.channelRead(context, message);
                    }
                };
                if (pipeline.get("packet_handler") != null) {
                    pipeline.addBefore("packet_handler", "fordix_tps", handler);
                } else {
                    pipeline.addLast("fordix_tps", handler);
                }
            }
        } catch (RuntimeException e) {
            // no TPS reading on this connection: the panel shows dashes
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        METER.reset();
    }

    /** TPS: the integrated server's own figure when playing alone, the estimate from the packets on a server; -1 if unknown. */
    private static double tps(Minecraft minecraft) {
        if (minecraft.getSingleplayerServer() != null) {
            double millis = minecraft.getSingleplayerServer().getAverageTickTime();
            return millis <= 0 ? TpsMeter.MAX_TPS : Math.min(TpsMeter.MAX_TPS, 1000.0D / millis);
        }
        return METER.tps();
    }

    // ---- drawing helpers

    private record Seg(String text, int color) {
    }

    /** One line made of colored pieces; groups are set apart by a few spaces. */
    private static final class Line {
        private final List<Seg> segs = new ArrayList<>();

        Line add(Seg... group) {
            if (!segs.isEmpty()) {
                segs.add(new Seg("   ", WHITE));
            }
            segs.addAll(List.of(group));
            return this;
        }

        boolean isEmpty() {
            return segs.isEmpty();
        }

        int width(Font font) {
            int width = 0;
            for (Seg seg : segs) {
                width += font.width(seg.text());
            }
            return width;
        }
    }

    private static Seg label(String text) {
        return new Seg(text, LABEL);
    }

    private static Seg value(String text, int color) {
        return new Seg(text, color);
    }

    private static boolean hidden(Minecraft minecraft) {
        return !FordixConfig.VOIDFRAME_HUD.get() || minecraft.options.hideGui || minecraft.options.renderDebug
                || minecraft.player == null || minecraft.level == null;
    }

    private static int panelHeight(int lines) {
        return lines == 0 ? 0 : lines * LINE + 4;
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_FILL);
        graphics.fill(x, y, x + 2, y + height, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
    }

    /** Draws the non-empty lines as one panel at (x, y); nothing at all when there is no line. */
    private static void drawLines(GuiGraphics graphics, Font font, int x, int y, List<Line> all) {
        List<Line> lines = all.stream().filter(line -> !line.isEmpty()).toList();
        if (lines.isEmpty()) {
            return;
        }
        int width = 0;
        for (Line line : lines) {
            width = Math.max(width, line.width(font));
        }
        panel(graphics, x, y, width + PAD * 2 + 2, panelHeight(lines.size()));
        for (int i = 0; i < lines.size(); i++) {
            int cursor = x + PAD + 2;
            for (Seg seg : lines.get(i).segs) {
                graphics.drawString(font, seg.text(), cursor, y + 3 + i * LINE, seg.color(), true);
                cursor += font.width(seg.text());
            }
        }
    }

    private static boolean on(String line) {
        return FordixConfig.hudLine(line);
    }

    // ---- the left column: where each panel sits by default, whatever its neighbours show

    private static int coordinatesRows() {
        return (on("coordinates_xyz") ? 1 : 0) + (on("coordinates_direction") || on("coordinates_biome") ? 1 : 0);
    }

    private static int performanceRows() {
        return on("performance_fps") || on("performance_tps") || on("performance_ping") ? 1 : 0;
    }

    private static int clockRows() {
        return (on("clock_real") || on("clock_game") ? 1 : 0) + (on("clock_weather") ? 1 : 0);
    }

    /** Top of the panel at this place of the column (coordinates, performance, clock, compass), under the ones above it. */
    private static int columnTop(int index) {
        int[] rows = {coordinatesRows(), performanceRows(), clockRows()};
        int top = 4;
        for (int i = 0; i < index; i++) {
            if (rows[i] > 0) {
                top += panelHeight(rows[i]) + GAP;
            }
        }
        return top;
    }

    // ---- coordinates

    private static void drawCoordinates(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        LocalPlayer player = minecraft.player;
        Line position = new Line();
        if (on("coordinates_xyz")) {
            position.add(value("X ", axisColor('X')), value(number(player.getX()), WHITE))
                    .add(value("Y ", axisColor('Y')), value(number(player.getY()), WHITE))
                    .add(value("Z ", axisColor('Z')), value(number(player.getZ()), WHITE));
        }
        Line where = new Line();
        if (on("coordinates_direction")) {
            where.add(value(Tr.s("hud.dir." + player.getDirection().getName()), LABEL));
        }
        if (on("coordinates_biome")) {
            where.add(value(biome(minecraft, player.blockPosition()), LABEL));
        }
        drawLines(graphics, minecraft.font, 4, columnTop(0), List.of(position, where));
    }

    private static int axisColor(char axis) {
        return switch (axis) {
            case 'X' -> 0xFFFF6B6B;
            case 'Y' -> 0xFF7DFFB0;
            default -> 0xFF6FB4FF;
        };
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String biome(Minecraft minecraft, BlockPos pos) {
        var key = minecraft.level.getBiome(pos).unwrapKey().map(ResourceKey::location).orElse(null);
        if (key == null) {
            return "?";
        }
        String translation = "biome." + key.getNamespace() + "." + key.getPath().replace('/', '.');
        if (I18n.exists(translation)) {
            return I18n.get(translation);
        }
        String words = key.getPath().replace('_', ' ').replace('/', ' ');
        return words.isEmpty() ? "?" : words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
    }

    // ---- performance

    private static void drawPerformance(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        Line line = new Line();
        if (on("performance_fps")) {
            int fps = minecraft.getFps();
            line.add(label("FPS "), value(Integer.toString(fps), fps >= 60 ? GOOD : fps >= 30 ? WARN : BAD));
        }
        if (on("performance_tps")) {
            double tps = tps(minecraft);
            line.add(label("TPS "), value(tps < 0 ? "--" : number(tps), tps < 0 ? LABEL : tps >= 19.0D ? GOOD : tps >= 15.0D ? WARN : BAD));
        }
        if (on("performance_ping")) {
            int ping = ping(minecraft);
            line.add(label("PING "), value(ping < 0 ? "--" : ping + " ms", ping < 0 ? LABEL : ping <= 100 ? GOOD : ping <= 250 ? WARN : BAD));
        }
        drawLines(graphics, minecraft.font, 4, columnTop(1), List.of(line));
    }

    /** Latency to the server in ms, from the tab list entry; -1 when playing alone (no network) or not yet measured. */
    private static int ping(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.hasSingleplayerServer()) {
            return -1;
        }
        var info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
        return info == null || info.getLatency() <= 0 ? -1 : info.getLatency();
    }

    // ---- clock and weather

    private static void drawClock(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        Level level = minecraft.level;
        boolean cycle = !level.dimensionType().hasFixedTime();
        Line times = new Line();
        if (on("clock_real")) {
            times.add(label(Tr.s("hud.clock.real") + " "), value(LocalTime.now().format(CLOCK_FORMAT), WHITE));
        }
        if (on("clock_game") && cycle) {
            boolean night = HudFormat.isNight(level.getDayTime());
            times.add(label(Tr.s("hud.clock.game") + " "), value(HudFormat.gameClock(level.getDayTime()), WHITE),
                    value(" " + Tr.s(night ? "hud.clock.night" : "hud.clock.day"), night ? BLUE : WARN));
        }
        Line weather = new Line();
        if (on("clock_weather") && cycle) {
            weather.add(level.isThundering() ? value(Tr.s("hud.weather.thunder"), BAD)
                    : level.isRaining() ? value(Tr.s("hud.weather.rain"), BLUE) : value(Tr.s("hud.weather.clear"), LABEL));
        }
        drawLines(graphics, minecraft.font, 4, columnTop(2), List.of(times, weather));
    }

    // ---- compass

    private static String markerKey(Minecraft minecraft) {
        String world = minecraft.getCurrentServer() != null ? minecraft.getCurrentServer().ip
                : minecraft.getSingleplayerServer() != null ? "sp:" + minecraft.getSingleplayerServer().getWorldData().getLevelName() : "?";
        return world + "|" + minecraft.level.dimension().location();
    }

    private static MarkerStore markers() {
        if (markers == null) {
            markers = new MarkerStore(FMLPaths.CONFIGDIR.get().resolve("fordix-markers.json"));
        }
        return markers;
    }

    /** The marker key: puts the marker where the player stands, or with Shift takes it away. */
    public static void onMarkerKey(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        String key = markerKey(minecraft);
        if (Screen.hasShiftDown()) {
            markers().remove(key);
            minecraft.gui.setOverlayMessage(Tr.c("hud.compass.cleared"), false);
        } else {
            BlockPos pos = minecraft.player.blockPosition();
            markers().set(key, new MarkerStore.Marker(pos.getX(), pos.getY(), pos.getZ()));
            minecraft.gui.setOverlayMessage(Tr.c("hud.compass.set", pos.getX(), pos.getY(), pos.getZ()), false);
        }
    }

    private static Line pointer(String name, Minecraft minecraft, double x, double z) {
        LocalPlayer player = minecraft.player;
        double dx = x - player.getX();
        double dz = z - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        return new Line().add(label(name + " "), value(distance < 2.0D ? "\u25CF" : ARROWS[HudFormat.arrowIndex(player.getYRot(), dx, dz)], WARN),
                value(HudFormat.distance(distance), WHITE));
    }

    private static void drawCompass(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        Line spawn = new Line();
        if (on("compass_spawn") && minecraft.level.dimension() == Level.OVERWORLD) {
            BlockPos at = minecraft.level.getSharedSpawnPos();
            spawn = pointer(Tr.s("hud.compass.spawn"), minecraft, at.getX() + 0.5D, at.getZ() + 0.5D);
        }
        Line marker = new Line();
        if (on("compass_marker")) {
            MarkerStore.Marker at = markers().get(markerKey(minecraft));
            marker = at != null ? pointer(Tr.s("hud.compass.marker"), minecraft, at.x() + 0.5D, at.z() + 0.5D)
                    : new Line().add(label(Tr.s("hud.compass.marker") + " "),
                            value(Tr.s("hud.compass.none", FordixClient.SET_MARKER.getTranslatedKeyMessage().getString()), LABEL));
        }
        int top = columnTop(3) + 0;
        drawLines(graphics, minecraft.font, 4, top, List.of(spawn, marker));
    }

    // ---- what is under the crosshair

    private static void drawTarget(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            drawBlock(graphics, minecraft, width, blockHit);
        } else if (hit instanceof EntityHitResult entityHit && on("target_entity")) {
            drawEntity(graphics, minecraft, width, entityHit.getEntity());
        }
    }

    /** Centered under the boss bar, but never over the left column of panels on a narrow screen. */
    private static int targetLeft(int width, int boxWidth) {
        return Math.max(width / 2 - boxWidth / 2, 4 + LEFT_COLUMN_WIDTH + GAP);
    }

    private static void drawBlock(GuiGraphics graphics, Minecraft minecraft, int width, BlockHitResult hit) {
        BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
        if (state.isAir()) {
            return;
        }
        Font font = minecraft.font;
        String name = state.getBlock().getName().getString();
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        List<Seg> tool = on("target_tool") ? toolLine(minecraft, state, hit.getBlockPos()) : List.of();
        int toolWidth = 0;
        for (Seg seg : tool) {
            toolWidth += font.width(seg.text());
        }
        int boxWidth = Math.min(TARGET_MAX_WIDTH, Math.max(TARGET_WIDTH, Math.max(toolWidth, Math.max(font.width(name), font.width(id))) + 6 + 16 + 5 + 6));
        int boxHeight = TARGET_HEIGHT + (tool.isEmpty() ? 0 : LINE);
        int x = targetLeft(width, boxWidth);
        int y = 26;
        panel(graphics, x, y, boxWidth, boxHeight);
        ItemStack icon = state.getBlock().getCloneItemStack(minecraft.level, hit.getBlockPos(), state);
        if (icon.isEmpty()) {
            icon = new ItemStack(state.getBlock());
        }
        graphics.renderItem(icon, x + 6, y + 6);
        int textX = x + 6 + 16 + 5;
        int textWidth = boxWidth - (textX - x) - 4;
        graphics.drawString(font, font.plainSubstrByWidth(name, textWidth), textX, y + 5, WHITE, true);
        graphics.drawString(font, font.plainSubstrByWidth(id, textWidth), textX, y + 5 + LINE, LABEL, false);
        int cursor = x + 6;
        for (Seg seg : tool) {
            graphics.drawString(font, seg.text(), cursor, y + TARGET_HEIGHT - 1, seg.color(), true);
            cursor += font.width(seg.text());
        }
    }

    /** "Tool : Pickaxe (Iron) · Hardness 3.0", the tool colored by whether the one in hand does the job. */
    private static List<Seg> toolLine(Minecraft minecraft, BlockState state, BlockPos pos) {
        float hardness = state.getDestroySpeed(minecraft.level, pos);
        if (hardness < 0) {
            return List.of(value(Tr.s("hud.target.unbreakable"), BAD));
        }
        String tool = state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? "pickaxe" : state.is(BlockTags.MINEABLE_WITH_AXE) ? "axe"
                : state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? "shovel" : state.is(BlockTags.MINEABLE_WITH_HOE) ? "hoe" : "hand";
        String tier = state.is(BlockTags.NEEDS_DIAMOND_TOOL) ? "diamond" : state.is(BlockTags.NEEDS_IRON_TOOL) ? "iron"
                : state.is(BlockTags.NEEDS_STONE_TOOL) ? "stone" : null;
        boolean ready = !state.requiresCorrectToolForDrops() || minecraft.player.getMainHandItem().isCorrectToolForDrops(state);
        String tierText = tier == null ? "" : " (" + Tr.s("hud.tier." + tier) + ")";
        return List.of(value(Tr.s("hud.tool." + tool) + tierText, ready ? GOOD : WARN),
                label("  ·  " + Tr.s("hud.target.hardness") + " " + number(hardness)));
    }

    private static void drawEntity(GuiGraphics graphics, Minecraft minecraft, int width, Entity entity) {
        Font font = minecraft.font;
        String name = entity.getDisplayName().getString();
        String detail;
        float left = 1.0F;
        int detailColor = LABEL;
        if (entity instanceof LivingEntity living) {
            float health = living.getHealth();
            float max = Math.max(1.0F, living.getMaxHealth());
            left = Math.max(0.0F, Math.min(1.0F, health / max));
            detail = "\u2665 " + health(health) + " / " + health(max);
            detailColor = LEVEL_COLORS[HudFormat.level(left)];
        } else {
            detail = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        }
        SpawnEggItem egg = entity instanceof net.minecraft.world.entity.Mob ? SpawnEggItem.byId(entity.getType()) : null;
        int iconSpace = egg != null ? 16 + 5 : 0;
        int boxWidth = Math.min(TARGET_MAX_WIDTH, Math.max(TARGET_WIDTH, Math.max(font.width(name), font.width(detail)) + 6 + iconSpace + 6));
        int x = targetLeft(width, boxWidth);
        int y = 26;
        panel(graphics, x, y, boxWidth, TARGET_HEIGHT);
        if (egg != null) {
            graphics.renderItem(new ItemStack(egg), x + 6, y + 6);
        }
        int textX = x + 6 + iconSpace;
        int textWidth = boxWidth - (textX - x) - 4;
        graphics.drawString(font, font.plainSubstrByWidth(name, textWidth), textX, y + 5, WHITE, true);
        graphics.drawString(font, font.plainSubstrByWidth(detail, textWidth), textX, y + 5 + LINE, detailColor, true);
        if (entity instanceof LivingEntity) {
            graphics.fill(x + 2, y + TARGET_HEIGHT - 2, x + boxWidth, y + TARGET_HEIGHT, 0x40FFFFFF);
            graphics.fill(x + 2, y + TARGET_HEIGHT - 2, x + 2 + Math.round((boxWidth - 2) * left), y + TARGET_HEIGHT,
                    LEVEL_COLORS[HudFormat.level(left)]);
        }
    }

    private static String health(float value) {
        return value == Math.floor(value) ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    // ---- what is in hand: durability and ammunition

    private static int ammoCount(Player player, ProjectileWeaponItem weapon) {
        Predicate<ItemStack> supported = weapon.getAllSupportedProjectiles();
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (supported.test(stack)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (supported.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private record GearRow(ItemStack stack, String durability, float left, String ammo, int ammoColor) {
        int height() {
            return Math.max(18, (durability != null ? 13 : 0) + (ammo != null ? LINE : 0) + 2);
        }
    }

    private static GearRow gearRow(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String durability = null;
        float left = 1.0F;
        if (on("gear_durability") && stack.isDamageableItem()) {
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            durability = remaining + " / " + stack.getMaxDamage();
            left = HudFormat.durabilityLeft(stack.getDamageValue(), stack.getMaxDamage());
        }
        String ammo = null;
        int ammoColor = WHITE;
        if (on("gear_ammo") && stack.getItem() instanceof ProjectileWeaponItem weapon) {
            if (player.getAbilities().instabuild) {
                ammo = Tr.s("hud.gear.ammo") + " \u221E";
            } else {
                int count = ammoCount(player, weapon);
                ammo = Tr.s("hud.gear.ammo") + " \u00D7" + count;
                ammoColor = count == 0 ? BAD : count < 16 ? WARN : WHITE;
            }
        }
        return durability == null && ammo == null ? null : new GearRow(stack, durability, left, ammo, ammoColor);
    }

    private static void drawGear(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        Player player = minecraft.player;
        List<GearRow> rows = new ArrayList<>();
        for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            GearRow row = gearRow(player, stack);
            if (row != null) {
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            return;
        }
        Font font = minecraft.font;
        int textWidth = 56;
        int total = PAD;
        for (GearRow row : rows) {
            textWidth = Math.max(textWidth, Math.max(row.durability != null ? font.width(row.durability) : 0,
                    row.ammo != null ? font.width(row.ammo) : 0));
            total += row.height() + 2;
        }
        int boxWidth = PAD + 2 + 16 + 5 + textWidth + PAD;
        int boxHeight = total - 2 + PAD - 2;
        int x = width - boxWidth - 4;
        int y = height - boxHeight - 4;
        panel(graphics, x, y, boxWidth, boxHeight);
        int rowTop = y + PAD - 1;
        for (GearRow row : rows) {
            graphics.renderItem(row.stack, x + PAD + 2, rowTop + (row.height() - 16) / 2);
            int textX = x + PAD + 2 + 16 + 5;
            int cursor = rowTop + 1;
            if (row.durability != null) {
                int color = LEVEL_COLORS[HudFormat.level(row.left)];
                graphics.drawString(font, row.durability, textX, cursor, color, true);
                graphics.fill(textX, cursor + 10, textX + textWidth, cursor + 12, 0x40FFFFFF);
                graphics.fill(textX, cursor + 10, textX + Math.round(textWidth * row.left), cursor + 12, color);
                cursor += 13;
            }
            if (row.ammo != null) {
                graphics.drawString(font, row.ammo, textX, cursor, row.ammoColor, true);
            }
            rowTop += row.height() + 2;
        }
    }
}
