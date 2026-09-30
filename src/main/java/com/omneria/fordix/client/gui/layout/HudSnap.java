package com.omneria.fordix.client.gui.layout;

import java.util.List;

/**
 * Magnetism for the HUD layout screen: while an element is dragged, its left/center/right
 * (and top/middle/bottom) lines are pulled onto the screen's edges and center and onto the
 * other elements' lines when they come within a few pixels. Pure Java, unit tested.
 */
public final class HudSnap {
    private HudSnap() {
    }

    public record Rect(int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }
    }

    /**
     * The shift to add to the dragged rectangle, and the coordinates of the lines it snapped to
     * (null when it didn't snap on that axis).
     */
    public record Result(int dx, int dy, Integer guideX, Integer guideY) {
    }

    public static Result snap(Rect moving, List<Rect> others, int screenWidth, int screenHeight, int threshold) {
        int[] xs = {moving.x(), moving.x() + moving.w() / 2, moving.right()};
        int[] ys = {moving.y(), moving.y() + moving.h() / 2, moving.bottom()};
        int bestX = Integer.MAX_VALUE;
        int shiftX = 0;
        Integer guideX = null;
        int bestY = Integer.MAX_VALUE;
        int shiftY = 0;
        Integer guideY = null;

        int[] targetsX = new int[3 + others.size() * 3];
        int[] targetsY = new int[3 + others.size() * 3];
        targetsX[0] = 0;
        targetsX[1] = screenWidth / 2;
        targetsX[2] = screenWidth;
        targetsY[0] = 0;
        targetsY[1] = screenHeight / 2;
        targetsY[2] = screenHeight;
        int index = 3;
        for (Rect other : others) {
            targetsX[index] = other.x();
            targetsX[index + 1] = other.x() + other.w() / 2;
            targetsX[index + 2] = other.right();
            targetsY[index] = other.y();
            targetsY[index + 1] = other.y() + other.h() / 2;
            targetsY[index + 2] = other.bottom();
            index += 3;
        }
        for (int value : xs) {
            for (int target : targetsX) {
                int distance = Math.abs(target - value);
                if (distance <= threshold && distance < bestX) {
                    bestX = distance;
                    shiftX = target - value;
                    guideX = target;
                }
            }
        }
        for (int value : ys) {
            for (int target : targetsY) {
                int distance = Math.abs(target - value);
                if (distance <= threshold && distance < bestY) {
                    bestY = distance;
                    shiftY = target - value;
                    guideY = target;
                }
            }
        }
        return new Result(shiftX, shiftY, guideX, guideY);
    }
}
