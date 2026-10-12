package com.omneria.fordix.debug;

import com.omneria.fordix.Fordix;
import com.omneria.fordix.FordixServerConfig;
import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Answers the F3 screen: the ores of the biome the player is in with their height ranges (read from the world generation, so mods
 * count), the creatures that can spawn there and the slime chunks around. The slime chunks are computed here because the client
 * does not know the world seed, and only a yes or no per chunk leaves. {@code [hud] debugReveal = false} switches all of it off.
 */
public final class DebugInfoService {
    private static final long MIN_INTERVAL_TICKS = 10L;
    private static final long SLIME_SALT = 987234911L;
    private static final Map<UUID, Long> LAST_ANSWER = new HashMap<>();

    private DebugInfoService() {
    }

    public static void handle(ServerboundDebugInfoPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                answer(player, payload.slimes());
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void answer(ServerPlayer player, boolean slimes) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long last = LAST_ANSWER.get(player.getUUID());
        if (last != null && now - last < MIN_INTERVAL_TICKS && now >= last) {
            return;
        }
        LAST_ANSWER.put(player.getUUID(), now);
        ClientboundDebugInfoPayload reply;
        if (!FordixServerConfig.HUD_DEBUG_REVEAL.get()) {
            reply = ClientboundDebugInfoPayload.disabled();
        } else {
            try {
                reply = build(player, level, slimes);
            } catch (RuntimeException e) {
                Fordix.LOGGER.warn("Fordix: cannot read the world generation for the F3 screen: {}", e.toString());
                reply = ClientboundDebugInfoPayload.disabled();
            }
        }
        PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), reply);
    }

    private static ClientboundDebugInfoPayload build(ServerPlayer player, ServerLevel level, boolean slimes) {
        Holder<Biome> holder = level.getBiome(player.blockPosition());
        Biome biome = holder.value();
        String biomeId = holder.unwrapKey().map(key -> key.location().toString()).orElse("");
        boolean slimeKnown = false;
        long bits = 0L;
        if (slimes && level.dimension() == Level.OVERWORLD) {
            slimeKnown = true;
            ChunkPos here = player.chunkPosition();
            int half = ClientboundDebugInfoPayload.SLIME_SIZE / 2;
            for (int dz = -half; dz <= half; dz++) {
                for (int dx = -half; dx <= half; dx++) {
                    if (WorldgenRandom.seedSlimeChunk(here.x + dx, here.z + dz, level.getSeed(), SLIME_SALT).nextInt(10) == 0) {
                        bits |= 1L << ((dz + half) * ClientboundDebugInfoPayload.SLIME_SIZE + (dx + half));
                    }
                }
            }
        }
        return new ClientboundDebugInfoPayload(true, level.dimension().location().toString(), biomeId, level.getMinBuildHeight(),
                level.getMaxBuildHeight() - 1, slimeKnown, bits, ores(level, biome), mobs(biome, MobCategory.CREATURE), mobs(biome, MobCategory.MONSTER));
    }

    private static List<ClientboundDebugInfoPayload.Mob> mobs(Biome biome, MobCategory category) {
        List<ClientboundDebugInfoPayload.Mob> out = new ArrayList<>();
        for (MobSpawnSettings.SpawnerData data : biome.getMobSettings().getMobs(category).unwrap()) {
            var key = BuiltInRegistries.ENTITY_TYPE.getKey(data.type);
            if (key != null) {
                out.add(new ClientboundDebugInfoPayload.Mob(key.toString(), data.getWeight().asInt()));
            }
        }
        out.sort((a, b) -> Integer.compare(b.weight(), a.weight()));
        return out;
    }

    private static List<ClientboundDebugInfoPayload.Ore> ores(ServerLevel level, Biome biome) {
        WorldGenerationContext context = new WorldGenerationContext(level.getChunkSource().getGenerator(), level);
        Map<String, List<Integer>> byBlock = new LinkedHashMap<>();
        for (HolderSet<PlacedFeature> step : biome.getGenerationSettings().features()) {
            for (Holder<PlacedFeature> holder : step) {
                try {
                    collect(holder.value(), context, byBlock);
                } catch (RuntimeException | ReflectiveOperationException e) {
                    // a feature that is not read the usual way is left out
                }
            }
        }
        List<ClientboundDebugInfoPayload.Ore> out = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : byBlock.entrySet()) {
            int[] flat = new int[entry.getValue().size()];
            for (int i = 0; i < flat.length; i++) {
                flat[i] = entry.getValue().get(i);
            }
            out.add(new ClientboundDebugInfoPayload.Ore(entry.getKey(), RangeMath.merge(flat)));
        }
        return out;
    }

    private static void collect(PlacedFeature placed, WorldGenerationContext context, Map<String, List<Integer>> byBlock) throws ReflectiveOperationException {
        if (!(placed.feature().value().config() instanceof OreConfiguration ore)) {
            return;
        }
        String block = null;
        for (OreConfiguration.TargetBlockState target : ore.targetStates) {
            BlockState state = target.state;
            var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (state.is(Tags.Blocks.ORES) || key.getPath().endsWith("_ore") || key.getPath().contains("debris")) {
                block = key.toString();
                break;
            }
        }
        if (block == null) {
            return;
        }
        for (PlacementModifier modifier : placed.placement()) {
            if (modifier instanceof HeightRangePlacement range) {
                int[] bounds = bounds(range, context);
                if (bounds != null) {
                    List<Integer> list = byBlock.computeIfAbsent(block, name -> new ArrayList<>());
                    list.add(bounds[0]);
                    list.add(bounds[1]);
                }
            }
        }
    }

    /** The lowest and highest height a height range can give: the anchors of its provider, or a sample when it has another shape. */
    private static int[] bounds(HeightRangePlacement range, WorldGenerationContext context) throws ReflectiveOperationException {
        HeightProvider provider = null;
        for (Field field : HeightRangePlacement.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == HeightProvider.class) {
                field.setAccessible(true);
                provider = (HeightProvider) field.get(range);
                break;
            }
        }
        if (provider == null) {
            return null;
        }
        List<Integer> anchors = new ArrayList<>();
        for (Field field : provider.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == VerticalAnchor.class) {
                field.setAccessible(true);
                anchors.add(((VerticalAnchor) field.get(provider)).resolveY(context));
            }
        }
        if (anchors.size() == 2) {
            return new int[] {Math.min(anchors.get(0), anchors.get(1)), Math.max(anchors.get(0), anchors.get(1))};
        }
        if (anchors.size() == 1) {
            return new int[] {anchors.get(0), anchors.get(0)};
        }
        RandomSource random = RandomSource.create(42L);
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int i = 0; i < 600; i++) {
            int y = provider.sample(random, context);
            low = Math.min(low, y);
            high = Math.max(high, y);
        }
        return new int[] {low, high};
    }
}
