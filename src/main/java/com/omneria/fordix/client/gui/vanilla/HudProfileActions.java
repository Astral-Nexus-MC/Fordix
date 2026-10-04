package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.layout.HudProfiles;
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

    public static void save(int slot) {
        Map<String, HudProfiles.Placement> layout = new LinkedHashMap<>();
        for (String id : FordixConfig.HUD_ELEMENTS) {
            layout.put(id, HudSlots.snapshot(id));
        }
        for (String id : ExtraHud.knownIds()) {
            layout.put(id, HudSlots.snapshot(id));
        }
        profiles().put(slot, layout);
    }

    /** Puts a saved layout back; an element the profile doesn't know returns to its default spot. */
    public static boolean load(int slot) {
        Map<String, HudProfiles.Placement> layout = profiles().get(slot);
        if (layout == null) {
            return false;
        }
        for (String id : FordixConfig.HUD_ELEMENTS) {
            apply(id, layout);
        }
        for (String id : ExtraHud.knownIds()) {
            apply(id, layout);
        }
        FordixConfig.hudOffsetX(FordixConfig.HUD_ELEMENTS.get(0)).save();
        ExtraHud.save();
        profiles().setActive(slot);
        return true;
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
