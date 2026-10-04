package com.omneria.fordix.client.chat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a chat line is, decided from its translation key (vanilla and most servers send translatable components) or,
 * for a plain-text line, from its look. Pure Java, unit tested.
 */
public enum ChatKind {
    /** A player talking. */
    PLAYER,
    /** An announcement from the server itself ("/say", operator broadcasts). */
    SERVER,
    /** Everything the game says on its own: advancements, joins, deaths... */
    SYSTEM,
    /** Feedback of a command. */
    COMMAND,
    /** A private message (/msg, /tell, /w) received or sent. */
    PRIVATE;

    private static final Pattern PLAIN_PLAYER = Pattern.compile("^<([^<>\\s]{1,32})> (.*)$", Pattern.DOTALL);

    public static ChatKind classify(String translationKey, String plainText) {
        if (translationKey != null) {
            if (translationKey.equals("chat.type.text") || translationKey.startsWith("chat.type.team")
                    || translationKey.equals("chat.type.emote")) {
                return PLAYER;
            }
            if (translationKey.equals("chat.type.announcement") || translationKey.equals("chat.type.admin")) {
                return SERVER;
            }
            if (translationKey.startsWith("commands.message.display.")) {
                return PRIVATE;
            }
            if (translationKey.startsWith("commands.") || translationKey.startsWith("command.")) {
                return COMMAND;
            }
            return SYSTEM;
        }
        String text = plainText == null ? "" : plainText;
        if (PLAIN_PLAYER.matcher(text).matches()) {
            return PLAYER;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains(" whispers to you:") || lower.contains(" vous chuchote")) {
            return PRIVATE;
        }
        if (lower.startsWith("[server]") || lower.startsWith("[serveur]")) {
            return SERVER;
        }
        return SYSTEM;
    }

    /** True for a private message somebody sent to the player (not the echo of one the player sent). */
    public static boolean isIncomingPrivate(String translationKey, String plainText) {
        return "commands.message.display.incoming".equals(translationKey)
                || translationKey == null && plainText != null && plainText.toLowerCase(Locale.ROOT).contains(" whispers to you:");
    }

    /** {name, message} of a plain "&lt;Name&gt; message" line, or null. */
    public static String[] splitPlain(String plainText) {
        Matcher matcher = PLAIN_PLAYER.matcher(plainText == null ? "" : plainText);
        return matcher.matches() ? new String[]{matcher.group(1), matcher.group(2)} : null;
    }
}
