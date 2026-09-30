package com.omneria.fordix.client.tooltip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The names of the item tooltip layouts offered in the config. Plain Java. */
public final class TooltipStyles {
    public static final String FULL = "full";
    public static final String COMPACT = "compact";
    public static final String CLASSIC = "classic";
    /** A null-tolerant list: Forge's config validation asks contains(null) for a key missing from the file, which List.of rejects. */
    public static final List<String> NAMES = Collections.unmodifiableList(new ArrayList<>(List.of(FULL, COMPACT, CLASSIC)));

    private TooltipStyles() {
    }
}
