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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
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

    /** The chat's text field: not an overlay but part of the chat screen, so it has its own slot. */
    public static final String CHAT_INPUT = "fordix:chat_input";
    private static final java.lang.reflect.Field CHAT_INPUT_FIELD = Reflect.uniqueField(ChatScreen.class, EditBox.class);
    private static boolean chatShifted;
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
            case "item_name" -> new HudSnap.Rect(center - 80, height - 74, 160, 22);
            case "chat_input" -> new HudSnap.Rect(2, height - 14, Math.max(1, width - 4), 12);
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
        HudSnap.Rect base = defaults(id.toString(), width, height);
        HudSnap.Rect placed = slot.placed(base, width, height);
        float scale = slot.scaleValue();
        if (placed.x() != base.x() || placed.y() != base.y() || scale != 1.0F) {
            // Drawn at its default spot: move to the placed corner, size it from there, come back to draw as usual.
            var pose = event.getGuiGraphics().pose();
            pose.pushPose();
            pose.translate(placed.x(), placed.y(), 0.0F);
            pose.scale(scale, scale, 1.0F);
            pose.translate(-base.x(), -base.y(), 0.0F);
            shifted.add(id);
        }
    }

    /** Lowest priority: other listeners drawing on this overlay (the hotbar's rarity frames) move with it. */
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (shifted.remove(event.getOverlay().id())) {
            event.getGuiGraphics().pose().popPose();
        }
    }

    // ---- the chat's text field (drawn by the chat screen)

    /** Highest priority: shifts and sizes the whole chat screen, which draws only the text field, its bar and suggestions. */
    public static void onChatRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen screen)) {
            return;
        }
        HudSlot slot = slots().get(CHAT_INPUT);
        if (slot == null || !slot.customized()) {
            return;
        }
        HudSnap.Rect base = defaults(CHAT_INPUT, screen.width, screen.height);
        HudSnap.Rect placed = slot.placed(base, screen.width, screen.height);
        float scale = slot.scaleValue();
        if (placed.x() != base.x() || placed.y() != base.y() || scale != 1.0F) {
            var pose = event.getGuiGraphics().pose();
            pose.pushPose();
            pose.translate(placed.x(), placed.y(), 0.0F);
            pose.scale(scale, scale, 1.0F);
            pose.translate(-base.x(), -base.y(), 0.0F);
            chatShifted = true;
        }
    }

    public static void onChatRenderPost(ScreenEvent.Render.Post event) {
        if (chatShifted && event.getScreen() instanceof ChatScreen) {
            chatShifted = false;
            event.getGuiGraphics().pose().popPose();
        }
    }

    /** A click on the moved text field is handed to it in its own coordinates, so the caret lands where you clicked. */
    public static void onChatMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen screen) || CHAT_INPUT_FIELD == null) {
            return;
        }
        HudSlot slot = slots().get(CHAT_INPUT);
        if (slot == null || !slot.customized()) {
            return;
        }
        HudSnap.Rect base = defaults(CHAT_INPUT, screen.width, screen.height);
        HudSnap.Rect placed = slot.placed(base, screen.width, screen.height);
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        if (mouseX < placed.x() || mouseX >= placed.right() || mouseY < placed.y() || mouseY >= placed.bottom()
                || !(Reflect.get(CHAT_INPUT_FIELD, screen) instanceof EditBox input)) {
            return;
        }
        float scale = slot.scaleValue();
        input.mouseClicked((mouseX - placed.x()) / scale + base.x(), (mouseY - placed.y()) / scale + base.y(), event.getButton());
        event.setCanceled(true);
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
        ids.add(CHAT_INPUT);
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
