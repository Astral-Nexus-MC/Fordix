package com.omneria.fordix.wiki;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Keeps the essential of a wiki page when it is sent to a player: wikis are written for the web, and a page copied as it is
 * carries editing notes ("[Java Edition only]", "[needs testing]"), picture captions that are file names, tables that repeat the
 * same figures in three layouts, forty identical spawn rows, and sections nobody reads in a game (sounds, data values, history,
 * the other edition's text). Done when the page is served, so the stored copy stays whole and the rules can change without
 * importing again. Pure Java, unit tested.
 */
public final class PageCleaner {
    /** Rows kept of one table; the rest is counted. */
    public static final int MAX_TABLE_ROWS = 14;
    /** Pictures kept on a page: the first ones are the infobox. */
    public static final int MAX_PICTURES = 3;
    /** A page is cut after the section that passes this size (its own beginning always stays). */
    public static final int MAX_CHARS = 22_000;

    private static final Pattern NOTE = Pattern.compile("\\s*\\[(?:[^\\]]*\\b(?:only|needs?\\b|verify|citation|more information|discuss|edit|clarif|dubious|when\\?)[^\\]]*)\\]");
    private static final Pattern IN_BEDROCK = Pattern.compile("\\bIn Bedrock Edition:.*?(?=\\bIn Java Edition:|$)");
    private static final Pattern IN_JAVA = Pattern.compile("\\bIn Java Edition:\\s*");
    private static final Pattern FILE_CAPTION = Pattern.compile("^[^|]{0,80}\\.(?:png|gif|jpe?g|webp|svg)\\b.*$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> ROW_NOISE = Set.of("killed", "average", "total");
    private static final Set<String> HEADER_CELLS = Set.of("item", "default", "name", "description", "quantity", "chance", "amount", "probability");

    private PageCleaner() {
    }

    public static String clean(String body, boolean javaOnly) {
        List<String> lines = new ArrayList<>();
        for (String raw : body.split("\r?\n", -1)) {
            String line = NOTE.matcher(raw).replaceAll("").stripTrailing();
            if (javaOnly && line.startsWith("- ")) {
                line = IN_JAVA.matcher(IN_BEDROCK.matcher(line).replaceAll("")).replaceAll("").replaceAll("\\s{2,}", " ").stripTrailing();
            }
            lines.add(line);
        }
        List<Section> sections = split(lines, javaOnly);
        StringBuilder out = new StringBuilder();
        int pictures = 0;
        boolean first = true;
        for (Section section : sections) {
            if (section.dropped) {
                continue;
            }
            List<String> kept = tidy(section.body, pictures, javaOnly);
            pictures += countPictures(kept);
            boolean empty = kept.stream().allMatch(String::isBlank);
            if (empty && section.heading != null) {
                continue;
            }
            StringBuilder piece = new StringBuilder();
            if (section.heading != null) {
                piece.append(section.heading).append("\n\n");
            }
            for (String line : kept) {
                piece.append(line).append('\n');
            }
            if (!first && out.length() + piece.length() > MAX_CHARS) {
                break;
            }
            out.append(piece);
            first = false;
        }
        return out.toString().replaceAll("\n{3,}", "\n\n").strip();
    }

    // ---- sections

    private static final class Section {
        String heading;
        int level;
        boolean dropped;
        List<String> body = new ArrayList<>();
    }

    private static int levelOf(String heading) {
        int level = 0;
        while (level < heading.length() && heading.charAt(level) == '#') {
            level++;
        }
        return level;
    }

    private static String titleOf(String heading) {
        return heading.replaceFirst("^#+\\s*", "").strip().toLowerCase(Locale.ROOT);
    }

    private static List<Section> split(List<String> lines, boolean javaOnly) {
        List<Section> sections = new ArrayList<>();
        Section current = new Section();
        sections.add(current);
        int droppedLevel = 0;
        for (String line : lines) {
            if (line.startsWith("#")) {
                int level = levelOf(line);
                current = new Section();
                current.heading = line;
                current.level = level;
                sections.add(current);
                if (droppedLevel > 0 && level <= droppedLevel) {
                    droppedLevel = 0;
                }
                String title = titleOf(line);
                boolean dropIt = HtmlToMarkup.SKIPPED_SECTIONS.contains(title) || javaOnly && EditionFilter.OTHER_EDITION_SECTIONS.contains(title);
                if (droppedLevel == 0 && dropIt) {
                    droppedLevel = level;
                }
                current.dropped = droppedLevel > 0;
            } else {
                current.body.add(line);
            }
        }
        if (javaOnly) {
            for (Section section : sections) {
                section.body = withoutOtherEdition(section.body);
            }
        }
        return sections;
    }

    /** A line that only says "Bedrock Edition:" opens text for that edition, up to the "Java Edition:" line that follows it. */
    private static List<String> withoutOtherEdition(List<String> lines) {
        List<String> kept = new ArrayList<>();
        boolean skipping = false;
        for (String line : lines) {
            String text = line.strip().toLowerCase(Locale.ROOT);
            if (text.equals("bedrock edition:") || text.equals("bedrock edition") || text.equals("education edition:")) {
                skipping = true;
                continue;
            }
            if (text.equals("java edition:") || text.equals("java edition")) {
                skipping = false;
                continue;
            }
            if (!skipping) {
                kept.add(line);
            }
        }
        return kept;
    }

    // ---- inside a section

    private static int countPictures(List<String> lines) {
        return (int) lines.stream().filter(line -> line.startsWith("@image")).count();
    }

    /** Pictures, then runs of table rows (grouped, de-duplicated, cut), then blank lines. */
    private static List<String> tidy(List<String> lines, int picturesBefore, boolean javaOnly) {
        List<String> out = new ArrayList<>();
        List<List<String>> tablesSeen = new ArrayList<>();
        int pictures = picturesBefore;
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line.startsWith("@image")) {
                if (pictures++ < MAX_PICTURES) {
                    out.add(cleanPicture(line));
                }
                i++;
            } else if (isRow(line)) {
                int end = i;
                List<String> rows = new ArrayList<>();
                while (end < lines.size() && (isRow(lines.get(end)) || lines.get(end).isBlank() && end + 1 < lines.size() && isRow(lines.get(end + 1)))) {
                    if (!lines.get(end).isBlank()) {
                        rows.add(lines.get(end));
                    }
                    end++;
                }
                if (javaOnly) {
                    rows = withoutOtherEditionRows(rows);
                }
                if (isTable(rows)) {
                    // Several tables written one after the other make one run of rows: each "Item" row starts the next one.
                    for (List<String> table : splitTables(rows)) {
                        List<String> firsts = firstCells(table);
                        if (firsts.isEmpty() && table.size() <= 2) {
                            continue; // only the headings of a table whose rows were the same figures again
                        }
                        if (!repeats(firsts, tablesSeen)) {
                            tablesSeen.add(firsts);
                            out.addAll(spaced(compact(table)));
                        }
                    }
                } else {
                    for (String row : rows) {
                        out.add(row);
                        out.add("");
                    }
                }
                i = end;
            } else {
                out.add(line);
                i++;
            }
        }
        return out;
    }

    /** In a table that gives both editions, the rows between a "Bedrock Edition" row and the next "Java Edition" row are dropped. */
    private static List<String> withoutOtherEditionRows(List<String> rows) {
        List<String> kept = new ArrayList<>();
        boolean skipping = false;
        for (String row : rows) {
            String cell = row.substring(2).strip().toLowerCase(Locale.ROOT);
            if (cell.equals("bedrock edition") || cell.equals("education edition")) {
                skipping = true;
            } else if (cell.equals("java edition")) {
                skipping = false;
            } else if (!skipping) {
                kept.add(row);
            }
        }
        return kept;
    }

    private static String cleanPicture(String line) {
        int bar = line.indexOf('|');
        if (bar < 0) {
            return line;
        }
        String caption = line.substring(bar + 1).strip();
        if (FILE_CAPTION.matcher(caption).matches() || caption.toLowerCase(Locale.ROOT).contains("infobox image")) {
            return line.substring(0, bar).stripTrailing();
        }
        return line;
    }

    private static boolean isRow(String line) {
        return line.startsWith("- ") && line.length() > 2;
    }

    /** A table is a run of rows with several cells; bullets of plain text are left alone. */
    private static boolean isTable(List<String> rows) {
        long cells = rows.stream().filter(row -> row.contains(" | ")).count();
        return cells >= 2 && cells * 2 >= rows.size();
    }

    private static List<List<String>> splitTables(List<String> rows) {
        List<List<String>> tables = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String row : rows) {
            String first = firstCell(row).toLowerCase(Locale.ROOT);
            boolean starts = first.equals("item") || first.startsWith("item ");
            if (starts && !current.isEmpty()) {
                tables.add(current);
                current = new ArrayList<>();
            }
            current.add(row);
        }
        if (!current.isEmpty()) {
            tables.add(current);
        }
        return tables;
    }

    private static String firstCell(String row) {
        String text = row.substring(2);
        int bar = text.indexOf(" | ");
        int colon = text.indexOf(" : ");
        int cut = bar < 0 ? colon : colon < 0 ? bar : Math.min(bar, colon);
        return (cut < 0 ? text : text.substring(0, cut)).strip();
    }

    private static List<String> firstCells(List<String> rows) {
        List<String> cells = new ArrayList<>();
        for (String row : rows) {
            String cell = firstCell(row).toLowerCase(Locale.ROOT);
            if (!cell.isEmpty() && !HEADER_CELLS.contains(cell) && !cell.matches("[-+0-9.,/⁄%() ×x–]+")) {
                cells.add(cell);
            }
        }
        return cells;
    }

    /** True when most of the table's first cells were already in a table of the same section: the same figures written again. */
    private static boolean repeats(List<String> firsts, List<List<String>> seen) {
        if (firsts.size() < 2) {
            return false;
        }
        for (List<String> earlier : seen) {
            long shared = firsts.stream().filter(earlier::contains).count();
            if (shared * 10 >= firsts.size() * 7L) {
                return true;
            }
        }
        return false;
    }

    private static List<String> spaced(List<String> rows) {
        List<String> out = new ArrayList<>();
        for (String row : rows) {
            out.add(row);
            out.add("");
        }
        return out;
    }

    /** Rows without totals, the ones that only differ by their first cell merged into one, and at most {@link #MAX_TABLE_ROWS}. */
    static List<String> compact(List<String> rows) {
        List<String> kept = new ArrayList<>();
        for (String row : rows) {
            if (!ROW_NOISE.contains(firstCell(row).toLowerCase(Locale.ROOT).replaceAll("\\s*:.*$", ""))) {
                kept.add(row);
            }
        }
        Map<String, List<String>> byTail = new LinkedHashMap<>();
        List<String> order = new ArrayList<>();
        for (String row : kept) {
            String text = row.substring(2);
            int bar = text.indexOf(" | ");
            String tail = bar < 0 ? "" : text.substring(bar);
            String key = bar < 0 ? "\u0000" + row : tail;
            if (!byTail.containsKey(key)) {
                order.add(key);
            }
            byTail.computeIfAbsent(key, k -> new ArrayList<>()).add(bar < 0 ? text : text.substring(0, bar));
        }
        List<String> merged = new ArrayList<>();
        for (String key : order) {
            List<String> heads = byTail.get(key);
            if (key.startsWith("\u0000")) {
                merged.add("- " + heads.get(0));
            } else if (heads.size() >= 3) {
                Set<String> shown = new LinkedHashSet<>(heads.subList(0, Math.min(6, heads.size())));
                String names = String.join(", ", shown) + (heads.size() > 6 ? " +" + (heads.size() - 6) : "");
                merged.add("- " + names + key);
            } else {
                for (String head : heads) {
                    merged.add("- " + head + key);
                }
            }
        }
        if (merged.size() > MAX_TABLE_ROWS) {
            int more = merged.size() - MAX_TABLE_ROWS;
            merged = new ArrayList<>(merged.subList(0, MAX_TABLE_ROWS));
            merged.add("- … +" + more);
        }
        return merged;
    }
}
