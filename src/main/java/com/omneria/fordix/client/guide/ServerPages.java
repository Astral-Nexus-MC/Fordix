package com.omneria.fordix.client.guide;

import com.omneria.fordix.guide.GuidePage;

import java.util.List;

/** The guide pages the connected server pushed; cleared on disconnect like {@link ServerGuides}. */
public final class ServerPages {
    private static volatile List<GuidePage> pages = List.of();

    private ServerPages() {
    }

    public static List<GuidePage> pages() {
        return pages;
    }

    public static void set(List<GuidePage> received) {
        pages = List.copyOf(received);
    }

    public static void clear() {
        pages = List.of();
    }

    /** True when some page was edited since the player last opened it. */
    public static boolean hasUnread() {
        for (GuidePage page : pages) {
            if (SeenPages.isNew(page)) {
                return true;
            }
        }
        return false;
    }
}
