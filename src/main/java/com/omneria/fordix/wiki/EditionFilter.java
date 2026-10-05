package com.omneria.fordix.wiki;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Keeps the Java Edition of a text that gives both ("Attack damage: JE: 6 BE: 7" becomes "Attack damage: 6"), for wikis of
 * a game that exists in several editions while the server runs only one. Pure Java, unit tested.
 */
public final class EditionFilter {
    /** Sections about other editions, dropped whole when only Java is kept. */
    public static final Set<String> OTHER_EDITION_SECTIONS = Set.of("bedrock edition", "bedrock", "education edition", "minecraft education",
            "legacy console edition", "console edition", "new nintendo 3ds edition", "pocket edition", "pe", "be", "bedrock and education editions");

    private static final Pattern BEDROCK_PART = Pattern.compile("\\b(?:BE|Bedrock Edition):\\s*.*?(?=\\b(?:JE|Java Edition):|$)");
    private static final Pattern JAVA_LABEL = Pattern.compile("\\b(?:JE|Java Edition):\\s*");

    private static final Pattern BEDROCK_CELL = Pattern.compile("\\b(?:BE|Bedrock Edition):?\\s.*?(?=\\b(?:JE|Java Edition)\\b|$)");
    private static final Pattern JAVA_CELL_LABEL = Pattern.compile("\\b(?:JE|Java Edition):?\\s+");

    private EditionFilter() {
    }

    /** For a table cell, which is only values: "Java Edition Iron: 250 Bedrock Edition Iron: 251" becomes "Iron: 250". */
    public static String javaOnlyCell(String text) {
        String kept = BEDROCK_CELL.matcher(text).replaceAll("");
        return JAVA_CELL_LABEL.matcher(kept).replaceAll("").replaceAll("\\s{2,}", " ").strip();
    }

    public static String javaOnly(String text) {
        String kept = BEDROCK_PART.matcher(text).replaceAll("");
        return JAVA_LABEL.matcher(kept).replaceAll("").replaceAll("\\s{2,}", " ").strip();
    }
}
