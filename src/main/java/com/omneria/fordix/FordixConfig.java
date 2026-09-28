package com.omneria.fordix;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-side options, saved per player in config/fordix-client.toml. No client
 * class is referenced here, so it's safe to register from the common mod constructor.
 */
public final class FordixConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue VOIDFRAME_VANILLA_SCREENS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        VOIDFRAME_VANILLA_SCREENS = builder
                .comment("Draw vanilla container screens (inventory, chests, furnaces...) in the Fordix Voidframe style.")
                .define("voidframeVanillaScreens", true);
        CLIENT_SPEC = builder.build();
    }

    private FordixConfig() {
    }
}
