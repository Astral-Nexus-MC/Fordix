package com.omneria.fordix.client;

import com.omneria.fordix.FordixConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;

/**
 * A full screen without the video mode change of Minecraft's own: the window stays a normal window, loses its border and
 * title bar and covers the monitor it is on, so Alt-Tab is instant and nothing flickers. F11 (or whatever key the game's
 * "Toggle Fullscreen" is set to) does it when the {@code borderlessFullscreen} option is on, by a key callback set in front
 * of the game's own, which cannot be intercepted any other way (the game handles that key and returns before any event).
 * Leaving gives back the position, size and border the window had.
 */
public final class BorderlessFullscreen {
    private static boolean installed;
    private static boolean active;
    private static boolean enterNextTick;
    private static final int[] SAVED = new int[4];

    private BorderlessFullscreen() {
    }

    public static boolean active() {
        return active;
    }

    /** Puts the key callback in front of the game's. Done once, on the render thread, after the window exists. */
    public static void install(Minecraft minecraft) {
        if (installed) {
            return;
        }
        installed = true;
        long handle = minecraft.getWindow().getWindow();
        GLFWKeyCallback[] previous = new GLFWKeyCallback[1];
        previous[0] = GLFW.glfwSetKeyCallback(handle, (window, key, scancode, action, mods) -> {
            if (action == GLFW.GLFW_PRESS && FordixConfig.BORDERLESS_FULLSCREEN.get()
                    && !(minecraft.screen instanceof KeyBindsScreen) && minecraft.options.keyFullscreen.matches(key, scancode)) {
                toggle(minecraft);
                return;
            }
            if (previous[0] != null) {
                previous[0].invoke(window, key, scancode, action, mods);
            }
        });
    }

    /** Enters or leaves the borderless full screen. */
    public static void toggle(Minecraft minecraft) {
        Window window = minecraft.getWindow();
        if (active) {
            leave(window.getWindow());
            return;
        }
        if (window.isFullscreen()) {
            // The game's own full screen is on: leave it first, the window is placed on the next tick (the game applies the
            // change at the end of this frame and would put the window back otherwise).
            window.toggleFullScreen();
            minecraft.options.fullscreen().set(window.isFullscreen());
            enterNextTick = true;
            return;
        }
        enter(window.getWindow());
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && enterNextTick) {
            enterNextTick = false;
            Minecraft minecraft = Minecraft.getInstance();
            if (!minecraft.getWindow().isFullscreen() && !active) {
                enter(minecraft.getWindow().getWindow());
            }
        }
    }

    private static void enter(long handle) {
        int[] x = new int[1];
        int[] y = new int[1];
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetWindowPos(handle, x, y);
        GLFW.glfwGetWindowSize(handle, w, h);
        SAVED[0] = x[0];
        SAVED[1] = y[0];
        SAVED[2] = w[0];
        SAVED[3] = h[0];
        int[] area = monitorUnder(x[0] + w[0] / 2, y[0] + h[0] / 2);
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowPos(handle, area[0], area[1]);
        GLFW.glfwSetWindowSize(handle, area[2], area[3]);
        active = true;
    }

    private static void leave(long handle) {
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
        GLFW.glfwSetWindowSize(handle, Math.max(320, SAVED[2]), Math.max(240, SAVED[3]));
        GLFW.glfwSetWindowPos(handle, SAVED[0], SAVED[1]);
        active = false;
    }

    /** {x, y, width, height} of the monitor that holds this point, or of the primary one. */
    private static int[] monitorUnder(int px, int py) {
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        int[] best = null;
        if (monitors != null) {
            for (int i = 0; i < monitors.limit(); i++) {
                long monitor = monitors.get(i);
                GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
                if (mode == null) {
                    continue;
                }
                int[] mx = new int[1];
                int[] my = new int[1];
                GLFW.glfwGetMonitorPos(monitor, mx, my);
                int[] area = {mx[0], my[0], mode.width(), mode.height()};
                if (best == null) {
                    best = area;
                }
                if (contains(area, px, py)) {
                    return area;
                }
            }
        }
        if (best != null) {
            return best;
        }
        GLFWVidMode primary = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        return new int[]{0, 0, primary == null ? 1280 : primary.width(), primary == null ? 720 : primary.height()};
    }

    /** Whether the point lies in the {x, y, width, height} rectangle. Public for tests. */
    public static boolean contains(int[] area, int px, int py) {
        return px >= area[0] && px < area[0] + area[2] && py >= area[1] && py < area[1] + area[3];
    }
}
