package com.omneria.fordix.client.gui.layout;

/** One HUD element's placement inside a layout (what a share code carries). Pure Java. */
public final class HudLayout {
    private HudLayout() {
    }

    public static final class Placement {
        public String anchor = "default";
        public int offsetX;
        public int offsetY;
        public boolean hidden;
        public float scale = 1.0F;
        /** 0 in an older code: same as the width factor. */
        public float scaleY;

        public Placement() {
        }

        public Placement(String anchor, int offsetX, int offsetY, boolean hidden, float scale) {
            this.anchor = anchor;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.hidden = hidden;
            this.scale = scale;
        }
    }
}
