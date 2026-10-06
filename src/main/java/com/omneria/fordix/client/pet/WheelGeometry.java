package com.omneria.fordix.client.pet;

/** Which sector of a wheel a point is on. Pure Java, unit tested; sector 0 is centered on the top, then clockwise. */
public final class WheelGeometry {
    private WheelGeometry() {
    }

    /** The sector under the offset (dx, dy) from the wheel's center (screen axes, y down), or -1 inside the hub. */
    public static int sectorAt(double dx, double dy, int count, double hubRadius) {
        if (count <= 0 || dx * dx + dy * dy < hubRadius * hubRadius) {
            return -1;
        }
        double angle = Math.atan2(dx, -dy);
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        double width = 2 * Math.PI / count;
        return (int) Math.floor((angle + width / 2) / width) % count;
    }

    /** The angle (radians clockwise from the top) at the middle of a sector. */
    public static double middleOf(int sector, int count) {
        return sector * 2 * Math.PI / count;
    }
}
