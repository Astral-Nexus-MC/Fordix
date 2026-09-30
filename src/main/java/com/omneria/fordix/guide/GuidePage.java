package com.omneria.fordix.guide;

/**
 * A page of the server's guide, written by admins in the administration screen and shown to
 * every player in the Guides screen. Plain fields so Gson can store it (see AdminStore).
 */
public final class GuidePage {
    public static final int MAX_BODY = 3500;

    public String id = "";
    public String title = "";
    /** Written in {@link PageMarkup}. */
    public String body = "";
    public String author = "";
    /** Epoch ms of the last edit: a player who read an older version sees the page as new. */
    public long updatedAt;

    public GuidePage() {
    }

    public GuidePage(String id, String title, String body, String author, long updatedAt) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.author = author;
        this.updatedAt = updatedAt;
    }
}
