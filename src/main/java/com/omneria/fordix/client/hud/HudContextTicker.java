package com.omneria.fordix.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.vanilla.VoidframeGauges;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Feeds {@link HudContext} with what the player is doing, announces each change, loads the server's layout of the situation
 * and fades the information panels as the server's table says. Always on: the server decides, not the player.
 */
public final class HudContextTicker {
    private static final HudContext CONTEXT = new HudContext();
    private static final Map<String, Float> SHOWN = new HashMap<>();
    private static long lastFrame;

    private HudContextTicker() {
    }

    public static HudContext.Kind kind() {
        return CONTEXT.current();
    }

    /** Every client tick. */
    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        boolean fighting = fightSignal(minecraft, player);
        boolean mining = minecraft.options.keyAttack.isDown() && minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
        HudContext.Kind kind = CONTEXT.update(System.currentTimeMillis(),
                new HudContext.Signals(VoidframeGauges.bossActive(minecraft), player.getVehicle() != null, fighting, mining));
        announce(kind);
        followWithProfile(minecraft, kind);
    }

    private static int hostileCheck;
    private static boolean hostileNear;

    /**
     * What a fight looks like from the client: the player was just hurt (the hurt animation is synced), or is attacking a
     * creature, or a hostile creature is close. (The game's own "last hurt" times are only kept on the server.)
     */
    private static boolean fightSignal(Minecraft minecraft, LocalPlayer player) {
        if (player.hurtTime > 0) {
            return true;
        }
        if (minecraft.options.keyAttack.isDown() && minecraft.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                && hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity target && target.isAlive()
                && !(target instanceof net.minecraft.world.entity.OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID()))) {
            return true;
        }
        if (++hostileCheck % 5 == 0) {
            hostileNear = !minecraft.level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                    player.getBoundingBox().inflate(HOSTILE_RANGE), monster -> monster.isAlive()).isEmpty();
        }
        return hostileNear;
    }

    private static final double HOSTILE_RANGE = 10.0D;

    private static HudContext.Kind announced = HudContext.Kind.EXPLORE;

    /** Tells the player when the situation changes: fighting, mining, a boss, a mount, and back to normal. */
    private static void announce(HudContext.Kind kind) {
        if (kind == announced) {
            return;
        }
        announced = kind;
        int accent = switch (kind) {
            case COMBAT -> 0xE5534B;
            case MINING -> 0xE8A64A;
            case BOSS -> 0xB45FE0;
            case MOUNT -> 0x5ED68A;
            case EXPLORE -> 0x3FD1C7;
        };
        String name = Tr.s("hud.context." + kind.name().toLowerCase(java.util.Locale.ROOT));
        HudToast.show(Tr.s("hud.toast.situation"), kind == HudContext.Kind.EXPLORE ? Tr.s("hud.toast.normal") : name, accent);
    }

    private static final ContextProfileSwitch PROFILES = new ContextProfileSwitch();
    private static java.util.Map<String, com.omneria.fordix.client.gui.layout.HudLayout.Placement> beforeProfile;

    /** Loads the layout the server set for the situation, and gives the previous layout back when it ends. */
    private static void followWithProfile(Minecraft minecraft, HudContext.Kind kind) {
        // The "slot" of a situation is its own number; the layout is the server's share code for it.
        var action = PROFILES.update(kind, ordinal -> ordinal, ordinal -> serverLayout(ordinal) != null);
        switch (action.type()) {
            case LOAD -> {
                if (beforeProfile == null) {
                    beforeProfile = com.omneria.fordix.client.gui.vanilla.HudProfileActions.capture();
                }
                com.omneria.fordix.client.gui.vanilla.HudProfileActions.applyLayout(serverLayout(action.slot()));
                HudToast.show(Tr.s("hud.toast.profile"),
                        Tr.s("hud.context." + HudContext.Kind.values()[action.slot()].name().toLowerCase(java.util.Locale.ROOT)), 0x3FD1C7);
            }
            case RESTORE -> {
                if (beforeProfile != null) {
                    com.omneria.fordix.client.gui.vanilla.HudProfileActions.applyLayout(beforeProfile);
                    beforeProfile = null;
                    HudToast.show(Tr.s("hud.toast.profile"), Tr.s("hud.toast.back"), 0x3FD1C7);
                }
            }
            default -> {
            }
        }
    }

    private static java.util.Map<String, com.omneria.fordix.client.gui.layout.HudLayout.Placement> serverLayout(int situation) {
        String code = HudRules.situationProfile(situation);
        return code.isEmpty() ? null : com.omneria.fordix.client.gui.layout.HudProfileCodec.decode(code);
    }

    /** Logging out: forget the situation and the layout to give back. */
    public static void reset() {
        PROFILES.reset();
        announced = HudContext.Kind.EXPLORE;
        hostileNear = false;
        beforeProfile = null;
    }

    /** Opacity multiplier for a panel, eased over 200 ms so a change of situation is a fade and never a jump. */
    public static float factor(String element) {
        float target = HudContext.factor(kind(), element);
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
