package com.omneria.fordix;

import com.omneria.fordix.client.gui.Theme;
import com.omneria.fordix.client.gui.layout.HudAnchors;
import com.omneria.fordix.client.tooltip.TooltipStyles;
import com.omneria.fordix.client.guide.GuideEntry;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side options, saved per player in config/fordix-client.toml. Nothing here
 * touches Minecraft client classes ({@link Theme}, {@link GuideEntry} and
 * ), so it's safe to
 * register from the common mod constructor.
 */
public final class FordixConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue VOIDFRAME_VANILLA_SCREENS;
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
    private static final Map<String, ForgeConfigSpec.DoubleValue> HUD_SCALES_Y = new LinkedHashMap<>();
    /** Lines of the information panels the player can switch off, in the order the lines screen lists them. */
    public static final List<String> HUD_LINES = List.of(
            "coordinates_xyz", "coordinates_direction", "coordinates_biome",
            "performance_fps", "performance_tps", "performance_ping",
            "target_tool", "target_entity", "target_container",
            "gear_durability", "gear_ammo", "gear_armor",
            "clock_real", "clock_game", "clock_weather",
            "compass_spawn");
    private static final Map<String, ForgeConfigSpec.BooleanValue> HUD_LINE_VALUES = new LinkedHashMap<>();
    /** Alerts that can be switched off, in the order the options screen lists them. */
    public static final List<String> HUD_ALERTS = List.of("alert_durability", "alert_ammo", "alert_tps", "alert_critical", "alert_sound");
    private static final Map<String, ForgeConfigSpec.BooleanValue> HUD_ALERT_VALUES = new LinkedHashMap<>();
    public static final List<String> HUD_OPACITIES = List.of("100", "85", "70", "55", "40");
    public static final ForgeConfigSpec.ConfigValue<String> HUD_OPACITY;
    /** How far (blocks) the targeted creature panel and the creature sheet key reach; 4 or less keeps the game's own reach. */
    public static final ForgeConfigSpec.BooleanValue HUD_COMPACT;
    public static final ForgeConfigSpec.BooleanValue HUD_EFFECTS_LIST;
    public static final ForgeConfigSpec.BooleanValue BORDERLESS_FULLSCREEN;
    public static final ForgeConfigSpec.BooleanValue INVENTORY_TOOLS;
    public static final ForgeConfigSpec.IntValue ALERT_DURABILITY_PERCENT;
    public static final ForgeConfigSpec.BooleanValue CHAT_MENTION_SOUND;
    public static final ForgeConfigSpec.ConfigValue<String> THEME;
    public static final ForgeConfigSpec.ConfigValue<String> ACCENT_COLOR;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUIDES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        VOIDFRAME_VANILLA_SCREENS = builder
                .comment("Draw vanilla container screens (inventory, chests, furnaces...) in the Fordix Voidframe style.")
                .define("voidframeVanillaScreens", true);
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
            ForgeConfigSpec.DoubleValue scaleY = builder
                    .comment("Height factor when it differs from the width one; 0 keeps both the same.")
                    .defineInRange("scaleY", 0.0D, 0.0D, HudAnchors.MAX_SCALE);
            ForgeConfigSpec.IntValue x = builder.defineInRange("offsetX", 0, -1000, 1000);
            ForgeConfigSpec.IntValue y = builder.defineInRange("offsetY", 0, -1000, 1000);
            builder.pop();
            HUD_OFFSETS.put(element, new ForgeConfigSpec.IntValue[]{x, y});
            HUD_ANCHORS.put(element, anchor);
            HUD_HIDDEN.put(element, hidden);
            HUD_SCALES.put(element, scale);
            HUD_SCALES_Y.put(element, scaleY);
        }
        builder.pop();
        builder.comment("Which lines the information panels of the HUD show (coordinates, FPS/TPS/ping, target, gear, clock, compass).")
                .push("hudLines");
        for (String line : HUD_LINES) {
            HUD_LINE_VALUES.put(line, builder.define(line, true));
        }
        builder.pop();
        builder.comment("Warnings above the hotbar: an item about to break, a bow without arrows, a server that lags.").push("alerts");
        for (String alert : HUD_ALERTS) {
            HUD_ALERT_VALUES.put(alert, builder.define(alert, true));
        }
        ALERT_DURABILITY_PERCENT = builder.comment("Durability left (in percent) under which the break warning shows.")
                .defineInRange("durabilityPercent", 10, 1, 50);
        builder.pop();
        HUD_OPACITY = builder.comment("Opacity of the information panels in percent: " + String.join(", ", HUD_OPACITIES) + ".")
                .define("hudOpacity", "70", value -> value instanceof String text && HUD_OPACITIES.contains(text));
        HUD_COMPACT = builder.comment("Tighter information panels (less padding and line spacing), for small screens.")
                .define("hudCompact", false);
        HUD_EFFECTS_LIST = builder.comment("Replace the potion effect icons with a list showing name and time left.")
                .define("hudEffectsList", true);
        BORDERLESS_FULLSCREEN = builder.comment("F11 (the game's Toggle Fullscreen key) gives a borderless window over the whole monitor instead of a video mode change: Alt-Tab is instant.")
                .define("borderlessFullscreen", false);
        INVENTORY_TOOLS = builder.comment("Add a search field and a sort button to the inventory and to chests, barrels and shulker boxes.")
                .define("inventoryTools", true);
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
                        "create | Create : Wiki | create:andesite_alloy | url:https://create.fandom.com/wiki/Create_Mod_Wiki",
                        "minecolonies | MineColonies : Wiki | minecolonies:blockhuttownhall | url:https://minecolonies.com/wiki/",
                        "mowziesmobs | Mowzie's Mobs : Wiki | mowziesmobs:foliaath_seed | url:https://mowziesmobs.fandom.com/",
                        "twilightforest | Twilight Forest : Wiki | twilightforest:naga_scale | url:https://twilightforestwiki.com/"
                ), value -> value instanceof String text && GuideEntry.parse(text) != null);
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

    public static ForgeConfigSpec.DoubleValue hudScaleYValue(String element) {
        return HUD_SCALES_Y.get(element);
    }

    /** Height factor of a HUD element: its own when set, the width factor otherwise. */
    public static float hudScaleY(String element) {
        try {
            double own = HUD_SCALES_Y.get(element).get();
            return own > 0.0D ? HudAnchors.clampScale((float) own) : hudScale(element);
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

    // ---- imposed on every player: no option, the HUD works this way

    /** The Voidframe HUD is always on. */
    public static boolean voidframeHud() {
        return true;
    }

    /** Floating damage numbers are always on. */
    public static boolean damageNumbers() {
        return true;
    }

    /** The Voidframe Tab list is always on. */
    public static boolean voidframePlayerList() {
        return true;
    }

    /** The compass and clock panels always need a compass and a clock in the inventory. */
    public static boolean hudRequireItems() {
        return true;
    }

    public static ForgeConfigSpec.BooleanValue hudAlertValue(String alert) {
        return HUD_ALERT_VALUES.get(alert);
    }

    public static boolean hudAlert(String alert) {
        try {
            return HUD_ALERT_VALUES.get(alert).get();
        } catch (IllegalStateException notLoadedYet) {
            return true;
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
