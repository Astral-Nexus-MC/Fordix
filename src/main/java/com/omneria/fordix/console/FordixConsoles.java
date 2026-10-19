package com.omneria.fordix.console;

import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Levelled-settings consoles in Voidframe: a mod that has a block or an item with a row of settings (a level from -max to +max, an
 * item to raise it and an item to lower it) shows it as a screen instead of chat lines. The API uses only Java and Minecraft types
 * ({@link CompoundTag}, {@link BiFunction}), so a mod calls it by reflection and keeps Fordix optional:
 * <pre>
 *   FordixConsoles.register(String kind, BiFunction&lt;ServerPlayer, CompoundTag, CompoundTag&gt; onAction)
 *   FordixConsoles.open(ServerPlayer player, CompoundTag data)
 * </pre>
 * {@code data} is described in {@link ConsoleData}. When a button is clicked the handler registered for the console's kind gets the
 * player and the action ({@code pos}, {@code row}, {@code bonus}) and returns the refreshed data (or null to leave the screen as is).
 * The server checks the player is close to {@code pos} before calling the handler: the mod still checks what the action is allowed to do.
 */
public final class FordixConsoles {
    private static final double MAX_DISTANCE_SQR = 64.0D;
    private static final long MIN_INTERVAL_TICKS = 2L;
    private static final Map<String, BiFunction<ServerPlayer, CompoundTag, CompoundTag>> HANDLERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();

    private FordixConsoles() {
    }

    public static void register(String kind, BiFunction<ServerPlayer, CompoundTag, CompoundTag> onAction) {
        HANDLERS.put(kind, onAction);
    }

    /** Opens (or refreshes) the screen on the player's side; false when the data has no kind or position. */
    public static boolean open(ServerPlayer player, CompoundTag data) {
        if (ConsoleData.fromTag(data) == null) {
            return false;
        }
        PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundConsolePayload(data));
        return true;
    }

    public static void handle(ServerboundConsolePayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                act(player, payload);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void act(ServerPlayer player, ServerboundConsolePayload payload) {
        BiFunction<ServerPlayer, CompoundTag, CompoundTag> handler = HANDLERS.get(payload.kind());
        int[] pos = payload.action().getIntArray("pos");
        if (handler == null || pos.length != 3 || player.distanceToSqr(pos[0] + 0.5D, pos[1] + 0.5D, pos[2] + 0.5D) > MAX_DISTANCE_SQR) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST_ACTION.get(player.getUUID());
        if (last != null && now >= last && now - last < MIN_INTERVAL_TICKS) {
            return;
        }
        LAST_ACTION.put(player.getUUID(), now);
        CompoundTag refreshed = handler.apply(player, payload.action());
        if (refreshed != null) {
            refreshed.putString("kind", payload.kind());
            open(player, refreshed);
        }
    }
}
