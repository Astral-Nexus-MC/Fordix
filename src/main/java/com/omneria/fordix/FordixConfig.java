package com.omneria.fordix;

import com.omneria.fordix.client.gui.Theme;
import com.omneria.fordix.client.guide.GuideEntry;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Client-side options, saved per player in config/fordix-client.toml. Nothing here
 * touches Minecraft client classes ({@link Theme} and {@link GuideEntry} are plain
 * Java), so it's safe to
 * register from the common mod constructor.
 */
public final class FordixConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue VOIDFRAME_VANILLA_SCREENS;
    public static final ForgeConfigSpec.ConfigValue<String> THEME;
    public static final ForgeConfigSpec.ConfigValue<String> ACCENT_COLOR;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUIDES;

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
        GUIDES = builder
                .comment("Guide tabs along the right edge of the survival inventory, one per line: \"modid | Label | icon item | action\".",
                        "A tab only shows when its mod is installed (\"minecraft\" always is). Actions: ponder (Create's",
                        "Ponder index), patchouli:<book id>, url:<https link> (asks before opening), command:<command>.")
                .defineList("guides", List.of(
                        "create | Create : Ponder | create:wrench | ponder",
                        "mowziesmobs | Mowzie's Mobs : Wiki | mowziesmobs:foliaath_seed | url:https://mowziesmobs.fandom.com/",
                        "twilightforest | Twilight Forest : Wiki | twilightforest:naga_scale | url:https://twilightforestwiki.com/"
                ), value -> value instanceof String text && GuideEntry.parse(text) != null);
        CLIENT_SPEC = builder.build();
    }

    private FordixConfig() {
    }
}
