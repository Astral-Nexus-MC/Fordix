package com.omneria.fordix;

import com.omneria.fordix.client.guide.GuideEntry;
import com.omneria.fordix.notification.NotificationTemplate;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Server-side options in config/fordix-common.toml: automatic notifications. The
 * file is watched, so edits apply without a restart. Notification lines use the
 * "style | Title | message" format with {player}, {server}, {version}, {online}
 * and {max} placeholders.
 */
public final class FordixServerConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> SERVER_NAME;
    public static final ForgeConfigSpec.ConfigValue<String> PACK_VERSION;

    public static final ForgeConfigSpec.BooleanValue WELCOME_ENABLED;
    public static final ForgeConfigSpec.IntValue WELCOME_DELAY_SECONDS;
    public static final ForgeConfigSpec.IntValue WELCOME_DURATION_SECONDS;
    public static final ForgeConfigSpec.ConfigValue<String> WELCOME;
    public static final ForgeConfigSpec.ConfigValue<String> FIRST_JOIN;

    public static final ForgeConfigSpec.IntValue ANNOUNCEMENT_INTERVAL_MINUTES;
    public static final ForgeConfigSpec.IntValue ANNOUNCEMENT_DURATION_SECONDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ANNOUNCEMENTS;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUIDES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Automatic notifications. Lines are \"style | Title | message\" (style: info, success, warning, error;",
                "style and message optional). Placeholders: {player}, {server}, {version}, {online}, {max}.")
                .push("notifications");

        SERVER_NAME = builder.comment("Shown as {server}.").define("serverName", "Astral Nexus");
        PACK_VERSION = builder.comment("Shown as {version} - bump it with each modpack release.").define("packVersion", "1.0.0");

        builder.push("welcome");
        WELCOME_ENABLED = builder.comment("Greet players when they join.").define("enabled", true);
        WELCOME_DELAY_SECONDS = builder.comment("Wait this long after joining, so the client has finished loading.")
                .defineInRange("delaySeconds", 3, 0, 60);
        WELCOME_DURATION_SECONDS = builder.defineInRange("durationSeconds", 6, 1, 60);
        WELCOME = builder.comment("Sent on every join.")
                .define("notification", "success | Bienvenue sur {server} v{version} | Bon jeu, {player} !", FordixServerConfig::isTemplate);
        FIRST_JOIN = builder.comment("Sent instead on a player's very first join. Leave empty to always use the one above.")
                .define("firstJoinNotification", "success | Bienvenue sur {server} ! | Premiere connexion : bienvenue parmi nous, {player} !",
                        value -> value instanceof String text && (text.isEmpty() || NotificationTemplate.parse(text) != null));
        builder.pop();

        builder.push("announcements");
        ANNOUNCEMENT_INTERVAL_MINUTES = builder.comment("Send the next announcement to everyone every this many minutes. 0 turns announcements off.")
                .defineInRange("intervalMinutes", 0, 0, 1440);
        ANNOUNCEMENT_DURATION_SECONDS = builder.defineInRange("durationSeconds", 8, 1, 60);
        ANNOUNCEMENTS = builder.comment("Sent in turn, one per interval.")
                .defineList("messages", List.of(
                        "info | Astuce | Tape /help pour voir les commandes disponibles.",
                        "info | {server} | {online} joueur(s) connecte(s) - bon jeu !"
                ), FordixServerConfig::isTemplate);
        builder.pop();

        builder.pop();

        builder.comment("Guide tabs shown to every player along the right edge of their inventory.")
                .push("guides");
        GUIDES = builder.comment("One per line: \"modid | Label | icon item | action\" (actions: ponder, patchouli:<book id>,",
                        "url:<https link>, command:<command>). A tab only shows for players who have its mod.",
                        "Leave empty to let each player use the guides of their own config.")
                .defineList("tabs", List.of(), value -> value instanceof String text && GuideEntry.parse(text) != null);
        builder.pop();
        SPEC = builder.build();
    }

    private FordixServerConfig() {
    }

    private static boolean isTemplate(Object value) {
        return value instanceof String text && NotificationTemplate.parse(text) != null;
    }
}
