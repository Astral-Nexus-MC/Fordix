package com.omneria.fordix.client.tooltip;

import java.util.List;

/** The names of the item tooltip layouts offered in the config. Plain Java. */
public final class TooltipStyles {
    public static final String FULL = "full";
    public static final String COMPACT = "compact";
    public static final String CLASSIC = "classic";
    public static final List<String> NAMES = List.of(FULL, COMPACT, CLASSIC);

    private TooltipStyles() {
    }
}
