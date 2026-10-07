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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.Locale;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
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
    public static final String TARGET_MOB = "target_mob";
    public static final String GEAR = "gear";
    public static final String CLOCK = "clock";
    public static final String COMPASS = "compass";
    public static final String EFFECTS = "effects";

    private static final int GAP = 4;
    private static final int TARGET_WIDTH = 160;
    private static final int TARGET_HEIGHT = 28;
    private static final int TARGET_MAX_WIDTH = 230;
    private static final int LEFT_COLUMN_WIDTH = 160;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int LABEL = 0xFF8AA0AD;
    private static final int HINT = 0xFF55D8E8;
    private static final int GOOD = 0xFF7DFFB0;
    private static final int WARN = 0xFFFFB347;
    private static final int BAD = 0xFFFF6B6B;
    private static final int BLUE = 0xFF6FB4FF;
    private static final int[] LEVEL_COLORS = {GOOD, WARN, BAD};
    private static final String[] ARROWS = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final TpsMeter METER = new TpsMeter();

    private InfoHud() {
    }

    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), COORDINATES, InfoHud::drawCoordinates);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), PERFORMANCE, InfoHud::drawPerformance);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), CLOCK, InfoHud::drawClock);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), COMPASS, InfoHud::drawCompass);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), TARGET_BLOCK, InfoHud::drawTargetBlock);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), TARGET_MOB, InfoHud::drawTargetMob);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), GEAR, InfoHud::drawGear);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), EFFECTS, InfoHud::drawEffects);
        // The pet wheel is a screen, drawn only when its key is held: this empty overlay lets the layout screen list and place it.
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "pet_wheel", (gui, graphics, partialTick, width, height) -> {
        });
        PlayerListHud.registerOverlays(event);
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
                        } else if (message instanceof net.minecraft.network.protocol.game.ClientboundTabListPacket packet) {
                            PlayerListHud.onTabListPacket(packet);
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
        PlayerListHud.reset();
    }

    /** TPS: the integrated server's own figure when playing alone, the estimate from the packets on a server; -1 if unknown. */
    static double tps(Minecraft minecraft) {
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

    private static boolean compact() {
        try {
            return FordixConfig.HUD_COMPACT.get();
        } catch (IllegalStateException notLoadedYet) {
            return false;
        }
    }

    private static int pad() {
        return compact() ? 3 : 4;
    }

    private static int lineH() {
        return compact() ? 9 : 10;
    }

    /** The panel background with the player's opacity. */
    private static int panelFill() {
        int percent;
        try {
            percent = Integer.parseInt(FordixConfig.HUD_OPACITY.get());
        } catch (IllegalStateException | NumberFormatException notReady) {
            percent = 70;
        }
        return (Math.max(0, Math.min(255, percent * 255 / 100)) << 24) | 0x0B1118;
    }

    private static boolean hidden(Minecraft minecraft) {
        return !FordixConfig.VOIDFRAME_HUD.get() || minecraft.options.hideGui || minecraft.options.renderDebug
                || minecraft.player == null || minecraft.level == null;
    }

    private static int panelHeight(int lines) {
        return lines == 0 ? 0 : lines * lineH() + pad();
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, panelFill());
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
        panel(graphics, x, y, width + pad() * 2 + 2, panelHeight(lines.size()));
        for (int i = 0; i < lines.size(); i++) {
            int cursor = x + pad() + 2;
            for (Seg seg : lines.get(i).segs) {
                graphics.drawString(font, seg.text(), cursor, y + pad() - 1 + i * lineH(), seg.color(), true);
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

    /** Whether the player has one of these items anywhere on them (bag, armor slots, off hand), or the option is off. */
    private static boolean unlocked(Player player, net.minecraft.world.item.Item... items) {
        if (!FordixConfig.HUD_REQUIRE_ITEMS.get()) {
            return true;
        }
        for (List<ItemStack> stacks : List.of(player.getInventory().items, player.getInventory().offhand, player.getInventory().armor)) {
            for (ItemStack stack : stacks) {
                for (net.minecraft.world.item.Item item : items) {
                    if (stack.is(item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasClock(Player player) {
        return unlocked(player, net.minecraft.world.item.Items.CLOCK);
    }

    private static boolean hasCompass(Player player) {
        return unlocked(player, net.minecraft.world.item.Items.COMPASS, net.minecraft.world.item.Items.RECOVERY_COMPASS);
    }

    private static int clockRows() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !hasClock(minecraft.player)) {
            return 0;
        }
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
        if (!hasClock(minecraft.player)) {
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
        if (!hasCompass(minecraft.player)) {
            return;
        }
        Line spawn = new Line();
        if (on("compass_spawn") && minecraft.level.dimension() == Level.OVERWORLD) {
            BlockPos at = minecraft.level.getSharedSpawnPos();
            spawn = pointer(Tr.s("hud.compass.spawn"), minecraft, at.getX() + 0.5D, at.getZ() + 0.5D);
        }
        int top = columnTop(3) + 0;
        drawLines(graphics, minecraft.font, 4, top, List.of(spawn));
    }

    // ---- potion effects as a list

    /** The vanilla effect icons give way to the list. */
    public static void onOverlayPre(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.POTION_ICONS.type() && replacesVanillaEffects()) {
            event.setCanceled(true);
        }
    }

    /** Whether the effects list stands in for vanilla's icons. */
    public static boolean replacesVanillaEffects() {
        try {
            return FordixConfig.VOIDFRAME_HUD.get() && FordixConfig.HUD_EFFECTS_LIST.get();
        } catch (IllegalStateException notLoadedYet) {
            return false;
        }
    }

    private static void drawEffects(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft) || !replacesVanillaEffects()) {
            return;
        }
        List<MobEffectInstance> effects = new ArrayList<>(minecraft.player.getActiveEffects());
        if (effects.isEmpty()) {
            return;
        }
        effects.sort(java.util.Comparator.comparingInt((MobEffectInstance e) -> e.getEffect().isBeneficial() ? 0 : 1));
        if (effects.size() > 10) {
            effects = effects.subList(0, 10);
        }
        Font font = minecraft.font;
        int nameWidth = 0;
        int timeWidth = 0;
        List<String> names = new ArrayList<>();
        List<String> times = new ArrayList<>();
        for (MobEffectInstance effect : effects) {
            String name = I18n.get(effect.getDescriptionId()) + (effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "");
            String time = MobEffectUtil.formatDuration(effect, 1.0F).getString();
            names.add(name);
            times.add(time);
            nameWidth = Math.max(nameWidth, font.width(name));
            timeWidth = Math.max(timeWidth, font.width(time));
        }
        int row = 18;
        int boxWidth = pad() + 2 + 16 + 4 + nameWidth + 8 + timeWidth + pad();
        int boxHeight = effects.size() * row + pad() - 2;
        int x = width - boxWidth - 4;
        int y = 102;
        panel(graphics, x, y, boxWidth, boxHeight);
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = effects.get(i);
            int top = y + pad() / 2 + i * row;
            graphics.blit(x + pad() + 2, top, 0, 16, 16, minecraft.getMobEffectTextures().get(effect.getEffect()));
            int textY = top + 4;
            graphics.drawString(font, names.get(i), x + pad() + 2 + 16 + 4, textY, effect.getEffect().isBeneficial() ? WHITE : BAD, true);
            boolean ending = effect.getDuration() != -1 && effect.getDuration() <= 200;
            graphics.drawString(font, times.get(i), x + boxWidth - pad() - font.width(times.get(i)), textY, ending ? BAD : LABEL, true);
        }
    }

    // ---- what is under the crosshair

    /** The block under the crosshair, with its tool and, for a container, what was last seen in it. */
    private static void drawTargetBlock(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        // A creature in sight (seen from afar) takes the place of the block behind it.
        if (on("target_entity") && FarTarget.entity(minecraft) != null) {
            return;
        }
        int[] box = drawBlock(graphics, minecraft, width, blockHit);
        if (box == null || !on("target_container")) {
            return;
        }
        List<ItemStack> contents = List.of();
        String caption = "";
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        if (minecraft.level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox) {
            contents = jukebox.getFirstItem().isEmpty() ? List.of() : List.of(jukebox.getFirstItem());
            caption = Tr.s("hud.target.record");
        } else {
            ContainerMemory.Snapshot snapshot = ContainerMemory.at(minecraft, pos, state);
            if (snapshot != null) {
                contents = snapshot.items();
                caption = Tr.s("hud.target.seen", age(System.currentTimeMillis() - snapshot.seenAt()));
                if (contents.isEmpty()) {
                    caption = Tr.s("hud.target.empty") + " · " + caption;
                }
            }
        }
        if (!contents.isEmpty() || !caption.isEmpty()) {
            drawPreview(graphics, minecraft.font, box[0], box[1] + 2, box[2], caption, contents);
        }
    }

    /** The creature (or item frame, dropped item...) under the crosshair, with its health and what it holds or wears. */
    private static void drawTargetMob(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft) || !on("target_entity")) {
            return;
        }
        EntityHitResult entityHit = FarTarget.entity(minecraft);
        if (entityHit == null) {
            return;
        }
        Entity entity = entityHit.getEntity();
        int[] box = drawEntity(graphics, minecraft, width, entity);
        if (box == null || !on("target_container")) {
            return;
        }
        List<ItemStack> contents = List.of();
        String caption = "";
        if (entity instanceof ItemFrame frame && !frame.getItem().isEmpty()) {
            contents = List.of(frame.getItem());
            caption = Tr.s("hud.target.frame");
        } else if (entity instanceof ItemEntity dropped) {
            contents = List.of(dropped.getItem());
            caption = Tr.s("hud.target.dropped");
        } else if (entity instanceof LivingEntity living) {
            List<ItemStack> worn = new ArrayList<>();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!living.getItemBySlot(slot).isEmpty()) {
                    worn.add(living.getItemBySlot(slot));
                }
            }
            contents = worn;
            caption = Tr.s("hud.target.equipment");
        }
        if (!contents.isEmpty() || !caption.isEmpty()) {
            drawPreview(graphics, minecraft.font, box[0], box[1] + 2, box[2], caption, contents);
        }
    }

    private static String age(long millis) {
        long seconds = millis / 1000L;
        return seconds < 60 ? seconds + " s" : seconds < 3600 ? seconds / 60 + " min" : seconds / 3600 + " h";
    }

    /** A strip under the target panel: a caption and the items (merged, counts on them), with "+N" when they don't all fit. */
    private static void drawPreview(GuiGraphics graphics, Font font, int x, int y, int boxWidth, String caption, List<ItemStack> stacks) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ContainerMemory.merge(merged, stack);
        }
        int fits = Math.max(1, (boxWidth - 12) / 18);
        boolean more = merged.size() > fits;
        int shown = more ? fits - 1 : merged.size();
        int height = lineH() + pad() + (merged.isEmpty() ? 0 : 20);
        panel(graphics, x, y, boxWidth, height);
        graphics.drawString(font, font.plainSubstrByWidth(caption, boxWidth - 12), x + 6, y + pad() - 1, LABEL, true);
        for (int i = 0; i < shown; i++) {
            int ix = x + 6 + i * 18;
            int iy = y + lineH() + pad() - 1;
            graphics.renderItem(merged.get(i), ix, iy);
            graphics.renderItemDecorations(font, merged.get(i), ix, iy);
        }
        if (more) {
            graphics.drawString(font, "+" + (merged.size() - shown), x + 6 + shown * 18 + 2, y + lineH() + pad() + 3, WHITE, true);
        }
    }

    /** Centered under the boss bar, but never over the left column of panels on a narrow screen. */
    private static int targetLeft(int width, int boxWidth) {
        int left = Math.max(width / 2 - boxWidth / 2, 4 + LEFT_COLUMN_WIDTH + GAP);
        // ... and never past the right edge of the screen, whatever the width of the text.
        return Math.max(4, Math.min(left, width - 4 - boxWidth));
    }

    /** The width of a target panel for what it has to show, kept inside the screen. */
    private static int targetWidth(int width, int wanted) {
        return Math.max(60, Math.min(Math.min(TARGET_MAX_WIDTH, width - 8), Math.max(TARGET_WIDTH, wanted)));
    }

    /** The pieces of a line that fit in {@code room} pixels: the last one that does not is cut, the following ones dropped. */
    private static List<Seg> fit(Font font, List<Seg> segs, int room) {
        List<Seg> fitted = new ArrayList<>();
        int used = 0;
        for (Seg seg : segs) {
            int wide = font.width(seg.text());
            if (used + wide <= room) {
                fitted.add(seg);
                used += wide;
            } else {
                String cut = font.plainSubstrByWidth(seg.text(), Math.max(0, room - used));
                if (!cut.isEmpty()) {
                    fitted.add(new Seg(cut, seg.color()));
                }
                break;
            }
        }
        return fitted;
    }

    private static int[] drawBlock(GuiGraphics graphics, Minecraft minecraft, int width, BlockHitResult hit) {
        BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
        if (state.isAir()) {
            return null;
        }
        Font font = minecraft.font;
        String name = state.getBlock().getName().getString();
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        List<Seg> tool = on("target_tool") ? toolLine(minecraft, state, hit.getBlockPos()) : List.of();
        int toolWidth = 0;
        for (Seg seg : tool) {
            toolWidth += font.width(seg.text());
        }
        int boxWidth = targetWidth(width, Math.max(toolWidth + 12, Math.max(font.width(name), font.width(id)) + 6 + 16 + 5 + 6));
        tool = fit(font, tool, boxWidth - 12);
        int boxHeight = TARGET_HEIGHT + (tool.isEmpty() ? 0 : lineH());
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
        graphics.drawString(font, font.plainSubstrByWidth(id, textWidth), textX, y + 5 + lineH(), LABEL, false);
        int cursor = x + 6;
        for (Seg seg : tool) {
            graphics.drawString(font, seg.text(), cursor, y + TARGET_HEIGHT - 1, seg.color(), true);
            cursor += font.width(seg.text());
        }
        return new int[]{x, y + boxHeight, boxWidth};
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

    private static int[] drawEntity(GuiGraphics graphics, Minecraft minecraft, int width, Entity entity) {
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
        int boxWidth = targetWidth(width, Math.max(font.width(name), font.width(detail)) + 6 + iconSpace + 6);
        // A living creature other than a player has a sheet (its drops, its wiki page): the panel says which keys open it.
        String hint = entity instanceof LivingEntity && !(entity instanceof Player) && FordixConfig.GUIDE_HINTS.get()
                && !com.omneria.fordix.client.FordixClient.OPEN_GUIDE.isUnbound()
                ? "[" + com.omneria.fordix.client.guide.GuideHints.keyLabel() + "] " + Tr.s("hud.target.sheet") : null;
        if (hint != null) {
            boxWidth = targetWidth(width, Math.max(boxWidth, font.width(hint) + 12));
        }
        int boxHeight = TARGET_HEIGHT + (hint != null ? lineH() : 0);
        int x = targetLeft(width, boxWidth);
        int y = 26;
        panel(graphics, x, y, boxWidth, boxHeight);
        if (hint != null) {
            graphics.drawString(font, font.plainSubstrByWidth(hint, boxWidth - 12), x + 6, y + 5 + 2 * lineH(), HINT, true);
        }
        if (egg != null) {
            graphics.renderItem(new ItemStack(egg), x + 6, y + 6);
        }
        int textX = x + 6 + iconSpace;
        int textWidth = boxWidth - (textX - x) - 4;
        graphics.drawString(font, font.plainSubstrByWidth(name, textWidth), textX, y + 5, WHITE, true);
        graphics.drawString(font, font.plainSubstrByWidth(detail, textWidth), textX, y + 5 + lineH(), detailColor, true);
        if (entity instanceof LivingEntity) {
            graphics.fill(x + 2, y + boxHeight - 2, x + boxWidth, y + boxHeight, 0x40FFFFFF);
            graphics.fill(x + 2, y + boxHeight - 2, x + 2 + Math.round((boxWidth - 2) * left), y + boxHeight,
                    LEVEL_COLORS[HudFormat.level(left)]);
        }
        return new int[]{x, y + boxHeight, boxWidth};
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
            return Math.max(18, (durability != null ? 13 : 0) + (ammo != null ? lineH() : 0) + 2);
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
        List<ItemStack> armor = new ArrayList<>();
        if (on("gear_armor")) {
            for (int piece = 3; piece >= 0; piece--) {
                ItemStack worn = player.getInventory().armor.get(piece);
                if (!worn.isEmpty() && worn.isDamageableItem()) {
                    armor.add(worn);
                }
            }
        }
        if (rows.isEmpty() && armor.isEmpty()) {
            return;
        }
        Font font = minecraft.font;
        int textWidth = 56;
        int total = pad();
        for (GearRow row : rows) {
            textWidth = Math.max(textWidth, Math.max(row.durability != null ? font.width(row.durability) : 0,
                    row.ammo != null ? font.width(row.ammo) : 0));
            total += row.height() + 2;
        }
        if (!armor.isEmpty()) {
            total += 22;
        }
        int rowsWidth = rows.isEmpty() ? 0 : 16 + 5 + textWidth;
        int armorWidth = armor.isEmpty() ? 0 : armor.size() * 18 - 2;
        int boxWidth = pad() + 2 + Math.max(rowsWidth, armorWidth) + pad();
        int boxHeight = total - 2 + pad() - 2;
        int x = width - boxWidth - 4;
        int y = height - boxHeight - 4;
        panel(graphics, x, y, boxWidth, boxHeight);
        int rowTop = y + pad() - 1;
        for (GearRow row : rows) {
            graphics.renderItem(row.stack, x + pad() + 2, rowTop + (row.height() - 16) / 2);
            int textX = x + pad() + 2 + 16 + 5;
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
        for (int i = 0; i < armor.size(); i++) {
            ItemStack piece = armor.get(i);
            int ix = x + pad() + 2 + i * 18;
            float left = HudFormat.durabilityLeft(piece.getDamageValue(), piece.getMaxDamage());
            graphics.renderItem(piece, ix, rowTop);
            graphics.fill(ix, rowTop + 17, ix + 16, rowTop + 19, 0x40FFFFFF);
            graphics.fill(ix, rowTop + 17, ix + Math.round(16 * left), rowTop + 19, LEVEL_COLORS[HudFormat.level(left)]);
        }
    }
}
