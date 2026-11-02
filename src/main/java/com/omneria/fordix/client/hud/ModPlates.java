package com.omneria.fordix.client.hud;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Astral Nexus plate under the HUD of other mods. What a mod draws inside its overlay is not redrawn: the plate (Voidframe
 * glass, accent edge, shadow) goes behind each separate thing the overlay shows, with the name of the mod small in a corner.
 * Where the things are is learned by drawing the overlay off screen at half size and looking at what was drawn (the same way
 * the HUD editor measures overlays), a few times a second and one overlay at a time, so a frame is not slowed by it. The server
 * decides whether plates are drawn ({@link HudRules#modPlates()}); there is no client option.
 */
public final class ModPlates {
    private static final int DOWN = 2;
    private static final int ALPHA_THRESHOLD = 16;
    /** The size of a cell of the grouping grid, in GUI pixels. */
    private static final int CELL = 6;
    private static final long EVERY_MS = 250L;
    private static final long STALE_MS = 900L;
    private static final int PAD = 4;
    private static final int MAX_GROUPS = 6;

    private record Seen(List<int[]> boxes, long at) {
    }

    private static final Map<ResourceLocation, Seen> SEEN = new HashMap<>();
    private static final Map<ResourceLocation, Long> NEXT = new HashMap<>();
    private static final Map<String, String> NAMES = new HashMap<>();
    private static long lastMeasure;
    private static TextureTarget target;
    private static NativeImage image;
    private static boolean failed;

    private ModPlates() {
    }

    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (failed || !HudRules.modPlates() || !FordixConfig.voidframeHud()) {
            return;
        }
        NamedGuiOverlay overlay = event.getOverlay();
        ResourceLocation id = overlay.id();
        String namespace = id.getNamespace();
        if (namespace.equals("minecraft") || namespace.equals("fordix")) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !(minecraft.gui instanceof ForgeGui gui)) {
            return;
        }
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();
        GuiGraphics g = event.getGuiGraphics();
        if (now - lastMeasure >= 12L && now >= NEXT.getOrDefault(id, 0L)) {
            lastMeasure = now;
            NEXT.put(id, now + EVERY_MS);
            g.flush();
            List<int[]> boxes = measure(minecraft, gui, overlay, width, height);
            if (boxes == null || boxes.isEmpty()) {
                SEEN.remove(id);
            } else {
                SEEN.put(id, new Seen(boxes, now));
            }
        }
        Seen seen = SEEN.get(id);
        if (seen == null || now - seen.at() > STALE_MS) {
            return;
        }
        Font font = minecraft.font;
        String label = modName(namespace).toUpperCase(Locale.ROOT);
        for (int[] box : seen.boxes()) {
            int x = box[0] - PAD;
            int y = box[1] - PAD + 1;
            int w = box[2] + 2 * PAD;
            int h = box[3] + 2 * PAD - 2;
            HudStyle.panel(g, x, y, w, h, FordixPalette.ACCENT & 0x00FFFFFF, 205);
            int labelY = y > 10 ? y - 9 : y + h + 1;
            g.drawString(font, label, x + Math.max(0, w - font.width(label)), labelY, Glass.dim(), false);
        }
    }

    private static String modName(String namespace) {
        return NAMES.computeIfAbsent(namespace, ns -> ModList.get().getModContainerById(ns)
                .map(container -> container.getModInfo().getDisplayName()).orElse(ns));
    }

    /** What the overlay draws, as boxes {x, y, width, height} in GUI pixels; null when it cannot be measured. */
    private static List<int[]> measure(Minecraft minecraft, ForgeGui gui, NamedGuiOverlay overlay, int guiWidth, int guiHeight) {
        Window window = minecraft.getWindow();
        int w = Math.max(1, window.getWidth() / DOWN);
        int h = Math.max(1, window.getHeight() / DOWN);
        try {
            if (target == null || image == null || image.getWidth() != w || image.getHeight() != h) {
                close();
                target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
                target.setClearColor(0F, 0F, 0F, 0F);
                image = new NativeImage(w, h, false);
            }
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            gui.setupOverlayRenderState(true, false);
            GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            overlay.overlay().render(gui, graphics, minecraft.getPartialTick(), guiWidth, guiHeight);
            graphics.flush();
            RenderSystem.bindTexture(target.getColorTextureId());
            image.downloadTexture(0, false);
        } catch (RuntimeException | LinkageError e) {
            Fordix.LOGGER.warn("Fordix: mod plates are off, an overlay could not be measured: {}", e.toString());
            failed = true;
            return null;
        } finally {
            minecraft.getMainRenderTarget().bindWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
        double scale = window.getGuiScale() / DOWN;
        int columns = (guiWidth + CELL - 1) / CELL;
        int rows = (guiHeight + CELL - 1) / CELL;
        boolean[] used = new boolean[columns * rows];
        int[] pixels = image.makePixelArray();
        for (int row = 0; row < h; row++) {
            int top = h - 1 - row; // GL reads the texture bottom to top
            int cellRow = Math.min(rows - 1, (int) (top / scale) / CELL);
            for (int column = 0; column < w; column++) {
                if ((pixels[row * w + column] >>> 24) > ALPHA_THRESHOLD) {
                    used[cellRow * columns + Math.min(columns - 1, (int) (column / scale) / CELL)] = true;
                }
            }
        }
        List<int[]> boxes = new ArrayList<>();
        for (int[] group : PlateClusters.find(used, columns, rows)) {
            int x = group[0] * CELL;
            int y = group[1] * CELL;
            int bw = Math.min(guiWidth, (group[2] + 1) * CELL) - x;
            int bh = Math.min(guiHeight, (group[3] + 1) * CELL) - y;
            // A thing that covers much of the screen is an effect (a tint, a vignette), not a panel.
            if ((long) bw * bh > 0.35 * guiWidth * guiHeight) {
                continue;
            }
            boxes.add(new int[]{x, y, bw, bh});
            if (boxes.size() >= MAX_GROUPS) {
                break;
            }
        }
        return boxes;
    }

    private static void close() {
        if (image != null) {
            image.close();
            image = null;
        }
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
