package com.omneria.fordix.hud;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The HUD situations table a server can impose, as lines of the server config. No Minecraft dependency (it is read by the
 * server config and by the client), unit tested.
 * <ul>
 *   <li>A table line: {@code panel | explore | combat | mining | boss | mount}, each a percent 0 to 100
 *       ({@code coordinates | 100 | 30 | 100 | 30 | 100}).</li>
 *   <li>A profile line: {@code situation | FXH1-code}, the layout loaded while the player is in that situation.</li>
 * </ul>
 */
public final class SituationTable {
    public static final List<String> SITUATIONS = List.of("explore", "combat", "mining", "boss", "mount");

    public record Row(String panel, float[] factors) {
    }

    private SituationTable() {
    }

    /** The row of a table line, or {@code null} when the line is not valid. */
    public static Row parseRow(Object line) {
        if (!(line instanceof String text)) {
            return null;
        }
        String[] parts = text.split("\\|");
        if (parts.length != SITUATIONS.size() + 1 || parts[0].isBlank()) {
            return null;
        }
        float[] factors = new float[SITUATIONS.size()];
        for (int i = 0; i < factors.length; i++) {
            try {
                int percent = Integer.parseInt(parts[i + 1].replace("%", "").strip());
                if (percent < 0 || percent > 100) {
                    return null;
                }
                factors[i] = percent / 100.0F;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return new Row(parts[0].strip(), factors);
    }

    /** {@code {situation, code}} of a profile line, or {@code null}. */
    public static String[] parseProfile(Object line) {
        if (!(line instanceof String text)) {
            return null;
        }
        int bar = text.indexOf('|');
        if (bar < 0) {
            return null;
        }
        String situation = text.substring(0, bar).strip().toLowerCase(Locale.ROOT);
        String code = text.substring(bar + 1).strip();
        if (!SITUATIONS.contains(situation) || code.isEmpty() || code.length() > 30_000) {
            return null;
        }
        return new String[]{situation, code};
    }

    public static Map<String, float[]> rows(List<String> lines) {
        Map<String, float[]> table = new LinkedHashMap<>();
        for (String line : lines) {
            Row row = parseRow(line);
            if (row != null) {
                table.put(row.panel(), row.factors());
            }
        }
        return table;
    }

    /** One code per situation (by {@link #SITUATIONS} order), empty text for none. */
    public static String[] profiles(List<String> lines) {
        String[] codes = new String[SITUATIONS.size()];
        java.util.Arrays.fill(codes, "");
        for (String line : lines) {
            String[] parsed = parseProfile(line);
            if (parsed != null) {
                codes[SITUATIONS.indexOf(parsed[0])] = parsed[1];
            }
        }
        return codes;
    }
}
