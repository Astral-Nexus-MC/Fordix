package com.omneria.fordix;

import com.omneria.fordix.client.gui.Theme;
import com.omneria.fordix.client.gui.layout.HudAnchors;
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
    public static final ForgeConfigSpec.BooleanValue VOIDFRAME_CHAT;
    public static final ForgeConfigSpec.BooleanValue VOIDFRAME_TOOLTIPS;
    public static final ForgeConfigSpec.BooleanValue GUIDE_HINTS;
    public static final ForgeConfigSpec.ConfigValue<String> TOOLTIP_STYLE;
    /** Elements of the Voidframe HUD whose position can be moved, in the order the layout screen lists them. */
    public static final List<String> HUD_ELEMENTS = List.of("health", "hunger", "armor", "air", "experience", "boss");
    private static final Map<String, ForgeConfigSpec.IntValue[]> HUD_OFFSETS = new LinkedHashMap<>();
    private static final Map<String, ForgeConfigSpec.ConfigValue<String>> HUD_ANCHORS = new LinkedHashMap<>();
    private static final Map<String, ForgeConfigSpec.BooleanValue> HUD_HIDDEN = new LinkedHashMap<>();
    private static final Map<String, ForgeConfigSpec.DoubleValue> HUD_SCALES = new LinkedHashMap<>();
    /** Lines of the information panels the player can switch off, in the order the lines screen lists them. */
    public static final List<String> HUD_LINES = List.of(
            "coordinates_xyz", "coordinates_direction", "coordinates_biome",
            "performance_fps", "performance_tps", "performance_ping",
            "target_tool", "target_entity",
            "gear_durability", "gear_ammo",
            "clock_real", "clock_game", "clock_weather",
            "compass_spawn", "compass_marker");
    private static final Map<String, ForgeConfigSpec.BooleanValue> HUD_LINE_VALUES = new LinkedHashMap<>();
    public static final ForgeConfigSpec.BooleanValue CHAT_MENTION_SOUND;
    public static final ForgeConfigSpec.BooleanValue HUD_REQUIRE_ITEMS;
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
        VOIDFRAME_CHAT = builder
                .comment("Replace the chat with the Voidframe one: tabs, player heads, times and a styled text field.")
                .define("voidframeChat", true);
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
            ForgeConfigSpec.ConfigValue<String> anchor = builder
                    .comment("Screen point the element is pinned to: default (its usual spot), top_left, top_center, top_right,",
                            "middle_left, center, middle_right, bottom_left, bottom_center or bottom_right.")
                    .define("anchor", "default", value -> value instanceof String text && HudAnchors.Anchor.byId(text) != null);
            ForgeConfigSpec.BooleanValue hidden = builder.define("hidden", false);
            ForgeConfigSpec.DoubleValue scale = builder
                    .comment("Size of the element, 1.0 being its usual size.")
                    .defineInRange("scale", 1.0D, HudAnchors.MIN_SCALE, HudAnchors.MAX_SCALE);
            ForgeConfigSpec.IntValue x = builder.defineInRange("offsetX", 0, -1000, 1000);
            ForgeConfigSpec.IntValue y = builder.defineInRange("offsetY", 0, -1000, 1000);
            builder.pop();
            HUD_OFFSETS.put(element, new ForgeConfigSpec.IntValue[]{x, y});
            HUD_ANCHORS.put(element, anchor);
            HUD_HIDDEN.put(element, hidden);
            HUD_SCALES.put(element, scale);
        }
        builder.pop();
        builder.comment("Which lines the information panels of the HUD show (coordinates, FPS/TPS/ping, target, gear, clock, compass).")
                .push("hudLines");
        for (String line : HUD_LINES) {
            HUD_LINE_VALUES.put(line, builder.define(line, true));
        }
        builder.pop();
        HUD_REQUIRE_ITEMS = builder
                .comment("The compass panel needs a compass (or recovery compass) in your inventory and the clock panel needs a clock:",
                        "craft them to unlock these two panels.")
                .define("hudRequireItems", true);
        CHAT_MENTION_SOUND = builder
                .comment("Play a short sound when a chat message mentions your name or whispers to you.")
                .define("chatMentionSound", true);
        THEME = builder
                .comment("Color theme for Fordix screens: " + String.join(", ", Theme.NAMES) + ".")
                .defineInList("theme", Theme.DEFAULT, Theme.NAMES);
        ACCENT_COLOR = builder
                .comment("Optional accent color override as #RRGGBB, e.g. \"#FF5577\". Leave empty to keep the theme's accent.")
                .define("accentColor", "", value -> value instanceof String text
                        && (text.isEmpty() || Theme.parseHex(text) != null));
        GUIDE_HINTS = builder
                .comment("Add the \"[Shift + G] open the guide\" line to the tooltip of items that have a guide (the key keeps working when off).")
                .define("guideHints", true);
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

    public static ForgeConfigSpec.ConfigValue<String> hudAnchorValue(String element) {
        return HUD_ANCHORS.get(element);
    }

    public static ForgeConfigSpec.DoubleValue hudScaleValue(String element) {
        return HUD_SCALES.get(element);
    }

    /** Size factor of a HUD element (1 before the config has loaded). */
    public static float hudScale(String element) {
        try {
            return HudAnchors.clampScale(HUD_SCALES.get(element).get().floatValue());
        } catch (IllegalStateException notLoadedYet) {
            return 1.0F;
        }
    }

    public static ForgeConfigSpec.BooleanValue hudHiddenValue(String element) {
        return HUD_HIDDEN.get(element);
    }

    public static HudAnchors.Anchor hudAnchor(String element) {
        try {
            HudAnchors.Anchor anchor = HudAnchors.Anchor.byId(HUD_ANCHORS.get(element).get());
            return anchor != null ? anchor : HudAnchors.Anchor.DEFAULT;
        } catch (IllegalStateException notLoadedYet) {
            return HudAnchors.Anchor.DEFAULT;
        }
    }

    public static boolean hudHidden(String element) {
        try {
            return HUD_HIDDEN.get(element).get();
        } catch (IllegalStateException notLoadedYet) {
            return false;
        }
    }

    /** Raw {x, y} offset of a HUD element (from its anchor); zero before the config has loaded. */
    public static int[] hudOffset(String element) {
        try {
            return new int[]{hudOffsetX(element).get(), hudOffsetY(element).get()};
        } catch (IllegalStateException notLoadedYet) {
            return new int[]{0, 0};
        }
    }

    public static ForgeConfigSpec.BooleanValue hudLineValue(String line) {
        return HUD_LINE_VALUES.get(line);
    }

    /** Whether a line of an information panel is shown (yes before the config has loaded). */
    public static boolean hudLine(String line) {
        try {
            return HUD_LINE_VALUES.get(line).get();
        } catch (IllegalStateException notLoadedYet) {
            return true;
        }
    }

    /** The tooltip layout in force: "classic" when the tooltip option is off. */
    public static String tooltipStyle() {
        return VOIDFRAME_TOOLTIPS.get() ? TOOLTIP_STYLE.get() : TooltipStyles.CLASSIC;
    }
}
