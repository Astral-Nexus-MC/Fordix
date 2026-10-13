package com.omneria.fordix.client.hud;

import com.mojang.blaze3d.platform.GlUtil;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.debug.ClientboundDebugInfoPayload;
import com.omneria.fordix.debug.OreColors;
import com.omneria.fordix.debug.RangeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * The F3 text in Voidframe 2, in glass cards: World, the ores of the layer (with a height scale that follows the player), the
 * creatures that can spawn here and, with F3 + G, the slime chunks around on the left; System and Hardware on the right. What the
 * HUD panels already show (position, FPS, TPS) is left out. The sections that come from the server disappear when it switches
 * them off ({@code [hud] debugReveal}).
 */
public final class DebugHud {
    private static final int GOOD = 0xFF6FE08A;
    private static final int WARN = 0xFFF0B64C;
    private static final int BAD = 0xFFF06A5C;
    private static final int BAR_WIDTH = 130;
    private static final int LINE = 10;
    private static final int WRAP = 260;
    private static boolean takenOver;

    private DebugHud() {
    }

    /** True once F3 has been drawn by Fordix (the dev test checks it). */
    public static boolean takenOver() {
        return takenOver;
    }

    /** Whether F3 + G has the chunk borders on: the game keeps it private, but its switch gives the new state, so two switches read it. */
    public static boolean chunkBorders(Minecraft minecraft) {
        boolean flipped = minecraft.debugRenderer.switchRenderChunkborder();
        minecraft.debugRenderer.switchRenderChunkborder();
        return !flipped;
    }

    public static void setChunkBorders(Minecraft minecraft, boolean on) {
        if (chunkBorders(minecraft) != on) {
            minecraft.debugRenderer.switchRenderChunkborder();
        }
    }

    private record Seg(String text, int color) {
    }

    private interface Drawer {
        void draw(GuiGraphics g, int x, int y);
    }

    /** A row of a card: text pieces, or a block with its own drawing. */
    private record Line(List<Seg> segs, int width, int height, Drawer drawer) {
    }

