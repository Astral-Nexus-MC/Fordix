package com.omneria.fordix.client.hud;

import com.omneria.fordix.FordixConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The creature the player is looking at, from further than arm's length: the game only picks what is within reach (3 blocks),
 * which for a hostile creature is too late to read its panel or open its sheet. The ray stops at the first block, so nothing
 * is targeted through a wall. The distance is the option {@code hudTargetRange}.
 */
public final class FarTarget {
    private FarTarget() {
    }

    /** What the game itself targets if it is an entity, else the nearest living creature in the line of sight, or null. */
    public static EntityHitResult entity(Minecraft minecraft) {
        if (minecraft.hitResult instanceof EntityHitResult hit) {
            return hit;
        }
        Entity viewer = minecraft.getCameraEntity();
        int range = com.omneria.fordix.client.hud.HudRules.targetRange();
        if (viewer == null || minecraft.level == null || range <= 4) {
            return null;
        }
        Vec3 eye = viewer.getEyePosition(1.0F);
        Vec3 look = viewer.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));
        AABB area = viewer.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(viewer, eye, end, area,
                other -> !other.isSpectator() && other.isPickable() && other instanceof LivingEntity && other != viewer,
                (double) range * range);
        if (hit == null) {
            return null;
        }
        HitResult wall = minecraft.level.clip(new ClipContext(eye, hit.getLocation(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, viewer));
        return wall.getType() == HitResult.Type.MISS ? hit : null;
    }
}
