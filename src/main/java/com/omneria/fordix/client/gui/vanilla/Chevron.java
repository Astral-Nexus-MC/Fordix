package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The Voidframe status bar shape shared by the HUD gauges, boss bars, item durability and
 * tooltips: a dark outline, an empty track and a vertical color gradient fill, with one end
 * cut on a slant ({@link BarShape}). Only flat fills, so it draws anywhere GuiGraphics does.
 */
public final class Chevron {
    private Chevron() {
    }

    /**
     * @param cutRight  true: the right end is slanted (top row longest); false: the left end is
     * @param fraction  how much of the bar is filled, 0..1
     * @param fromRight fill grows from the right end instead of the left
     */
    public static void draw(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean cutRight, int slant,
                            float fraction, int topColor, int bottomColor, boolean fromRight) {
        int inner = width - 2;
        int filled = Math.round(inner * Math.max(0.0F, Math.min(1.0F, fraction)));
        for (int row = 0; row < height; row++) {
            int[] span = BarShape.rowSpan(row, height, width, slant, cutRight);
            boolean edge = row == 0 || row == height - 1;
            guiGraphics.fill(x + span[0], y + row, x + span[1], y + row + 1, FordixPalette.BEVEL_SHADOW);
            if (edge) {
                continue;
            }
            guiGraphics.fill(x + span[0] + 1, y + row, x + span[1] - 1, y + row + 1, FordixPalette.SLOT_FILL);
            int from = fromRight ? inner - filled : 0;
            int to = fromRight ? inner : filled;
            int left = Math.max(x + 1 + from, x + span[0] + 1);
            int right = Math.min(x + 1 + to, x + span[1] - 1);
            if (right > left) {
                float t = height <= 3 ? 0.0F : (row - 1) / (float) (height - 3);
                guiGraphics.fill(left, y + row, right, y + row + 1, mix(topColor, bottomColor, t));
                if (row == 1 && height >= 5) {
                    // Specular line on the first row, like the glossy bars of the Voidframe 2 mockup.
                    guiGraphics.fill(left, y + row, right, y + row + 1, 0x38FFFFFF);
                }
            }
        }
    }

    public static int mix(int from, int to, float t) {
        int a = 0xFF;
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
