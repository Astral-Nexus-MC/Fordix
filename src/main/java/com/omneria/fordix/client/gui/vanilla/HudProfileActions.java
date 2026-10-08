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

    /** Ready-made layouts for the slots nobody has filled yet: the panels each one hides, the rest at their default spot. */
    private static final Map<Integer, java.util.List<String>> PRESET_HIDDEN = Map.of(
            3, java.util.List.of("fordix:clock", "fordix:compass", "fordix:target_mob", "fordix:gear"),
            4, java.util.List.of("fordix:coordinates", "fordix:performance", "fordix:clock", "fordix:compass"),
            5, java.util.List.of("fordix:coordinates", "fordix:performance", "fordix:clock", "fordix:compass", "fordix:target_block",
                    "fordix:target_mob", "fordix:gear", "fordix:effects"));

    /** What a slot holds: the player's saved layout, else the ready-made one of that slot, else nothing. */
    public static Map<String, HudProfiles.Placement> layoutOf(int slot) {
        Map<String, HudProfiles.Placement> saved = profiles().get(slot);
        if (saved != null) {
            return saved;
        }
        // The server's suggestion takes the last slot while the player has not filled it.
        if (slot == HudProfiles.SLOTS - 1) {
            Map<String, HudProfiles.Placement> proposed = com.omneria.fordix.client.gui.layout.HudProfileCodec.decode(
                    com.omneria.fordix.client.hud.HudRules.proposed());
            if (proposed != null) {
                return proposed;
            }
        }
        java.util.List<String> hidden = PRESET_HIDDEN.get(slot);
        if (hidden == null) {
            return null;
        }
        Map<String, HudProfiles.Placement> layout = new LinkedHashMap<>();
        for (String id : hidden) {
            HudProfiles.Placement placement = new HudProfiles.Placement();
            placement.hidden = true;
            layout.put(id, placement);
        }
        return layout;
    }

    public static boolean usable(int slot) {
        return layoutOf(slot) != null;
    }

    /** The share code of what is on screen now. */
    public static String exportCode() {
        return com.omneria.fordix.client.gui.layout.HudProfileCodec.encode(snapshotAll());
    }

    /** Puts a pasted code on screen and files it in the slot; false when the text is not a valid code. */
    public static boolean importCode(String text, int slot) {
        Map<String, HudProfiles.Placement> layout = com.omneria.fordix.client.gui.layout.HudProfileCodec.decode(text);
        if (layout == null) {
            return false;
        }
        restoreAll(layout);
        profiles().put(slot, layout);
        return true;
    }

    /** The placement of every element as it is now, to give back later with {@link #applyLayout}. */
    public static Map<String, HudProfiles.Placement> capture() {
        return snapshotAll();
    }

    public static void applyLayout(Map<String, HudProfiles.Placement> layout) {
        restoreAll(layout);
    }

    public static void save(int slot) {
        profiles().put(slot, snapshotAll());
    }

    /** Puts a saved layout back. */
    public static boolean load(int slot) {
        Map<String, HudProfiles.Placement> layout = layoutOf(slot);
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
        if (com.omneria.fordix.client.hud.HudContextTicker.enabled()) {
            return; // the situations have their own profiles (HudContextsScreen)
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
        int slot = -1;
        for (int step = 1; step <= HudProfiles.SLOTS; step++) {
            int candidate = Math.floorMod(profiles().active() + step, HudProfiles.SLOTS);
            if (usable(candidate)) {
                slot = candidate;
                break;
            }
        }
        if (slot < 0) {
            minecraft.gui.setOverlayMessage(Tr.c("hud.profile.none"), false);
        } else if (load(slot)) {
            minecraft.gui.setOverlayMessage(Tr.c("hud.profile.loaded", name(slot)), false);
        }
    }
}
