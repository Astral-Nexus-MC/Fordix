package com.omneria.fordix.client.gui.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout math shared by FordixScreen's positioning helpers. Deliberately has
 * no Minecraft dependency so it can be unit tested with plain JUnit, without
 * booting the game.
 */
public final class Layout {

    private Layout() {
    }

    public record Size(int width, int height) {
    }

    public record Position(int x, int y) {
    }

    public record Result(List<Position> positions, int nextX, int nextY) {
    }

    public static Result stackVertical(int x, int y, int gap, List<Size> sizes) {
        List<Position> positions = new ArrayList<>(sizes.size());
        int currentY = y;
        for (Size size : sizes) {
            positions.add(new Position(x, currentY));
            currentY += size.height() + gap;
        }
        return new Result(positions, x, currentY);
    }

    public static Result stackHorizontal(int x, int y, int gap, List<Size> sizes) {
        List<Position> positions = new ArrayList<>(sizes.size());
        int currentX = x;
        for (Size size : sizes) {
            positions.add(new Position(currentX, y));
            currentX += size.width() + gap;
        }
        return new Result(positions, currentX, y);
    }

    public static int center(int containerSize, int itemSize) {
        return (containerSize - itemSize) / 2;
    }
}
