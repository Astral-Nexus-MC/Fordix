package com.omneria.fordix.client.hud;

import com.omneria.fordix.debug.ClientboundDebugInfoPayload;
import com.omneria.fordix.debug.ServerboundDebugInfoPayload;
import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * What the server says about where the player stands (ores, creatures, slime chunks), asked for when the F3 screen is open: at
 * first, when the biome or the dimension changes and when the player enters another chunk, at most once a second.
 */
public final class DebugInfoClient {
    private static final long MIN_INTERVAL_MS = 1000L;
    private static final long DISABLED_INTERVAL_MS = 30000L;

    private static ClientboundDebugInfoPayload data;
    private static long lastRequest;
    private static long sentChunk = Long.MIN_VALUE;
    private static long dataChunk = Long.MIN_VALUE;

    private DebugInfoClient() {
    }

    public static void receive(ClientboundDebugInfoPayload payload) {
        data = payload;
        dataChunk = sentChunk;
    }

    /** The last answer, or null before the first one. */
    public static ClientboundDebugInfoPayload data() {
        return data;
    }

    public static void forget() {
        data = null;
        sentChunk = Long.MIN_VALUE;
        dataChunk = Long.MIN_VALUE;
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long wait = data != null && !data.enabled() ? DISABLED_INTERVAL_MS : MIN_INTERVAL_MS;
        if (now - lastRequest < wait) {
            return;
        }
        BlockPos pos = minecraft.player.blockPosition();
        long chunk = (((long) (pos.getX() >> 4)) << 32) ^ (pos.getZ() >> 4 & 0xFFFFFFFFL);
        String biome = minecraft.level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("");
        String dimension = minecraft.level.dimension().location().toString();
        boolean stale = data == null || (data.enabled() && (!data.biome().equals(biome) || !data.dimension().equals(dimension) || dataChunk != chunk))
                || (!data.enabled() && now - lastRequest >= DISABLED_INTERVAL_MS);
        if (stale) {
            lastRequest = now;
            sentChunk = chunk;
            PanelNetwork.CHANNEL.sendToServer(new ServerboundDebugInfoPayload(true));
        }
    }
}