    private record Card(String title, List<Line> lines) {
    }

    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.DEBUG_TEXT.type() || event.isCanceled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.renderDebug || minecraft.level == null || minecraft.player == null) {
            return;
        }
        event.setCanceled(true);
        takenOver = true;
        DebugInfoClient.tick(minecraft);
        Font font = minecraft.font;
        ClientboundDebugInfoPayload data = DebugInfoClient.data();
        boolean server = data != null && data.enabled();
        List<Card> left = new ArrayList<>();
        left.add(world(font, minecraft, server ? data : null));
        if (server) {
            Card ores = ores(font, minecraft, data);
            if (ores != null) {
                left.add(ores);
            }
            Card mobs = mobs(font, minecraft, data);
            if (mobs != null) {
                left.add(mobs);
            }
            if (chunkBorders(minecraft) && data.slimeKnown()) {
                left.add(slimes(font, data));
            }
        }
        List<Card> right = List.of(system(font), hardware(font, minecraft));
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        // One scale for both columns, so they never overlap and their text has the same size.
        float scale = Math.min(Math.min(1.0F, (height - 4.0F) / Math.max(1, columnHeight(left))), Math.min(1.0F, (height - 4.0F) / Math.max(1, columnHeight(right))));
        float spread = (width - 8.0F) / Math.max(1, columnWidth(font, left) + columnWidth(font, right) + 6);
        scale = Math.min(scale, spread);
        drawColumn(event.getGuiGraphics(), font, left, false, width, scale);
        drawColumn(event.getGuiGraphics(), font, right, true, width, scale);
    }

    // ---- cards

    private static Line text(Font font, Seg... segs) {
        int width = 0;
        for (Seg seg : segs) {
            width += font.width(seg.text());
        }
        return new Line(List.of(segs), width, LINE, null);
    }

    private static Seg key(String text) {
        return new Seg(text + " ", Glass.dim());
    }

    private static Seg value(String text) {
        return new Seg(text, FordixPalette.TEXT_BRIGHT);
    }

    private static Card world(Font font, Minecraft minecraft, ClientboundDebugInfoPayload data) {
        ClientLevel level = minecraft.level;
        BlockPos pos = minecraft.player.blockPosition();
        List<Line> lines = new ArrayList<>();
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("?");
        lines.add(text(font, key(Tr.s("debug.biome")), value(biome)));
        int sky = level.getBrightness(LightLayer.SKY, pos);
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int light = level.getMaxLocalRawBrightness(pos);
        lines.add(text(font, key(Tr.s("debug.light")), value(Tr.s("debug.light_value", light, sky, block)), new Seg("  ", 0), key(Tr.s("debug.day")), value(Long.toString(level.getDayTime() / 24000L))));
        if (data != null && data.slimeKnown()) {
            boolean slime = data.slimeAt(0, 0);
            lines.add(text(font, key(Tr.s("debug.slime_chunk")), new Seg(Tr.s(slime ? "debug.yes" : "debug.no"), slime ? GOOD : BAD),
                    new Seg("  " + Tr.s("debug.slime_hint"), Glass.dim())));
        }
        return new Card(Tr.s("debug.world"), lines);
    }

    private static String blockName(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return id;
        }
        return BuiltInRegistries.BLOCK.getOptional(location).map(block -> block.getName().getString()).orElse(id);
    }

    private static String entityName(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return id;
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(location).map(type -> ((EntityType<?>) type).getDescription().getString()).orElse(id);
    }

    /** The mod's name for something that does not come from the game itself, else an empty string. */
    private static String modOf(String id) {
        int colon = id.indexOf(':');
        if (colon <= 0 || id.startsWith("minecraft:")) {
            return "";
        }
        String namespace = id.substring(0, colon);
        return ModList.get().getModContainerById(namespace).map(container -> container.getModInfo().getDisplayName()).orElse(namespace);
    }

    /** "Minerai de fer" or "Iron Ore" become "Fer" and "Iron": the words around the name come from the language file. */
    private static String shortOreName(String name) {
        String result = name;
        for (String prefix : Tr.s("debug.ore_prefixes").split("\\|")) {
            if (!prefix.isEmpty() && result.regionMatches(true, 0, prefix, 0, prefix.length()) && result.length() > prefix.length()) {
                result = result.substring(prefix.length());
                break;
            }
        }
        for (String suffix : Tr.s("debug.ore_suffixes").split("\\|")) {
            if (!suffix.isEmpty() && result.toLowerCase(java.util.Locale.ROOT).endsWith(suffix.toLowerCase(java.util.Locale.ROOT)) && result.length() > suffix.length()) {
                result = result.substring(0, result.length() - suffix.length());
                break;
            }
        }
        return result.isEmpty() ? name : Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    /** The ores of the player's layer, one row each: a swatch, the name and a bar showing where the ore lies between the bottom and the top of the world; a white line crosses every bar at the player's height. */
    private static Card ores(Font font, Minecraft minecraft, ClientboundDebugInfoPayload data) {
        int y = minecraft.player.blockPosition().getY();
        int minY = data.minY();
        int maxY = data.maxY();
        List<ClientboundDebugInfoPayload.Ore> active = new ArrayList<>();
        int nameWidth = 0;
        for (ClientboundDebugInfoPayload.Ore ore : data.ores()) {
            if (RangeMath.contains(ore.ranges(), y)) {
                active.add(ore);
                String mod = modOf(ore.block());
                nameWidth = Math.max(nameWidth, font.width(shortOreName(blockName(ore.block()))) + (mod.isEmpty() ? 0 : font.width(" [" + mod + "]")));
            }
        }
        String none = Tr.s("debug.ores_none");
        String low = Integer.toString(minY);
        String high = Integer.toString(maxY);
        String here = Integer.toString(y);
        int barX = 11 + nameWidth + 8;
        int bodyWidth = Math.max(barX + BAR_WIDTH, font.width(none));
        int rowsHeight = Math.max(active.size(), 1) * LINE;
        Drawer drawer = (g, x, top) -> {
            if (active.isEmpty()) {
                g.drawString(font, none, x, top + 1, Glass.dim(), false);
                return;
            }
            int markerX = x + barX + (int) Math.round((1.0D - RangeMath.fraction(y, minY, maxY)) * (BAR_WIDTH - 1));
            for (int i = 0; i < active.size(); i++) {
                ClientboundDebugInfoPayload.Ore ore = active.get(i);
                int rowY = top + i * LINE;
                int color = 0xFF000000 | OreColors.of(ore.block());
                g.fill(x, rowY + 1, x + 7, rowY + 8, color);
                String name = shortOreName(blockName(ore.block()));
                g.drawString(font, name, x + 11, rowY, FordixPalette.TEXT_BRIGHT, false);
                String mod = modOf(ore.block());
                if (!mod.isEmpty()) {
                    g.drawString(font, " [" + mod + "]", x + 11 + font.width(name), rowY, Glass.dim(), false);
                }
                g.fill(x + barX, rowY + 2, x + barX + BAR_WIDTH, rowY + 7, 0x22FFFFFF);
                for (int r = 0; r + 1 < ore.ranges().length; r += 2) {
                    int x0 = x + barX + (int) Math.round((1.0D - RangeMath.fraction(ore.ranges()[r], minY, maxY)) * (BAR_WIDTH - 1));
                    int x1 = x + barX + (int) Math.round((1.0D - RangeMath.fraction(ore.ranges()[r + 1], minY, maxY)) * (BAR_WIDTH - 1)) + 1;
                    g.fill(x0, rowY + 2, Math.max(x1, x0 + 1), rowY + 7, color);
                }
            }
            g.fill(markerX, top, markerX + 1, top + active.size() * LINE, 0xFFFFFFFF);
            int axisY = top + active.size() * LINE + 1;
            g.drawString(font, low, x + barX, axisY, Glass.dim(), false);
            g.drawString(font, high, x + barX + BAR_WIDTH - font.width(high), axisY, Glass.dim(), false);
            int hereX = Math.max(x + barX + font.width(low) + 4, Math.min(markerX - font.width(here) / 2, x + barX + BAR_WIDTH - font.width(high) - font.width(here) - 4));
            g.drawString(font, here, hereX, axisY, FordixPalette.TEXT_BRIGHT, false);
        };
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(List.of(), bodyWidth, rowsHeight + (active.isEmpty() ? 0 : LINE), drawer));
        return new Card(Tr.s("debug.ores") + "  Y " + y, lines);
    }

    private static void mobLines(Font font, List<ClientboundDebugInfoPayload.Mob> mobs, List<Line> lines) {
        List<Seg> current = new ArrayList<>();
        int width = 0;
        for (ClientboundDebugInfoPayload.Mob mob : mobs) {
            List<Seg> item = new ArrayList<>();
            item.add(value(entityName(mob.entity())));
            String mod = modOf(mob.entity());
            if (!mod.isEmpty()) {
                item.add(new Seg(" [" + mod + "]", Glass.dim()));
            }
            item.add(new Seg(" " + mob.weight(), Glass.dim()));
            int itemWidth = 0;
            for (Seg seg : item) {
                itemWidth += font.width(seg.text());
            }
            if (!current.isEmpty() && width + itemWidth > WRAP) {
                lines.add(new Line(current, width, LINE, null));
                current = new ArrayList<>();
                width = 0;
            }
            if (!current.isEmpty()) {
                current.add(new Seg("  ", 0));
                width += font.width("  ");
            }
            current.addAll(item);
            width += itemWidth;
        }
        if (!current.isEmpty()) {
            lines.add(new Line(current, width, LINE, null));
        }
    }

    private static Card mobs(Font font, Minecraft minecraft, ClientboundDebugInfoPayload data) {
        if (data.friendly().isEmpty() && data.hostile().isEmpty()) {
            return null;
        }
        List<Line> lines = new ArrayList<>();
        if (!data.friendly().isEmpty()) {
            lines.add(text(font, new Seg(Tr.s("debug.friendly") + " ", GOOD), new Seg(Tr.s("debug.friendly_cond"), Glass.dim())));
            mobLines(font, data.friendly(), lines);
        }
        if (!data.hostile().isEmpty()) {
            lines.add(text(font, new Seg(Tr.s("debug.hostile") + " ", BAD), new Seg(Tr.s("debug.hostile_cond"), Glass.dim())));
            mobLines(font, data.hostile(), lines);
        }
        int light = minecraft.level.getMaxLocalRawBrightness(minecraft.player.blockPosition());
        boolean dark = light <= 0;
        lines.add(text(font, key(Tr.s("debug.hostile_now")), new Seg(Tr.s(dark ? "debug.yes" : "debug.no"), dark ? BAD : GOOD),
                new Seg("  " + Tr.s("debug.light_short", light), Glass.dim())));
        lines.add(text(font, new Seg(Tr.s("debug.weights"), Glass.dim())));
        return new Card(Tr.s("debug.mobs"), lines);
    }

    private static Card slimes(Font font, ClientboundDebugInfoPayload data) {
        int size = ClientboundDebugInfoPayload.SLIME_SIZE;
        int cell = 8;
        int span = size * cell;
        Drawer drawer = (g, x, y) -> {
            int half = size / 2;
            for (int dz = -half; dz <= half; dz++) {
                for (int dx = -half; dx <= half; dx++) {
                    int cx = x + (dx + half) * cell;
                    int cy = y + (dz + half) * cell;
                    g.fill(cx, cy, cx + cell - 1, cy + cell - 1, data.slimeAt(dx, dz) ? 0xFF3FA862 : 0x30FFFFFF);
                    if (dx == 0 && dz == 0) {
                        g.renderOutline(cx - 1, cy - 1, cell + 1, cell + 1, 0xFFFFFFFF);
                    }
                }
            }
        };
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(List.of(), span, span, drawer));
        lines.add(text(font, new Seg(Tr.s("debug.slimes_legend"), Glass.dim())));
        return new Card(Tr.s("debug.slimes_title"), lines);
    }

    private static Card system(Font font) {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = runtime.totalMemory() - runtime.freeMemory();
        int percent = (int) Math.round(used * 100.0D / Math.max(1L, max));
        int color = percent >= 85 ? BAD : percent >= 65 ? WARN : GOOD;
        List<Line> lines = new ArrayList<>();
        lines.add(text(font, new Seg(percent + "%", color), new Seg("  " + Tr.s("debug.memory", used / 1048576L, max / 1048576L), Glass.dim())));
        int barWidth = 150;
        lines.add(new Line(List.of(), barWidth, 5, (g, x, y) -> {
            g.fill(x, y + 1, x + barWidth, y + 4, 0x30FFFFFF);
            g.fill(x, y + 1, x + Math.max(1, barWidth * Math.min(100, percent) / 100), y + 4, color);
        }));
        lines.add(text(font, key(Tr.s("debug.java")), value(System.getProperty("java.version", "?") + " " + System.getProperty("os.arch", ""))));
        return new Card(Tr.s("debug.system"), lines);
    }

    private static Card hardware(Font font, Minecraft minecraft) {
        List<Line> lines = new ArrayList<>();
        lines.add(text(font, key(Tr.s("debug.cpu")), value(GlUtil.getCpuInfo())));
        lines.add(text(font, key(Tr.s("debug.gpu")), value(GlUtil.getRenderer())));
        lines.add(text(font, key(Tr.s("debug.screen")), value(minecraft.getWindow().getWidth() + "x" + minecraft.getWindow().getHeight()),
                new Seg("  ", 0), key(Tr.s("debug.opengl")), value(GlUtil.getOpenGLVersion())));
        return new Card(Tr.s("debug.hardware"), lines);
    }

    // ---- drawing

    private static int cardWidth(Font font, Card card) {
        int widest = font.width(card.title());
        for (Line line : card.lines()) {
            widest = Math.max(widest, line.width());
        }
        return widest;
    }

    private static int cardHeight(Card card) {
        int height = 14;
        for (Line line : card.lines()) {
            height += line.height();
        }
        return height + 4;
    }

    private static int columnHeight(List<Card> cards) {
        int total = 0;
        for (Card card : cards) {
            total += cardHeight(card) + 4;
        }
        return total;
    }

    private static int columnWidth(Font font, List<Card> cards) {
        int widest = 0;
        for (Card card : cards) {
            widest = Math.max(widest, cardWidth(font, card) + 18);
        }
        return widest;
    }

    private static void drawColumn(GuiGraphics g, Font font, List<Card> cards, boolean rightAligned, int screenWidth, float scale) {
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0F);
        int width = (int) (screenWidth / scale);
        int y = 2;
        for (Card card : cards) {
            int inner = cardWidth(font, card);
            int boxWidth = inner + 18;
            int boxHeight = cardHeight(card);
            int x = rightAligned ? width - 2 - boxWidth : 2;
            g.fill(x, y, x + boxWidth, y + boxHeight, 0xC4070B10);
            g.fill(x, y, x + boxWidth, y + 1, 0x24FFFFFF);
            int accentX = rightAligned ? x + boxWidth - 2 : x;
            g.fill(accentX, y, accentX + 2, y + boxHeight, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
            int tx = x + 9;
            g.drawString(font, card.title().toUpperCase(java.util.Locale.ROOT), tx, y + 5, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF), false);
            int ly = y + 17;
            for (Line line : card.lines()) {
                if (line.drawer() != null) {
                    line.drawer().draw(g, tx, ly);
                } else {
                    int sx = tx;
                    for (Seg seg : line.segs()) {
                        g.drawString(font, seg.text(), sx, ly, seg.color(), false);
                        sx += font.width(seg.text());
                    }
                }
                ly += line.height();
            }
            y += boxHeight + 4;
        }
        g.pose().popPose();
    }
}
