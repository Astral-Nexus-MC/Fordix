package com.omneria.fordix.wiki;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the HTML MediaWiki returns for a page ({@code action=parse}) into the guide page markup
 * ({@link com.omneria.fordix.guide.PageMarkup}): headings, paragraphs, bullets, one line per table row, pictures, and a list
 * of the pages the text links to. Navigation boxes, tables of contents, references and the like are dropped, and so are the
 * long reference sections (history, trivia, gallery...). Pure Java, unit tested.
 */
public final class HtmlToMarkup {
    /** A picture met in the page; the caller decides whether it is kept and under which file name. */
    public record Image(String src, String alt, int width, int height) {
    }

    public record Result(String markup, List<String> links) {
    }

    private static final int MAX_MARKUP = 90_000;
    private static final int MAX_TABLE_ROWS = 40;
    private static final int MAX_ROW_TEXT = 200;
    private static final int MAX_LINKS = 60;
    private static final int MIN_IMAGE_SIZE = 40;
    private static final Pattern TOKEN = Pattern.compile("<!--.*?-->|<(/?)([a-zA-Z][a-zA-Z0-9]*)((?:[^>\"']|\"[^\"]*\"|'[^']*')*)>|([^<]+)|<", Pattern.DOTALL);
    private static final Pattern ATTRIBUTE = Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))");
    private static final Set<String> VOID = Set.of("img", "br", "hr", "input", "meta", "link", "wbr", "source", "col");
    private static final Set<String> SKIPPED_TAGS = Set.of("script", "style", "noscript", "form", "button", "svg", "iframe", "select");
    private static final Set<String> BLOCK = Set.of("p", "div", "section", "ul", "ol", "dl", "table", "figure", "figcaption",
            "blockquote", "pre", "center");
    private static final List<String> SKIPPED_CLASSES = List.of("navbox", "toc", "mw-editsection", "reference", "reflist", "noprint",
            "hatnote", "catlinks", "printfooter", "mw-empty-elt", "hidden-alt-text", "sr-only", "mbox", "metadata", "mw-authority-control",
            "infobox-invimages", "invslot", "page-footer", "mw-references-wrap");
    static final Set<String> SKIPPED_SECTIONS = Set.of("history", "trivia", "gallery", "references", "external links", "notes",
            "issues", "sounds", "data values", "achievements", "advancements", "video", "videos", "navigation", "see also", "footnotes",
            "bugs", "screenshots", "in other languages", "notes and references", "sources", "category",
            "historique", "galerie", "anecdotes", "r\u00e9f\u00e9rences", "liens externes", "voir aussi", "dans d'autres langues",
            "bogues", "sons", "succ\u00e8s", "progr\u00e8s", "vid\u00e9o", "vid\u00e9os", "notes et r\u00e9f\u00e9rences");

    private record Element(String name, boolean skip) {
    }

    private final Function<Image, String> imageNamer;
    private boolean javaOnly;
    private final Deque<Element> stack = new ArrayDeque<>();
    private final List<String> blocks = new ArrayList<>();
    private final Set<String> links = new LinkedHashSet<>();
    private final StringBuilder line = new StringBuilder();
    private int size;
    private int skipDepth;
    private boolean skippingSection;
    private int sectionLevel;
    private int headingLevel;
    private final StringBuilder heading = new StringBuilder();
    private int tableDepth;
    private int tableRows;
    private boolean inRow;
    private List<StringBuilder> cells;
    private StringBuilder cell;
    private boolean cellIsHeader;
    private final List<Boolean> cellHeaders = new ArrayList<>();
    private int listItems;
    private boolean inItem;

    private HtmlToMarkup(Function<Image, String> imageNamer) {
        this.imageNamer = imageNamer;
    }

    /**
     * Converts a page's HTML. {@code imageNamer} gets each picture worth keeping (not an icon) and returns the file name it
     * will be saved under, or null to leave it out.
     */
    public static Result convert(String html, Function<Image, String> imageNamer) {
        return convert(html, imageNamer, false);
    }

    /** Same, keeping only the Java Edition of what is written for several editions when {@code javaOnly}. */
    public static Result convert(String html, Function<Image, String> imageNamer, boolean javaOnly) {
        HtmlToMarkup converter = new HtmlToMarkup(imageNamer);
        converter.javaOnly = javaOnly;
        converter.run(html == null ? "" : html);
        return new Result(String.join("\n\n", converter.blocks), List.copyOf(converter.links));
    }

    private void run(String html) {
        Matcher matcher = TOKEN.matcher(html);
        while (matcher.find() && size < MAX_MARKUP) {
            if (matcher.group(2) != null) {
                String name = matcher.group(2).toLowerCase(Locale.ROOT);
                String attributes = matcher.group(3) == null ? "" : matcher.group(3);
                if (!matcher.group(1).isEmpty()) {
                    end(name);
                } else {
                    start(name, attributes, attributes.endsWith("/") || VOID.contains(name));
                }
            } else if (matcher.group(4) != null) {
                text(matcher.group(4));
            }
        }
        flushLine();
    }

    private boolean skipping() {
        return skipDepth > 0 || skippingSection;
    }

    // ---- tags

    private void start(String name, String attributes, boolean selfClosing) {
        String classes = attribute(attributes, "class").toLowerCase(Locale.ROOT);
        if (selfClosing) {
            if (name.equals("img")) {
                image(attributes);
            } else if (name.equals("br") || name.equals("hr")) {
                if (!skipping()) {
                    space();
                }
            }
            return;
        }
        boolean skip = SKIPPED_TAGS.contains(name) || matchesSkippedClass(classes)
                || (name.equals("sup") && classes.contains("reference")) || attribute(attributes, "id").equals("toc");
        stack.push(new Element(name, skip));
        if (skip) {
            skipDepth++;
            return;
        }
        if (skipDepth > 0) {
            return;
        }
        if (name.matches("h[1-6]")) {
            flushLine();
            headingLevel = name.charAt(1) - '0';
            heading.setLength(0);
        } else if (name.equals("a")) {
            link(attribute(attributes, "href"));
        } else if (name.equals("li") || name.equals("dd") || name.equals("dt")) {
            flushLine();
            inItem = true;
        } else if (name.equals("table")) {
            flushLine();
            tableDepth++;
            tableRows = 0;
        } else if (name.equals("tr") && tableDepth > 0) {
            inRow = true;
            cells = new ArrayList<>();
            cellHeaders.clear();
        } else if ((name.equals("td") || name.equals("th")) && inRow) {
            cell = new StringBuilder();
            cellIsHeader = name.equals("th");
        } else if (BLOCK.contains(name) && tableDepth == 0) {
            flushLine();
        }
    }

    private void end(String name) {
        // Close up to the matching element (HTML from the wiki is regular, but be forgiving).
        Element[] open = stack.toArray(new Element[0]);
        int found = -1;
        for (int i = 0; i < open.length; i++) {
            if (open[i].name().equals(name)) {
                found = i;
                break;
            }
        }
        if (found < 0) {
            return;
        }
        for (int i = 0; i <= found; i++) {
            Element element = stack.pop();
            if (element.skip()) {
                skipDepth--;
                continue;
            }
            if (skipDepth == 0) {
                close(element.name());
            }
        }
    }

    private void close(String name) {
        if (name.matches("h[1-6]")) {
            endHeading();
        } else if (name.equals("li") || name.equals("dd") || name.equals("dt")) {
            flushLine();
            inItem = false;
        } else if ((name.equals("td") || name.equals("th")) && cell != null) {
            cells.add(cell);
            cellHeaders.add(cellIsHeader);
            cell = null;
        } else if (name.equals("tr") && inRow) {
            inRow = false;
            row();
        } else if (name.equals("table") && tableDepth > 0) {
            tableDepth--;
        } else if (BLOCK.contains(name) && tableDepth == 0) {
            flushLine();
        }
    }

    private boolean matchesSkippedClass(String classes) {
        if (classes.isEmpty()) {
            return false;
        }
        for (String word : classes.split("\\s+")) {
            if (SKIPPED_CLASSES.contains(word) || word.startsWith("navbox")) {
                return true;
            }
        }
        return false;
    }

    // ---- content

    private void text(String raw) {
        if (skipDepth > 0 || skippingSection && headingLevel == 0) {
            return;
        }
        String decoded = decode(raw).replace('\u00A0', ' ').replaceAll("[\\u200B-\\u200F\\uFEFF]", "").replaceAll("\\s+", " ");
        if (decoded.isBlank() && line.length() == 0 && heading.length() == 0 && cell == null) {
            return;
        }
        if (headingLevel > 0) {
            heading.append(decoded);
        } else if (cell != null) {
            cell.append(decoded);
        } else {
            line.append(decoded);
        }
    }

    private void space() {
        if (headingLevel > 0) {
            heading.append(' ');
        } else if (cell != null) {
            cell.append(' ');
        } else {
            line.append(' ');
        }
    }

    private void endHeading() {
        String text = heading.toString().replace("[edit]", "").replaceAll("\\s+", " ").strip();
        int level = headingLevel;
        headingLevel = 0;
        heading.setLength(0);
        if (skippingSection && level > sectionLevel) {
            return;
        }
        skippingSection = false;
        if (text.isEmpty()) {
            return;
        }
        if (SKIPPED_SECTIONS.contains(text.toLowerCase(Locale.ROOT)) || javaOnly && EditionFilter.OTHER_EDITION_SECTIONS.contains(text.toLowerCase(Locale.ROOT))) {
            skippingSection = true;
            sectionLevel = level;
            return;
        }
        add("#".repeat(Math.min(3, Math.max(1, level - 1))) + " " + text);
    }

    /** One row of a table as one bullet: "label : value" for a header and a cell, cells joined by " | " otherwise. */
    private void row() {
        if (cells == null || cells.isEmpty() || ++tableRows > MAX_TABLE_ROWS) {
            return;
        }
        List<String> texts = new ArrayList<>();
        for (StringBuilder builder : cells) {
            // Data modules leave their JSON in a cell: the text stops where it starts.
            String text = builder.toString().replaceAll("\\{\\s*\".*$", "").replaceAll("\\s+", " ").strip();
            if (!text.isEmpty()) {
                texts.add(text);
            }
        }
        if (texts.isEmpty()) {
            return;
        }
        if (javaOnly) {
            texts.replaceAll(EditionFilter::javaOnlyCell);
            texts.removeIf(String::isEmpty);
            if (texts.isEmpty()) {
                return;
            }
        }
        String joined = texts.size() == 2 && cellHeaders.get(0) ? texts.get(0) + " : " + texts.get(1) : String.join(" | ", texts.subList(0, Math.min(6, texts.size())));
        add("- " + (joined.length() > MAX_ROW_TEXT ? joined.substring(0, MAX_ROW_TEXT - 1) + "…" : joined));
    }

    private void flushLine() {
        String text = line.toString().replaceAll("\\s+", " ").strip();
        line.setLength(0);
        if (javaOnly) {
            text = EditionFilter.javaOnly(text);
        }
        // Some wikis print the JSON of their data modules as text: not a paragraph.
        if (text.isEmpty() || text.equals("[edit]") || text.startsWith("{ \"") || text.startsWith("[ {")) {
            return;
        }
        add(inItem ? "- " + text : text);
    }

    private void add(String block) {
        if (skipping()) {
            return;
        }
        blocks.add(block);
        size += block.length() + 2;
    }

    private void image(String attributes) {
        if (skipping() || tableDepth > 0 || imageNamer == null) {
            return;
        }
        String src = attribute(attributes, "data-src");
        if (src.isEmpty()) {
            src = attribute(attributes, "src");
        }
        if (src.isEmpty() || src.startsWith("data:")) {
            return;
        }
        int width = number(attribute(attributes, "width"));
        int height = number(attribute(attributes, "height"));
        if (width > 0 && width < MIN_IMAGE_SIZE || height > 0 && height < MIN_IMAGE_SIZE) {
            return;
        }
        String alt = decode(attribute(attributes, "alt")).replaceAll("\\s+", " ").strip();
        String name = imageNamer.apply(new Image(src, alt, width, height));
        if (name != null) {
            flushLine();
            add("@image " + name + (alt.isEmpty() ? "" : " | " + alt.replace('|', '/')));
        }
    }

    private void link(String href) {
        if (href.isEmpty() || links.size() >= MAX_LINKS || skippingSection) {
            return;
        }
        String page = pageOf(href);
        if (page != null) {
            links.add(page);
        }
    }

    /** The wiki page a link points to ("/wiki/Naga", "/w/Naga#Drops", "/index.php/Naga", "index.php?title=Naga"), or null for anything else. */
    public static String pageOf(String href) {
        String path = decode(href);
        int hash = path.indexOf('#');
        if (hash >= 0) {
            path = path.substring(0, hash);
        }
        int title = path.indexOf("title=");
        if (title >= 0) {
            path = path.substring(title + 6);
            int amp = path.indexOf('&');
            path = amp < 0 ? path : path.substring(0, amp);
        } else if (path.contains("/index.php/")) {
            path = path.substring(path.indexOf("/index.php/") + 11);
        } else if (path.contains("/wiki/")) {
            path = path.substring(path.indexOf("/wiki/") + 6);
        } else if (path.startsWith("/w/")) {
            path = path.substring(3);
        } else {
            return null;
        }
        try {
            path = java.net.URLDecoder.decode(path.replace("+", "%2B"), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        path = path.replace(' ', '_').strip();
        // Other namespaces (File:, Category:, Special:, Talk:...) are not articles.
        if (path.isEmpty() || path.length() > 120 || path.contains("?") || path.matches("^[A-Za-z_ ]{2,20}:.*")) {
            return null;
        }
        return path;
    }

    // ---- helpers

    private static int number(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String attribute(String attributes, String name) {
        Matcher matcher = ATTRIBUTE.matcher(attributes);
        while (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase(name)) {
                return matcher.group(2) != null ? matcher.group(2) : matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
            }
        }
        return "";
    }

    public static String decode(String text) {
        if (text.indexOf('&') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        Matcher matcher = Pattern.compile("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);").matcher(text);
        int last = 0;
        while (matcher.find()) {
            out.append(text, last, matcher.start());
            String entity = matcher.group(1);
            String replacement = null;
            if (entity.startsWith("#x")) {
                replacement = codePoint(entity.substring(2), 16);
            } else if (entity.startsWith("#")) {
                replacement = codePoint(entity.substring(1), 10);
            } else {
                replacement = switch (entity) {
                    case "amp" -> "&";
                    case "lt" -> "<";
                    case "gt" -> ">";
                    case "quot" -> "\"";
                    case "apos" -> "'";
                    case "nbsp" -> " ";
                    case "ndash" -> "\u2013";
                    case "mdash" -> "\u2014";
                    case "hellip" -> "\u2026";
                    case "times" -> "\u00D7";
                    case "rarr" -> "\u2192";
                    case "larr" -> "\u2190";
                    case "middot" -> "\u00B7";
                    case "deg" -> "\u00B0";
                    default -> null;
                };
            }
            out.append(replacement == null ? matcher.group() : replacement);
            last = matcher.end();
        }
        out.append(text.substring(last));
        return out.toString();
    }

    private static String codePoint(String digits, int radix) {
        try {
            int value = Integer.parseInt(digits, radix);
            return Character.isValidCodePoint(value) ? new String(Character.toChars(value)) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
