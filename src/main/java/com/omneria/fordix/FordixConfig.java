package com.omneria.fordix;

import com.omneria.fordix.client.gui.Theme;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-side options, saved per player in config/fordix-client.toml. Nothing here
 * touches Minecraft client classes ({@link Theme} is plain Java), so it's safe to
 * register from the common mod constructor.
 */
public final class FordixConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue VOIDFRAME_VANILLA_SCREENS;
    public static final ForgeConfigSpec.ConfigValue<String> THEME;
    public static final ForgeConfigSpec.ConfigValue<String> ACCENT_COLOR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        VOIDFRAME_VANILLA_SCREENS = builder
                .comment("Draw vanilla container screens (inventory, chests, furnaces...) in the Fordix Voidframe style.")
                .define("voidframeVanillaScreens", true);
        THEME = builder
                .comment("Color theme for Fordix screens: " + String.join(", ", Theme.NAMES) + ".")
                .defineInList("theme", Theme.DEFAULT, Theme.NAMES);
        ACCENT_COLOR = builder
                .comment("Optional accent color override as #RRGGBB, e.g. \"#FF5577\". Leave empty to keep the theme's accent.")
                .define("accentColor", "", value -> value instanceof String text
                        && (text.isEmpty() || Theme.parseHex(text) != null));
        CLIENT_SPEC = builder.build();
    }

    private FordixConfig() {
    }
}
