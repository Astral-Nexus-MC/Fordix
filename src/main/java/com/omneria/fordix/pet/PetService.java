package com.omneria.fordix.pet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Carries out the wheel's actions on a tamed creature, for its owner only and only when it is close (the wheel opens on a
 * creature the player is looking at). Sitting, following, the horse inventory, calling it back and setting it free.
 */
public final class PetService {
    private static final double MAX_DISTANCE = 16.0D;

    private PetService() {
    }

    public static void handle(ServerboundPetActionPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            PetAction action = PetAction.byOrdinal(payload.action());
            if (player != null && action != null) {
                Entity entity = player.level().getEntity(payload.entityId());
                if (entity instanceof LivingEntity pet && ownedBy(pet, player) && player.distanceToSqr(pet) <= MAX_DISTANCE * MAX_DISTANCE) {
                    run(player, pet, action);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static boolean ownedBy(Entity entity, net.minecraft.world.entity.player.Player player) {
        return entity instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID());
    }

    private static void run(ServerPlayer player, LivingEntity pet, PetAction action) {
        switch (action) {
            case FOLLOW -> {
                // Stands up if it was sitting, then walks to the player (a standing wolf only follows from 10 blocks away).
                if (pet instanceof TamableAnimal animal) {
                    animal.setOrderedToSit(false);
                    animal.setInSittingPose(false);
                }
                if (pet instanceof net.minecraft.world.entity.Mob mob) {
                    mob.getNavigation().moveTo(player, 1.3D);
                }
            }
            case STAY -> {
                if (pet instanceof TamableAnimal animal) {
                    animal.setOrderedToSit(true);
                    animal.setInSittingPose(true);
                    animal.getNavigation().stop();
                    animal.setTarget(null);
                }
            }
            case INVENTORY -> {
                if (pet instanceof AbstractHorse horse && horse.isTamed()) {
                    horse.openCustomInventoryScreen(player);
                }
            }
            case RECALL -> {
                Vec3 look = player.getLookAngle();
                Vec3 flat = new Vec3(look.x, 0, look.z);
                Vec3 spot = player.position().add(flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(1.5D));
                if (pet instanceof net.minecraft.world.entity.Mob mob) {
                    mob.getNavigation().stop();
                }
                pet.teleportTo(spot.x, spot.y, spot.z);
            }
            case RELEASE -> {
                if (pet instanceof TamableAnimal animal) {
                    animal.setTame(false);
                    animal.setOwnerUUID(null);
                    animal.setOrderedToSit(false);
                    animal.setInSittingPose(false);
                } else if (pet instanceof AbstractHorse horse) {
                    horse.setTamed(false);
                    horse.setOwnerUUID(null);
                }
            }
        }
    }
}
