package com.omneria.fordix.client.guide;

/**
 * One tab of the inventory "Guides" column, written in the client config as
 * {@code "modid | Label | icon item | action"}. The tab only shows when {@code modid}
 * is loaded ("minecraft" always is). Actions:
 * <ul>
 *     <li>{@code ponder} - Create's Ponder index;</li>
 *     <li>{@code patchouli:<book id>} - a Patchouli guide book;</li>
 *     <li>{@code url:<https link>} - a web page, after vanilla's link confirmation; {@code {objet}} in the link is
 *     replaced by the wiki page of the item the player was on (e.g. "Iron_Sword"), or by "Minecraft_Wiki" without an item;</li>
 *     <li>{@code command:<command>} - a command run as the player, without the slash.</li>
 * </ul>
 * Plain Java, unit tested.
 */
public record GuideEntry(String modId, String label, String icon, Action action, String target) {

    public enum Action {
        PONDER,
        PATCHOULI,
        URL,
        COMMAND
    }

    /** Parses a config line; null when a part is missing or the action is unknown or unsafe. */
    public static GuideEntry parse(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", 4);
        if (parts.length != 4) {
            return null;
        }
        String modId = parts[0].trim();
        String label = parts[1].trim();
        String icon = parts[2].trim();
        String action = parts[3].trim();
        if (modId.isEmpty() || label.isEmpty() || icon.isEmpty()) {
            return null;
        }
        if (action.equals("ponder")) {
            return new GuideEntry(modId, label, icon, Action.PONDER, "");
        }
        int colon = action.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String kind = action.substring(0, colon);
        String target = action.substring(colon + 1).trim();
        if (target.isEmpty()) {
            return null;
        }
        return switch (kind) {
            case "patchouli" -> new GuideEntry(modId, label, icon, Action.PATCHOULI, target);
            // Only web links: no file:// or other schemes the OS would hand to arbitrary programs.
            case "url" -> target.startsWith("https://") || target.startsWith("http://")
                    ? new GuideEntry(modId, label, icon, Action.URL, target) : null;
            case "command" -> new GuideEntry(modId, label, icon, Action.COMMAND,
                    target.startsWith("/") ? target.substring(1) : target);
            default -> null;
        };
    }

    /** "iron_sword" becomes the wiki page title "Iron_Sword". */
    public static String wikiPage(String itemPath) {
        StringBuilder title = new StringBuilder();
        for (String word : itemPath.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (title.length() > 0) {
                title.append('_');
            }
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return title.toString();
    }

    /** The link to open: {@code {objet}} replaced by the item's wiki page, or by the wiki's main page when null. */
    public static String resolveUrl(String template, String itemPath) {
        return template.replace("{objet}", itemPath == null || itemPath.isBlank() ? "Minecraft_Wiki" : wikiPage(itemPath));
    }
}
