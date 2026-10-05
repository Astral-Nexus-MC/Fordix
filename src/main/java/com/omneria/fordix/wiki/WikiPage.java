package com.omneria.fordix.wiki;

/** One imported wiki page: its text in the guide markup, and the address it was taken from. */
public final class WikiPage {
    public static final int MAX_BODY = 120_000;

    /** The page name with underscores, as wiki links write it ("Iron_Sword"). */
    public String slug = "";
    public String title = "";
    public String body = "";
    public String source = "";
    public long updatedAt;
    /** The pages this one links to (slugs); the ones that exist in the wiki are offered under the text. */
    public java.util.List<String> links = new java.util.ArrayList<>();

    public WikiPage() {
    }

    public WikiPage(String slug, String title, String body, String source, long updatedAt) {
        this.slug = slug;
        this.title = title;
        this.body = body;
        this.source = source;
        this.updatedAt = updatedAt;
    }
}
