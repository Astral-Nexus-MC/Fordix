package com.omneria.fordix.wiki;

/** One wiki kept on the server: where it comes from and under which licence its text may be shown. */
public final class WikiInfo {
    public String id = "";
    public String title = "";
    /** Address of the wiki (for the "source" line and for the importer). */
    public String source = "";
    public String license = "";
    /** The MediaWiki API address that worked for this wiki, once known. */
    public String api = "";
    /** The game version this wiki is kept for, shown with the licence ("1.20.1"). */
    public String gameVersion = "";
    /** "yyyy-MM-dd": pages are copied as the wiki had them on that day (newer content is left out), or empty for the latest. */
    public String asOf = "";
    /** Keep only the Java Edition of a text that gives several editions. */
    public boolean javaOnly;

    public WikiInfo() {
    }

    public WikiInfo(String id, String title, String source, String license) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.license = license;
    }

    public static boolean validDate(String date) {
        return date != null && date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    /** The licence line shown under a page: licence, game version and the date the wiki is frozen at. */
    public String credit() {
        StringBuilder out = new StringBuilder(license);
        if (!gameVersion.isEmpty()) {
            out.append(out.length() > 0 ? " \u00B7 " : "").append(gameVersion);
        }
        if (!asOf.isEmpty()) {
            out.append(out.length() > 0 ? " \u00B7 " : "").append("wiki ").append(asOf);
        }
        return out.toString();
    }

    public static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9][a-z0-9_-]{0,31}");
    }
}
