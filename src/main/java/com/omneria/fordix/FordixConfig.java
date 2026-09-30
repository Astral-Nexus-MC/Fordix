package com.omneria.fordix;

import com.omneria.fordix.client.gui.Theme;
import com.omneria.fordix.client.tooltip.TooltipStyles;
import com.omneria.fordix.client.notification.NotificationCorner;
import com.omneria.fordix.client.guide.GuideEntry;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side options, saved per player in config/fordix-client.toml. Nothing here
 * touches Minecraft client classes ({@link Theme}, {@link GuideEntry} and
 * {@link NotificationCorner} are plain Java), so it's safe to
 * register from the common mod constructor.
 */
public final class FordixConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue VOIDFRAME_VANILLA_SCREENS;
    public static final ForgeConfigSpec.BooleanValue VOIDFRAME_HUD;
    public static final ForgeConfigSpec.BooleanValue VOIDFRAME_TOOLTIPS;
    public static final ForgeConfigSpec.ConfigValue<String> TOOLTIP_STYLE;
    /** Elements of the Voidframe HUD whose position can be moved, in the order the layout screen lists them. */
    public static final List<String> HUD_ELEMENTS = List.of("health", "hunger", "armor", "air", "experience", "boss");
    private static final Map<String, ForgeConfigSpec.IntValue[]> HUD_OFFSETS = new LinkedHashMap<>();
    public static final ForgeConfigSpec.ConfigValue<String> THEME;
    public static final ForgeConfigSpec.ConfigValue<String> ACCENT_COLOR;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUIDES;
    public static final ForgeConfigSpec.ConfigValue<String> NOTIFICATION_POSITION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        VOIDFRAME_VANILLA_SCREENS = builder
                .comment("Draw vanilla container screens (inventory, chests, furnaces...) in the Fordix Voidframe style.")
                .define("voidframeVanillaScreens", true);
        VOIDFRAME_HUD = builder
                .comment("Replace the health hearts with a Voidframe bar showing the exact value.")
                .define("voidframeHud", true);
        VOIDFRAME_TOOLTIPS = builder
                .comment("Draw item tooltips with a header, attribute bars and rarity colors.")
                .define("voidframeTooltips", true);
        TOOLTIP_STYLE = builder
                .comment("Item tooltip layout: " + String.join(", ", TooltipStyles.NAMES) + ".",
                        "full: header, attribute bars and enchantment chips; compact: bars only, no header line or chips;",
                        "classic: vanilla layout in Fordix colors. Ignored (classic) when voidframeTooltips is false.")
                .defineInList("tooltipStyle", TooltipStyles.FULL, TooltipStyles.NAMES);
        builder.comment("Offsets in pixels (scaled GUI) of the Voidframe HUD elements from their default spot; edited from the in-game layout screen.")
                .push("hud");
        for (String element : HUD_ELEMENTS) {
            builder.push(element);
            ForgeConfigSpec.IntValue x = builder.defineInRange("offsetX", 0, -1000, 1000);
            ForgeConfigSpec.IntValue y = builder.defineInRange("offsetY", 0, -1000, 1000);
            builder.pop();
            HUD_OFFSETS.put(element, new ForgeConfigSpec.IntValue[]{x, y});
        }
        builder.pop();
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
        NOTIFICATION_POSITION = builder
                .comment("Screen corner for server notifications: " + String.join(", ", NotificationCorner.OPTIONS) + ".",
                        "\"auto\" is top_right. Notifications are drawn over every HUD overlay, JourneyMap's minimap included.")
                .defineInList("notificationPosition", NotificationCorner.AUTO, NotificationCorner.OPTIONS);
        CLIENT_SPEC = builder.build();
    }

    private FordixConfig() {
    }

    public static ForgeConfigSpec.IntValue hudOffsetX(String element) {
        return HUD_OFFSETS.get(element)[0];
    }

    public static ForgeConfigSpec.IntValue hudOffsetY(String element) {
        return HUD_OFFSETS.get(element)[1];
    }

    /** {x, y} offset of a HUD element; zero before the config has loaded. */
    public static int[] hudOffset(String element) {
        try {
            return new int[]{hudOffsetX(element).get(), hudOffsetY(element).get()};
        } catch (IllegalStateException notLoadedYet) {
            return new int[]{0, 0};
        }
    }

    /** The tooltip layout in force: "classic" when the tooltip option is off. */
    public static String tooltipStyle() {
        return VOIDFRAME_TOOLTIPS.get() ? TOOLTIP_STYLE.get() : TooltipStyles.CLASSIC;
    }
}
