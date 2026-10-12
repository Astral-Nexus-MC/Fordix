package com.omneria.fordix.debug;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * What the F3 screen shows that only the server knows: the ores of the biome with their height ranges, the creatures that
 * can spawn there and the slime chunks around the player (a 7 x 7 square of chunks, one bit each, row by row from the north-west).
 * When {@code enabled} is false the server has switched these sections off ({@code [hud] debugReveal}).
 */
public record ClientboundDebugInfoPayload(boolean enabled, String dimension, String biome, int minY, int maxY, boolean slimeKnown,
                                          long slimeBits, List<Ore> ores, List<Mob> friendly, List<Mob> hostile) {
    public static final int SLIME_SIZE = 7;
    private static final int MAX_ENTRIES = 96;

    /** An ore block with its height ranges as [low, high, low, high...]. */
    public record Ore(String block, int[] ranges) {
    }

    public record Mob(String entity, int weight) {
    }

    public static ClientboundDebugInfoPayload disabled() {
        return new ClientboundDebugInfoPayload(false, "", "", 0, 0, false, 0L, List.of(), List.of(), List.of());
    }

    public static void encode(ClientboundDebugInfoPayload payload, FriendlyByteBuf buf) {
        buf.writeBoolean(payload.enabled());
        buf.writeUtf(payload.dimension(), 128);
        buf.writeUtf(payload.biome(), 128);
        buf.writeVarInt(payload.minY());
        buf.writeVarInt(payload.maxY());
        buf.writeBoolean(payload.slimeKnown());
        buf.writeLong(payload.slimeBits());
        buf.writeVarInt(payload.ores().size());
        for (Ore ore : payload.ores()) {
            buf.writeUtf(ore.block(), 128);
            buf.writeVarInt(ore.ranges().length);
            for (int value : ore.ranges()) {
                buf.writeVarInt(value);
            }
        }
        writeMobs(buf, payload.friendly());
        writeMobs(buf, payload.hostile());
    }

    private static void writeMobs(FriendlyByteBuf buf, List<Mob> mobs) {
        buf.writeVarInt(mobs.size());
        for (Mob mob : mobs) {
            buf.writeUtf(mob.entity(), 128);
            buf.writeVarInt(mob.weight());
        }
    }

    public static ClientboundDebugInfoPayload decode(FriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean();
        String dimension = buf.readUtf(128);
        String biome = buf.readUtf(128);
        int minY = buf.readVarInt();
        int maxY = buf.readVarInt();
        boolean slimeKnown = buf.readBoolean();
        long slimeBits = buf.readLong();
        int oreCount = Math.min(buf.readVarInt(), MAX_ENTRIES);
        List<Ore> ores = new ArrayList<>();
        for (int i = 0; i < oreCount; i++) {
            String block = buf.readUtf(128);
            int length = Math.min(buf.readVarInt(), 32);
            int[] ranges = new int[length];
            for (int j = 0; j < length; j++) {
                ranges[j] = buf.readVarInt();
            }
            ores.add(new Ore(block, ranges));
        }
        return new ClientboundDebugInfoPayload(enabled, dimension, biome, minY, maxY, slimeKnown, slimeBits, ores, readMobs(buf), readMobs(buf));
    }

    private static List<Mob> readMobs(FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
        List<Mob> mobs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            mobs.add(new Mob(buf.readUtf(128), buf.readVarInt()));
        }
        return mobs;
    }

    public boolean slimeAt(int dx, int dz) {
        int half = SLIME_SIZE / 2;
        if (!slimeKnown || Math.abs(dx) > half || Math.abs(dz) > half) {
            return false;
        }
        return (slimeBits >> ((dz + half) * SLIME_SIZE + (dx + half)) & 1L) != 0L;
    }
}
