package com.omneria.fordix.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.vanilla.VoidframeGauges;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Feeds {@link HudContext} with what the player is doing and fades the information panels accordingly (the option
 * {@code autoHudContext}, off by default). A locked context stays until unlocked.
 */
public final class HudContextTicker {
    private static final HudContext CONTEXT = new HudContext();
    private static final Map<String, Float> SHOWN = new HashMap<>();
    private static HudContext.Kind locked;
    private static long lastFrame;

    private static HudContextMatrix matrix;

    private HudContextTicker() {
    }

    /** The player's table of situations, loaded once. */
    public static HudContextMatrix matrix() {
        if (matrix == null) {
            matrix = new HudContextMatrix(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("fordix-hud-contexts.json"));
            HudContext.useMatrix(matrix);
        }
        return matrix;
    }

    public static boolean enabled() {
        return FordixConfig.AUTO_HUD_CONTEXT.get();
    }

    public static HudContext.Kind kind() {
        return locked != null ? locked : CONTEXT.current();
    }

    /** Every client tick. */
    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !enabled()) {
            return;
        }
        boolean fighting = player.tickCount - player.getLastHurtMobTimestamp() < 40
                || player.tickCount - player.getLastHurtByMobTimestamp() < 40;
        boolean mining = minecraft.options.keyAttack.isDown() && minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
        CONTEXT.update(System.currentTimeMillis(),
                new HudContext.Signals(VoidframeGauges.bossActive(minecraft), player.getVehicle() != null, fighting, mining));
    }

    /** The lock key: freezes the situation as it is, or lets it follow the player again. */
    public static void toggleLock(Minecraft minecraft) {
        locked = locked == null ? CONTEXT.current() : null;
        minecraft.gui.setOverlayMessage(Component.literal(Tr.s(locked == null ? "hud.context.unlocked" : "hud.context.locked",
                Tr.s("hud.context." + kind().name().toLowerCase(java.util.Locale.ROOT)))), false);
    }

    /** Opacity multiplier for a panel, eased over 200 ms so a change of situation is a fade and never a jump. */
    public static float factor(String element) {
        matrix();
        float target = enabled() ? HudContext.factor(kind(), element) : 1.0F;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - lastFrame) / 1000.0F);
        float shown = SHOWN.getOrDefault(element, target);
        float step = dt / (HudTokens.FADE_MS / 1000.0F);
        shown = shown < target ? Math.min(target, shown + step) : Math.max(target, shown - step);
        SHOWN.put(element, shown);
        return shown;
    }

    /** Marks the end of the frame for the easing above (call from one overlay per frame is enough: time is wall clock). */
    private static void stamp() {
        lastFrame = System.currentTimeMillis();
    }

    /** Wraps a panel overlay so it is drawn at the opacity the current situation asks for. */
    public static IGuiOverlay fade(String element, IGuiOverlay overlay) {
        return (gui, graphics, partialTick, width, height) -> {
            float factor = factor(element);
            stamp();
            if (factor <= 0.01F) {
                return;
            }
            if (factor >= 0.99F) {
                overlay.render(gui, graphics, partialTick, width, height);
                return;
            }
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, factor);
            try {
                overlay.render(gui, graphics, partialTick, width, height);
                graphics.flush();
            } finally {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        };
    }
}
