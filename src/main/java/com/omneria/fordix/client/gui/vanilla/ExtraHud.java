package com.omneria.fordix.client.gui.vanilla;

import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.client.gui.layout.HudSlot;
import com.omneria.fordix.client.gui.layout.HudSnap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.GuiOverlayManager;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Moves and hides the HUD that Fordix doesn't draw itself: the vanilla chat, hotbar, scoreboard, effects, titles...
 * and every overlay another mod registers with Forge. Each overlay is wrapped by its Forge render events: shifted
 * by a translation of the pose, or cancelled when hidden. Placements live in {@code fordix-hud-extras.json} (the
 * client config can't list overlays that aren't known at startup). The layout screen asks {@link #discover} for
 * what's available: every overlay is drawn once off screen to find out where it sits and how big it is.
 */
public final class ExtraHud {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int ALPHA_THRESHOLD = 16;

    /** Vanilla overlays worth moving; every other vanilla one is either a Fordix element or a full-screen effect. */
    private static final List<ResourceLocation> MOVABLE_VANILLA = List.of(
            VanillaGuiOverlay.HOTBAR.id(), VanillaGuiOverlay.CHAT_PANEL.id(), VanillaGuiOverlay.SCOREBOARD.id(),
            VanillaGuiOverlay.POTION_ICONS.id(), VanillaGuiOverlay.ITEM_NAME.id(), VanillaGuiOverlay.TITLE_TEXT.id(),
            VanillaGuiOverlay.SUBTITLES.id(), VanillaGuiOverlay.RECORD_OVERLAY.id());

    private static Map<String, HudSlot> slots;
    private static final Set<ResourceLocation> shifted = new HashSet<>();

    private ExtraHud() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fordix-hud-extras.json");
    }

    private static synchronized Map<String, HudSlot> slots() {
        if (slots == null) {
            slots = new LinkedHashMap<>();
            Path file = file();
            if (Files.isRegularFile(file)) {
                try {
                    Map<String, HudSlot> read = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                            new TypeToken<LinkedHashMap<String, HudSlot>>() { }.getType());
                    if (read != null) {
                        slots.putAll(read);
                    }
                } catch (IOException | RuntimeException e) {
                    Fordix.LOGGER.warn("Could not read {}: {}", file, e.toString());
                }
            }
        }
        return slots;
    }

    public static synchronized HudSlot slot(String id) {
        return slots().computeIfAbsent(id, key -> new HudSlot());
    }

    public static synchronized void save() {
        try {
            Files.writeString(file(), GSON.toJson(slots()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Fordix.LOGGER.warn("Could not write {}: {}", file(), e.toString());
        }
    }

    /** Where an overlay sits by default for this screen: measured when known, otherwise a guess from vanilla. */
    public static HudSnap.Rect defaults(String id, int width, int height) {
        HudSlot slot = slot(id);
        return slot.measured ? slot.base(width, height) : fallback(id, width, height);
    }

    public static HudSnap.Rect fallback(String id, int width, int height) {
        int center = width / 2;
        String path = id.substring(id.indexOf(':') + 1);
        return switch (path) {
            case "hotbar" -> new HudSnap.Rect(center - 91, height - 22, 182, 22);
            case "chat_panel" -> new HudSnap.Rect(0, height - 130, 320, 90);
            case "scoreboard" -> new HudSnap.Rect(width - 100, height / 2 - 30, 100, 60);
            case "potion_icons" -> new HudSnap.Rect(width - 25, 1, 24, 24);
            case "item_name" -> new HudSnap.Rect(center - 60, height - 72, 120, 10);
            case "title_text" -> new HudSnap.Rect(center - 80, height / 2 - 30, 160, 40);
            case "subtitles" -> new HudSnap.Rect(width - 120, height - 70, 118, 30);
            case "record_overlay" -> new HudSnap.Rect(center - 80, height - 68, 160, 12);
            default -> new HudSnap.Rect(center - 50, 60, 100, 20);
        };
    }

    // ---- Forge render events

    /** Highest priority, so a later listener that cancels the overlay still gets its pose pushed (and popped by the Post event). */
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        HudSlot slot = slots().get(id.toString());
        if (slot == null || !slot.customized()) {
            return;
        }
        if (slot.hidden) {
            event.setCanceled(true);
            return;
        }
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int[] delta = slot.delta(defaults(id.toString(), width, height), width, height);
        if (delta[0] != 0 || delta[1] != 0) {
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(delta[0], delta[1], 0.0F);
            shifted.add(id);
        }
    }

    /** Lowest priority: other listeners drawing on this overlay (the hotbar's rarity frames) move with it. */
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (shifted.remove(event.getOverlay().id())) {
            event.getGuiGraphics().pose().popPose();
        }
    }

    // ---- discovery

    /**
     * The overlays the layout screen lists: the movable vanilla ones and every mod overlay that actually draws
     * something small enough to be a HUD element. Must run on the render thread, inside a GUI render pass.
     */
    public static List<String> discover(int guiWidth, int guiHeight) {
        List<String> ids = new ArrayList<>();
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui instanceof ForgeGui gui)) {
            return ids;
        }
        Window window = minecraft.getWindow();
        RenderTarget main = minecraft.getMainRenderTarget();
        TextureTarget target = null;
        NativeImage image = null;
        try {
            target = new TextureTarget(window.getWidth(), window.getHeight(), true, Minecraft.ON_OSX);
            target.setClearColor(0F, 0F, 0F, 0F);
            image = new NativeImage(window.getWidth(), window.getHeight(), false);
            for (NamedGuiOverlay named : GuiOverlayManager.getOverlays()) {
                ResourceLocation id = named.id();
                boolean vanilla = id.getNamespace().equals("minecraft");
                if (vanilla && !MOVABLE_VANILLA.contains(id)) {
                    continue;
                }
                HudSnap.Rect rect = measure(minecraft, gui, named, target, image, guiWidth, guiHeight);
                if (!vanilla && (rect == null || rect.w() * rect.h() > 0.6 * guiWidth * guiHeight)) {
                    continue;
                }
                if (rect != null) {
                    slot(id.toString()).setBase(rect, guiWidth, guiHeight);
                }
                ids.add(id.toString());
            }
        } catch (RuntimeException | LinkageError e) {
            Fordix.LOGGER.warn("HUD overlay discovery failed: {}", e.toString());
        } finally {
            if (image != null) {
                image.close();
            }
            if (target != null) {
                target.destroyBuffers();
            }
            main.bindWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
        return ids;
    }

    /** Draws the overlay alone into the off-screen target and returns the box of what it drew, in scaled GUI pixels. */
    private static HudSnap.Rect measure(Minecraft minecraft, ForgeGui gui, NamedGuiOverlay named, TextureTarget target,
                                        NativeImage image, int guiWidth, int guiHeight) {
        Window window = minecraft.getWindow();
        try {
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            gui.setupOverlayRenderState(true, false);
            GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            named.overlay().render(gui, graphics, minecraft.getPartialTick(), guiWidth, guiHeight);
            graphics.flush();
            RenderSystem.bindTexture(target.getColorTextureId());
            image.downloadTexture(0, false);
        } catch (RuntimeException | LinkageError e) {
            Fordix.LOGGER.debug("Could not measure overlay {}: {}", named.id(), e.toString());
            return null;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.makePixelArray();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for (int row = 0; row < height; row++) {
            int top = height - 1 - row; // GL reads the texture bottom to top
            for (int column = 0; column < width; column++) {
                if ((pixels[row * width + column] >>> 24) > ALPHA_THRESHOLD) {
                    minX = Math.min(minX, column);
                    maxX = Math.max(maxX, column);
                    minY = Math.min(minY, top);
                    maxY = Math.max(maxY, top);
                }
            }
        }
        if (maxX < 0) {
            return null;
        }
        double scale = window.getGuiScale();
        int x = (int) Math.floor(minX / scale);
        int y = (int) Math.floor(minY / scale);
        int right = (int) Math.ceil((maxX + 1) / scale);
        int bottom = (int) Math.ceil((maxY + 1) / scale);
        return new HudSnap.Rect(x, y, Math.max(1, right - x), Math.max(1, bottom - y));
    }
}
