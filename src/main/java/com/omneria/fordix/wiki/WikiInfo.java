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

    public WikiInfo() {
    }

    public WikiInfo(String id, String title, String source, String license) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.license = license;
    }

    public static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9][a-z0-9_-]{0,31}");
    }
}
