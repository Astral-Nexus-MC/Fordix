package com.omneria.fordix;

import com.omneria.fordix.client.guide.GuideEntry;
import com.omneria.fordix.notification.NotificationTemplate;
import com.omneria.fordix.rarity.RarityRules;
import com.omneria.fordix.welcome.WelcomeStep;
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
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HUD_REQUIRED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HUD_FORBIDDEN;
    public static final ForgeConfigSpec.ConfigValue<String> HUD_PROPOSED;

    public static final ForgeConfigSpec.ConfigValue<String> WIKI_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> WIKI_TRANSLATION_MODEL;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RARITY_RULES;
    public static final ForgeConfigSpec.BooleanValue WELCOME_BOOK_GIVE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WELCOME_BOOK_STEPS;
    public static final ForgeConfigSpec.ConfigValue<String> WELCOME_BOOK_REWARD;

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

        builder.comment("Rules for the HUD on this server (players keep everything else).")
                .push("hud");
        HUD_REQUIRED = builder.comment("Elements players can never hide: \"health\", \"hunger\", \"armor\", \"air\", \"experience\", \"boss\",",
                        "\"minecraft:hotbar\", \"minecraft:chat_panel\", \"fordix:coordinates\"... (the ids the HUD layout screen uses).")
                .defineList("required", List.of("health", "minecraft:hotbar"), value -> value instanceof String text && !text.isBlank() && text.length() <= 96);
        HUD_FORBIDDEN = builder.comment("Elements that are always hidden here (streamer or competition servers): same ids as above.")
                .defineList("forbidden", List.of(), value -> value instanceof String text && !text.isBlank() && text.length() <= 96);
        HUD_PROPOSED = builder.comment("A HUD layout to suggest: paste the share code (FXH1-...) copied from the HUD layout screen.",
                        "Players find it in the last profile slot (F8) unless they filled that slot themselves. Empty: no suggestion.")
                .define("proposed", "");
        builder.pop();

        builder.comment("Item rarity, the same for every player.")
                .push("rarities");
        RARITY_RULES = builder.comment("One rule per line: \"pattern | TIER\", the first matching rule wins. TIER is COMMON, UNCOMMON, RARE, EPIC or LEGENDARY.",
                        "In a pattern, * stands for any text and {a,b} for a choice: \"twilightforest:knightmetal_*\", \"minecraft:iron_{sword,axe}\".",
                        "Strong enchantments raise a ruled item one or two tiers. Items no rule matches keep their own rarity.")
                .defineList("rules", RarityRules.DEFAULT_LINES, value -> value instanceof String text && RarityRules.parseLine(text) != null);
        builder.pop();

        builder.comment("The welcome book: an item with a checklist for new players (right-click to open it).")
                .push("welcomeBook");
        WELCOME_BOOK_GIVE = builder.comment("Give the book to a player the first time he joins (/fordix book gives it by hand).")
                .define("giveOnFirstJoin", true);
        WELCOME_BOOK_STEPS = builder.comment("One step per line: \"label | condition\" or \"label | condition | action\".",
                        "Conditions: advancement:<id>, command:<text> (a command starting with it), guides (opens the Guides screen),",
                        "manual (the player ticks it). The action is a button: command:/team join or url:https://...")
                .defineList("steps", List.of(
                        "Ouvrir les Guides depuis le menu pause | guides",
                        "Miner de la pierre | advancement:minecraft:story/mine_stone",
                        "Fabriquer des outils en pierre | advancement:minecraft:story/upgrade_tools",
                        "Lire les regles du serveur | manual"
                ), value -> value instanceof String text && WelcomeStep.parse(text) != null);
        WELCOME_BOOK_REWARD = builder.comment("Command run once by the server when a player has ticked every step; %player% is his name.",
                        "Example: give %player% minecraft:diamond 3. Empty for no reward.")
                .define("reward", "");
        builder.pop();
        builder.comment("Automatic translation of the wiki pages kept on the server (/fordix wiki autotranslate).")
                .push("wiki");
        WIKI_API_KEY = builder.comment("Claude API key used to translate wiki pages. Empty: the ANTHROPIC_API_KEY environment variable of the server.",
                "Keep this file private: the key is read on the server only and never sent to players.")
                .define("anthropicApiKey", "");
        WIKI_TRANSLATION_MODEL = builder.comment("Claude model that translates the pages.").define("translationModel", "claude-opus-5-5");
        builder.pop();
        SPEC = builder.build();
    }

    private FordixServerConfig() {
    }

    private static boolean isTemplate(Object value) {
        return value instanceof String text && NotificationTemplate.parse(text) != null;
    }
}
