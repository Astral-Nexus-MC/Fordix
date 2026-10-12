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
    private static final int RULER_WIDTH = 14;
    private static final int RULER_HEIGHT = 96;
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

    private static Card ores(Font font, Minecraft minecraft, ClientboundDebugInfoPayload data) {
        int y = minecraft.player.blockPosition().getY();
        String and = Tr.s("debug.and");
        String to = Tr.s("debug.to");
        List<ClientboundDebugInfoPayload.Ore> all = data.ores();
        List<List<Seg>> rows = new ArrayList<>();
        List<Integer> rowColors = new ArrayList<>();
        int widest = 0;
        for (ClientboundDebugInfoPayload.Ore ore : all) {
            if (!RangeMath.contains(ore.ranges(), y)) {
                continue;
            }
            List<Seg> segs = new ArrayList<>();
            segs.add(value(blockName(ore.block())));
            String mod = modOf(ore.block());
            if (!mod.isEmpty()) {
                segs.add(new Seg(" [" + mod + "]", Glass.dim()));
            }
            segs.add(new Seg("  " + Tr.s("debug.ore_range", RangeMath.describe(ore.ranges(), and, to)), Glass.dim()));
            int width = 0;
            for (Seg seg : segs) {
                width += font.width(seg.text());
            }
            widest = Math.max(widest, width);
            rows.add(segs);
            rowColors.add(0xFF000000 | OreColors.of(ore.block()));
        }
        int minY = data.minY();
        int maxY = data.maxY();
        String scale = Tr.s("debug.scale", maxY, minY);
        int bodyHeight = Math.max(RULER_HEIGHT, Math.max(rows.size(), 1) * LINE);
        int listX = RULER_WIDTH + 8;
        String none = Tr.s("debug.ores_none");
        int listWidth = rows.isEmpty() ? font.width(none) : widest + 12;
        int bodyWidth = Math.max(listX + listWidth, font.width(scale));
        Drawer drawer = (g, x, top) -> {
            g.fill(x, top, x + RULER_WIDTH, top + RULER_HEIGHT, 0x22FFFFFF);
            int half = RULER_WIDTH / 2;
            for (int i = 0; i < all.size(); i++) {
                ClientboundDebugInfoPayload.Ore ore = all.get(i);
                boolean active = RangeMath.contains(ore.ranges(), y);
                int color = (active ? 0xFF000000 : 0x40000000) | OreColors.of(ore.block());
                int x0 = x + (i % 2 == 0 ? 0 : half);
                int x1 = x0 + half;
                for (int r = 0; r + 1 < ore.ranges().length; r += 2) {
                    int top0 = top + (int) Math.round(RangeMath.fraction(ore.ranges()[r + 1], minY, maxY) * (RULER_HEIGHT - 1));
                    int bottom0 = top + (int) Math.round(RangeMath.fraction(ore.ranges()[r], minY, maxY) * (RULER_HEIGHT - 1)) + 1;
                    g.fill(x0, top0, x1, Math.max(bottom0, top0 + 1), color);
                }
            }
            int marker = top + (int) Math.round(RangeMath.fraction(y, minY, maxY) * (RULER_HEIGHT - 1));
            g.fill(x - 2, marker, x + RULER_WIDTH + 2, marker + 1, 0xFFFFFFFF);
            if (rows.isEmpty()) {
                g.drawString(font, none, x + listX, top + 1, Glass.dim(), false);
            }
            for (int i = 0; i < rows.size(); i++) {
                int rowY = top + i * LINE;
                g.fill(x + listX, rowY + 1, x + listX + 7, rowY + 8, rowColors.get(i));
                int tx = x + listX + 11;
                for (Seg seg : rows.get(i)) {
                    g.drawString(font, seg.text(), tx, rowY, seg.color(), false);
                    tx += font.width(seg.text());
                }
            }
        };
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(List.of(), bodyWidth, bodyHeight, drawer));
        lines.add(text(font, new Seg(scale, Glass.dim())));
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
