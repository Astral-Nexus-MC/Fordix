package com.omneria.fordix.welcome;

import com.omneria.fordix.Tr;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.FordixServerConfig;
import com.omneria.fordix.notification.server.NotificationScheduler;
import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The welcome book's checklist on the server: which steps a player has ticked (kept in his persisted
 * data), the conditions that tick them, the optional reward, and the book itself given on a first join.
 * The client only shows what it is sent and asks to tick manual steps.
 */
public final class WelcomeService {
    private static final String ROOT = "fordix_welcome";
    private static final String DONE = "done";
    private static final String REWARDED = "rewarded";
    private static final String BOOK_GIVEN = "book_given";

    private WelcomeService() {
    }

    /** The steps as config lines: the ones saved from the admin screen when there are some, else the server config's. */
    public static List<String> stepLines() {
        List<String> override = NotificationScheduler.store().welcomeSteps;
        return override != null ? override : new ArrayList<>(FordixServerConfig.WELCOME_BOOK_STEPS.get());
    }

    public static String rewardCommand() {
        String override = NotificationScheduler.store().welcomeReward;
        return (override != null ? override : FordixServerConfig.WELCOME_BOOK_REWARD.get()).strip();
    }

    public static List<WelcomeStep> steps() {
        List<WelcomeStep> steps = new ArrayList<>();
        for (String line : stepLines()) {
            WelcomeStep step = WelcomeStep.parse(line);
            if (step != null) {
                steps.add(step);
            }
        }
        return steps;
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!player.getPersistentData().contains(Player.PERSISTED_NBT_TAG)) {
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, new CompoundTag());
        }
        return persisted.getCompound(ROOT);
    }

    private static Set<String> doneKeys(ServerPlayer player) {
        Set<String> keys = new HashSet<>();
        for (Tag tag : data(player).getList(DONE, Tag.TAG_STRING)) {
            keys.add(tag.getAsString());
        }
        return keys;
    }

    private static void saveKeys(ServerPlayer player, Set<String> keys) {
        ListTag list = new ListTag();
        keys.forEach(key -> list.add(StringTag.valueOf(key)));
        data(player).put(DONE, list);
    }

    /** Ticks a step; true when it wasn't ticked before. */
    private static boolean mark(ServerPlayer player, WelcomeStep step) {
        Set<String> keys = doneKeys(player);
        if (!keys.add(step.key())) {
            return false;
        }
        saveKeys(player, keys);
        return true;
    }

    /** Ticks the steps whose condition now holds (advancements), then gives the reward once everything is done. */
    public static void refresh(ServerPlayer player) {
        for (WelcomeStep step : steps()) {
            if (step.kind() == WelcomeStep.Kind.ADVANCEMENT && hasAdvancement(player, step.argument())) {
                mark(player, step);
            }
        }
        maybeReward(player);
    }

    private static boolean hasAdvancement(ServerPlayer player, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        Advancement advancement = location == null ? null : player.server.getAdvancements().getAdvancement(location);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static boolean allDone(ServerPlayer player, List<WelcomeStep> steps) {
        Set<String> keys = doneKeys(player);
        return !steps.isEmpty() && steps.stream().allMatch(step -> keys.contains(step.key()));
    }

    private static void maybeReward(ServerPlayer player) {
        String reward = rewardCommand();
        if (reward.isEmpty() || data(player).getBoolean(REWARDED) || !allDone(player, steps())) {
            return;
        }
        data(player).putBoolean(REWARDED, true);
        String command = reward.replace("%player%", player.getGameProfile().getName());
        var server = player.server;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
        player.sendSystemMessage(Tr.c("welcome.reward_done"));
    }

    /** Sends the checklist; {@code open} makes the client show the book's screen. */
    public static void sendState(ServerPlayer player, boolean open) {
        refresh(player);
        Set<String> keys = doneKeys(player);
        List<ClientboundWelcomePayload.Step> steps = new ArrayList<>();
        for (WelcomeStep step : steps()) {
            steps.add(new ClientboundWelcomePayload.Step(keys.contains(step.key()), step.kind() == WelcomeStep.Kind.MANUAL,
                    step.label(), step.action()));
        }
        boolean rewardPending = !rewardCommand().isBlank() && !data(player).getBoolean(REWARDED);
        PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundWelcomePayload(open, rewardPending, steps));
    }

    public static void handle(ServerboundWelcomePayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            List<WelcomeStep> steps = steps();
            switch (payload.action()) {
                case "guides_opened" -> {
                    steps.stream().filter(step -> step.kind() == WelcomeStep.Kind.GUIDES).forEach(step -> mark(player, step));
                    maybeReward(player);
                }
                case "toggle" -> {
                    if (payload.index() >= 0 && payload.index() < steps.size() && steps.get(payload.index()).kind() == WelcomeStep.Kind.MANUAL) {
                        WelcomeStep step = steps.get(payload.index());
                        Set<String> keys = doneKeys(player);
                        if (!keys.remove(step.key())) {
                            keys.add(step.key());
                        }
                        saveKeys(player, keys);
                        sendState(player, false);
                    }
                }
                case "refresh" -> sendState(player, false);
                default -> {
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void onCommand(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String typed = event.getParseResults().getReader().getString();
        boolean changed = false;
        for (WelcomeStep step : steps()) {
            if (step.matchesCommand(typed)) {
                changed |= mark(player, step);
            }
        }
        if (changed) {
            maybeReward(player);
        }
    }

    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    /** The book itself, given once to a player joining for the first time. */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !FordixServerConfig.WELCOME_BOOK_GIVE.get()
                || data(player).getBoolean(BOOK_GIVEN)) {
            return;
        }
        data(player).putBoolean(BOOK_GIVEN, true);
        give(player);
    }

    public static void give(ServerPlayer player) {
        ItemStack book = new ItemStack(FordixItems.WELCOME_BOOK.get());
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        Fordix.LOGGER.debug("Gave the welcome book to {}", player.getGameProfile().getName());
    }
}
