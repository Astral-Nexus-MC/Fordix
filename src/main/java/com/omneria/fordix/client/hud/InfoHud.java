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

/**
 * Three information panels of the HUD, each a Forge overlay of its own (so the HUD layout screen lists, moves, sizes and
 * hides them like any other): the player's coordinates with facing and biome, FPS and TPS, and the block under the
 * crosshair. They stay out of the way of the F3 screen, which shows the same things.
 */
public final class InfoHud {
    public static final String COORDINATES = "coordinates";
    public static final String PERFORMANCE = "performance";
    public static final String TARGET_BLOCK = "target_block";

    private static final int PAD = 4;
    private static final int LINE = 10;
    private static final int TARGET_WIDTH = 160;
    private static final int TARGET_HEIGHT = 28;
    private static final int PANEL_FILL = 0xB00B1118;
    private static final int LABEL = 0xFF8AA0AD;
    private static final int GOOD = 0xFF7DFFB0;
    private static final int WARN = 0xFFFFB347;
    private static final int BAD = 0xFFFF6B6B;
    private static final TpsMeter METER = new TpsMeter();

    private InfoHud() {
    }

    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), COORDINATES, InfoHud::drawCoordinates);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), PERFORMANCE, InfoHud::drawPerformance);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), TARGET_BLOCK, InfoHud::drawTargetBlock);
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

    // ---- drawing

    private static boolean hidden(Minecraft minecraft) {
        return !FordixConfig.VOIDFRAME_HUD.get() || minecraft.options.hideGui || minecraft.options.renderDebug
                || minecraft.player == null || minecraft.level == null;
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_FILL);
        graphics.fill(x, y, x + 2, y + height, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
    }

    private static int axisColor(char axis) {
        return switch (axis) {
            case 'X' -> 0xFFFF6B6B;
            case 'Y' -> 0xFF7DFFB0;
            default -> 0xFF6FB4FF;
        };
    }

    private static void drawCoordinates(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        LocalPlayer player = minecraft.player;
        Font font = minecraft.font;
        String x = number(player.getX());
        String y = number(player.getY());
        String z = number(player.getZ());
        String facing = Tr.s("hud.dir." + player.getDirection().getName());
        String line2 = facing + "  ·  " + biome(minecraft, player.blockPosition());
        int line1Width = font.width("X " + x + "   Y " + y + "   Z " + z);
        int boxWidth = Math.max(line1Width, font.width(line2)) + PAD * 2 + 2;
        int boxHeight = LINE * 2 + PAD * 2 - 2;
        panel(graphics, 4, 4, boxWidth, boxHeight);
        int cursor = 4 + PAD + 2;
        int top = 4 + PAD;
        cursor = part(graphics, font, "X ", axisColor('X'), cursor, top);
        cursor = part(graphics, font, x + "   ", 0xFFFFFFFF, cursor, top);
        cursor = part(graphics, font, "Y ", axisColor('Y'), cursor, top);
        cursor = part(graphics, font, y + "   ", 0xFFFFFFFF, cursor, top);
        cursor = part(graphics, font, "Z ", axisColor('Z'), cursor, top);
        part(graphics, font, z, 0xFFFFFFFF, cursor, top);
        graphics.drawString(font, line2, 4 + PAD + 2, top + LINE, LABEL, true);
    }

    private static int part(GuiGraphics graphics, Font font, String text, int color, int x, int y) {
        graphics.drawString(font, text, x, y, color, true);
        return x + font.width(text);
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

    private static void drawPerformance(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        Font font = minecraft.font;
        int fps = minecraft.getFps();
        double tps = tps(minecraft);
        String fpsText = Integer.toString(fps);
        String tpsText = tps < 0 ? "--" : String.format(Locale.ROOT, "%.1f", tps);
        int ping = ping(minecraft);
        String pingText = ping < 0 ? "--" : ping + " ms";
        int boxWidth = font.width("FPS " + fpsText + "   TPS " + tpsText + "   PING " + pingText) + PAD * 2 + 2;
        int top = 4 + LINE * 2 + PAD * 2 - 2 + 4;
        panel(graphics, 4, top, boxWidth, LINE + PAD * 2 - 4);
        int cursor = 4 + PAD + 2;
        int textTop = top + PAD - 2;
        cursor = part(graphics, font, "FPS ", LABEL, cursor, textTop);
        cursor = part(graphics, font, fpsText + "   ", fps >= 60 ? GOOD : fps >= 30 ? WARN : BAD, cursor, textTop);
        cursor = part(graphics, font, "TPS ", LABEL, cursor, textTop);
        cursor = part(graphics, font, tpsText + "   ", tps < 0 ? LABEL : tps >= 19.0D ? GOOD : tps >= 15.0D ? WARN : BAD, cursor, textTop);
        cursor = part(graphics, font, "PING ", LABEL, cursor, textTop);
        part(graphics, font, pingText, ping < 0 ? LABEL : ping <= 100 ? GOOD : ping <= 250 ? WARN : BAD, cursor, textTop);
    }

    /** Latency to the server in ms, from the tab list entry; -1 when playing alone (no network) or not yet measured. */
    private static int ping(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.hasSingleplayerServer()) {
            return -1;
        }
        var info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
        return info == null || info.getLatency() <= 0 ? -1 : info.getLatency();
    }

    private static void drawTargetBlock(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
        if (state.isAir()) {
            return;
        }
        Font font = minecraft.font;
        int x = width / 2 - TARGET_WIDTH / 2;
        int y = 26;
        panel(graphics, x, y, TARGET_WIDTH, TARGET_HEIGHT);
        ItemStack icon = state.getBlock().getCloneItemStack(minecraft.level, blockHit.getBlockPos(), state);
        if (icon.isEmpty()) {
            icon = new ItemStack(state.getBlock());
        }
        graphics.renderItem(icon, x + 6, y + (TARGET_HEIGHT - 16) / 2);
        int textX = x + 6 + 16 + 5;
        int textWidth = TARGET_WIDTH - (textX - x) - 4;
        graphics.drawString(font, font.plainSubstrByWidth(state.getBlock().getName().getString(), textWidth), textX, y + 5,
                0xFFFFFFFF, true);
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        graphics.drawString(font, font.plainSubstrByWidth(id.toString(), textWidth), textX, y + 5 + LINE, LABEL, false);
    }
}
