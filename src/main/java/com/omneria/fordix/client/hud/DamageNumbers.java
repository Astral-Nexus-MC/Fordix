package com.omneria.fordix.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.omneria.fordix.FordixConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Floating numbers where health changes: red for damage, green for healing, bigger for heavy hits, drifting up and fading.
 * The client never sees "who hit whom", so this reads the health of every creature near the camera each tick and writes
 * the difference; the player's own damage shows in front of the camera, where it can be seen in first person.
 */
public final class DamageNumbers {
    private static final double RANGE = 32.0D;
    private static final int LIFETIME = 28;
    private static final int MAX_NUMBERS = 40;
    private static final float MIN_HEAL = 2.0F;
    private static final DamageTracker TRACKER = new DamageTracker();
    private static final List<Floating> NUMBERS = new ArrayList<>();
    private static long tick;

    private record Floating(Vec3 origin, String text, int rgb, float scale, long born, float drift) {
    }

    private DamageNumbers() {
    }

    public static void reset() {
        TRACKER.clear();
        NUMBERS.clear();
    }

    private static boolean enabled() {
        try {
            return FordixConfig.DAMAGE_NUMBERS.get() && FordixConfig.VOIDFRAME_HUD.get();
        } catch (IllegalStateException notLoadedYet) {
            return false;
        }
    }

    /** Every client tick. */
    public static void tick(Minecraft minecraft) {
        tick++;
        if (minecraft.level == null || minecraft.player == null || !enabled()) {
            if (!NUMBERS.isEmpty() || !enabled()) {
                NUMBERS.clear();
                TRACKER.clear();
            }
            return;
        }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Set<Integer> alive = new HashSet<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand || entity.position().distanceToSqr(camera) > RANGE * RANGE) {
                continue;
            }
            alive.add(entity.getId());
            float change = TRACKER.update(entity.getId(), living.getHealth());
            if (change < -0.4F || change >= MIN_HEAL && entity != minecraft.player) {
                spawn(minecraft, living, change);
            }
        }
        TRACKER.retain(alive);
        for (Iterator<Floating> it = NUMBERS.iterator(); it.hasNext(); ) {
            if (tick - it.next().born() > LIFETIME) {
                it.remove();
            }
        }
    }

    private static void spawn(Minecraft minecraft, LivingEntity living, float change) {
        boolean damage = change < 0;
        Vec3 origin;
        if (living == minecraft.player) {
            Vec3 look = living.getLookAngle();
            origin = living.getEyePosition().add(look.scale(1.4D)).add(0.0D, -0.35D, 0.0D);
        } else {
            origin = new Vec3(living.getX(), living.getY() + living.getBbHeight() + 0.25D, living.getZ());
        }
        float drift = (minecraft.level.random.nextFloat() - 0.5F) * 0.5F;
        origin = origin.add(drift, 0.0D, drift * 0.6D);
        float amount = Math.abs(change);
        int rgb = !damage ? 0x55FF7A : amount >= 10.0F ? 0xFFD24A : 0xFF5B5B;
        float scale = Math.min(1.8F, 1.0F + amount / 20.0F);
        if (NUMBERS.size() >= MAX_NUMBERS) {
            NUMBERS.remove(0);
        }
        NUMBERS.add(new Floating(origin, (damage ? "-" : "+") + DamageTracker.format(amount), rgb, scale, tick, drift));
    }

    /** After the particles: draws each number as a camera-facing label. */
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || NUMBERS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 view = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        Font font = minecraft.font;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        for (Floating number : NUMBERS) {
            float age = tick - number.born() + partial;
            float progress = Math.min(1.0F, age / LIFETIME);
            int alpha = progress < 0.65F ? 255 : (int) (255 * (1.0F - (progress - 0.65F) / 0.35F));
            if (alpha < 8) {
                continue;
            }
            double rise = 0.7D * (1.0D - Math.pow(1.0D - progress, 2));
            pose.pushPose();
            pose.translate(number.origin().x - view.x, number.origin().y + rise - view.y, number.origin().z - view.z);
            pose.mulPose(camera.rotation());
            float size = 0.03F * number.scale();
            pose.scale(-size, -size, size);
            font.drawInBatch(number.text(), -font.width(number.text()) / 2.0F, 0.0F, (alpha << 24) | number.rgb(), true,
                    pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
