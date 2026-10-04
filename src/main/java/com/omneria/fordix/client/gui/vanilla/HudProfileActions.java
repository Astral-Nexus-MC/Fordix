package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.layout.HudProfiles;
import com.omneria.fordix.client.hud.CombatSwitch;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Saved HUD layouts: {@link #save} files the current placement of every element (Voidframe's own and the overlays) in a
 * slot, {@link #load} puts one back, {@link #loadNext} is what the profile key does. JourneyMap's minimap keeps its own settings.
 */
public final class HudProfileActions {
    private static HudProfiles profiles;

    private HudProfileActions() {
    }

    public static HudProfiles profiles() {
        if (profiles == null) {
            profiles = new HudProfiles(FMLPaths.CONFIGDIR.get().resolve("fordix-hud-profiles.json"));
        }
        return profiles;
    }

    public static String name(int slot) {
        return Tr.s("hud.profile.name." + slot);
    }

    /** The placement of every element as it is now. */
    private static Map<String, HudProfiles.Placement> snapshotAll() {
        Map<String, HudProfiles.Placement> layout = new LinkedHashMap<>();
        for (String id : FordixConfig.HUD_ELEMENTS) {
            layout.put(id, HudSlots.snapshot(id));
        }
        for (String id : ExtraHud.knownIds()) {
            layout.put(id, HudSlots.snapshot(id));
        }
        return layout;
    }

    /** Puts a layout back; an element it doesn't know returns to its default spot. */
    private static void restoreAll(Map<String, HudProfiles.Placement> layout) {
        for (String id : FordixConfig.HUD_ELEMENTS) {
            apply(id, layout);
        }
        for (String id : ExtraHud.knownIds()) {
            apply(id, layout);
        }
        FordixConfig.hudOffsetX(FordixConfig.HUD_ELEMENTS.get(0)).save();
        ExtraHud.save();
    }

    public static void save(int slot) {
        profiles().put(slot, snapshotAll());
    }

    /** Puts a saved layout back. */
    public static boolean load(int slot) {
        Map<String, HudProfiles.Placement> layout = profiles().get(slot);
        if (layout == null) {
            return false;
        }
        restoreAll(layout);
        profiles().setActive(slot);
        return true;
    }

    // ---- the combat layout, entered while a hostile creature is near

    private static final int COMBAT_SLOT = 1;
    private static final int CHECK_EVERY_TICKS = 10;
    private static final double HOSTILE_RANGE = 12.0D;
    private static final CombatSwitch COMBAT = new CombatSwitch(6);
    private static Map<String, HudProfiles.Placement> beforeCombat;
    private static int ticks;

    public static void onLoggingOut() {
        beforeCombat = null;
    }

    /** Every client tick: with the option on, the second profile (Combat) is loaded while a hostile creature is near. */
    public static void autoTick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || ++ticks % CHECK_EVERY_TICKS != 0) {
            return;
        }
        if (!FordixConfig.AUTO_COMBAT_PROFILE.get()) {
            if (beforeCombat != null) {
                restoreAll(beforeCombat);
                beforeCombat = null;
            }
            return;
        }
        boolean hostile = profiles().has(COMBAT_SLOT) && !minecraft.level.getEntitiesOfClass(
                net.minecraft.world.entity.monster.Monster.class,
                minecraft.player.getBoundingBox().inflate(HOSTILE_RANGE), monster -> monster.isAlive()).isEmpty();
        switch (COMBAT.update(hostile)) {
            case ENTER -> {
                beforeCombat = snapshotAll();
                restoreAll(profiles().get(COMBAT_SLOT));
                minecraft.gui.setOverlayMessage(Tr.c("hud.profile.loaded", name(COMBAT_SLOT)), false);
            }
            case LEAVE -> {
                if (beforeCombat != null) {
                    restoreAll(beforeCombat);
                    beforeCombat = null;
                    minecraft.gui.setOverlayMessage(Tr.c("hud.profile.back"), false);
                }
            }
            default -> {
            }
        }
    }

    private static void apply(String id, Map<String, HudProfiles.Placement> layout) {
        HudProfiles.Placement placement = layout.get(id);
        if (placement != null) {
            HudSlots.restore(id, placement);
        } else {
            HudSlots.reset(id);
        }
    }

    /** The profile key: the next filled slot, announced above the hotbar. */
    public static void loadNext(Minecraft minecraft) {
        int slot = profiles().next();
        if (slot < 0) {
            minecraft.gui.setOverlayMessage(Tr.c("hud.profile.none"), false);
        } else if (load(slot)) {
            minecraft.gui.setOverlayMessage(Tr.c("hud.profile.loaded", name(slot)), false);
        }
    }
}
