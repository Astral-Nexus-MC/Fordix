package com.omneria.fordix.welcome;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * Server to client: the welcome book's checklist for this player. {@code open} asks the client to
 * show the book's screen (the player right-clicked the item); otherwise it only refreshes one already open.
 */
public record ClientboundWelcomePayload(boolean open, boolean rewardPending, List<Step> steps) {
    /** One line of the checklist; {@code action} is a guide action for a button, or empty. */
    public record Step(boolean done, boolean manual, String label, String action) {
    }

    public static void encode(ClientboundWelcomePayload payload, FriendlyByteBuf buf) {
        buf.writeBoolean(payload.open());
        buf.writeBoolean(payload.rewardPending());
        buf.writeCollection(payload.steps(), (b, step) -> {
            b.writeBoolean(step.done());
            b.writeBoolean(step.manual());
            b.writeUtf(step.label(), 128);
            b.writeUtf(step.action(), 512);
        });
    }

    public static ClientboundWelcomePayload decode(FriendlyByteBuf buf) {
        boolean open = buf.readBoolean();
        boolean reward = buf.readBoolean();
        List<Step> steps = buf.readList(b -> new Step(b.readBoolean(), b.readBoolean(), b.readUtf(128), b.readUtf(512)));
        return new ClientboundWelcomePayload(open, reward, steps.size() > 32 ? steps.subList(0, 32) : steps);
    }
}
