package com.omneria.fordix.client.gui;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.layout.HudBounds;
import com.omneria.fordix.client.gui.layout.HudSnap;
import com.omneria.fordix.client.gui.widget.FordixButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves the Voidframe HUD by dragging: every element is outlined where it sits over the live
 * game, you grab it and drop it anywhere. A grid and magnetism (screen center and edges, the other
 * elements' lines) help line things up; arrow keys nudge by 1 px (10 with Shift), a double-click or
 * Delete puts an element back. Offsets from each element's default spot are saved in
 * fordix-client.toml ([hud]) when the mouse is released.
 */
public class HudLayoutScreen extends FordixScreen {
    private static final int SNAP_DISTANCE = 4;
    private static final int GRID = 20;
    private static final long DOUBLE_CLICK_MS = 300L;
    private static final int TOOLBAR_WIDTH = 120;

    /** Kept for the session: the two helpers are a habit, not a setting worth a config file. */
    private static boolean showGrid = true;
    private static boolean magnet = true;

    private final Screen parent;
    private final List<String> elements = FordixConfig.HUD_ELEMENTS;
    private int selected = -1;
    private boolean dragging;
    private double rawX;
    private double rawY;
    private Integer guideX;
    private Integer guideY;
    private long lastClickAt;
    private int lastClicked = -1;
    private FordixButton gridButton;
    private FordixButton magnetButton;

    public HudLayoutScreen(Screen parent) {
        super(Component.literal("FORDIX // DISPOSITION DU HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = 6;
        int y = 6;
        gridButton = addRenderableWidget(new FordixButton(x, y, TOOLBAR_WIDTH, 16, gridLabel(), () -> {
            showGrid = !showGrid;
            gridButton.setMessage(gridLabel());
        }));
        magnetButton = addRenderableWidget(new FordixButton(x, y + 18, TOOLBAR_WIDTH, 16, magnetLabel(), () -> {
            magnet = !magnet;
            magnetButton.setMessage(magnetLabel());
        }));
        addRenderableWidget(new FordixButton(x, y + 36, TOOLBAR_WIDTH, 16, Component.literal("Tout remettre"), () -> {
            for (int i = 0; i < elements.size(); i++) {
                setOffset(i, 0, 0);
            }
            save();
        }));
        addRenderableWidget(new FordixButton(x, y + 54, TOOLBAR_WIDTH, 16, Component.literal("Fermer"), this::onClose));
    }

    private static Component gridLabel() {
        return Component.literal("Grille : " + (showGrid ? "ON" : "OFF"));
    }

    private static Component magnetLabel() {
        return Component.literal("Aimant : " + (magnet ? "ON" : "OFF"));
    }

    private HudSnap.Rect rectOf(int index) {
        String element = elements.get(index);
        int[] offset = FordixConfig.hudOffset(element);
        return HudBounds.place(HudBounds.defaults(element, this.width, this.height), offset[0], offset[1], this.width, this.height);
    }

    private HudSnap.Rect defaultOf(int index) {
        return HudBounds.defaults(elements.get(index), this.width, this.height);
    }

    /** Stores the offset that puts the element's rectangle at (x, y), kept on screen. */
    private void moveTo(int index, int x, int y) {
        HudSnap.Rect base = defaultOf(index);
        HudSnap.Rect placed = HudBounds.place(base, x - base.x(), y - base.y(), this.width, this.height);
        setOffset(index, placed.x() - base.x(), placed.y() - base.y());
    }

    private void setOffset(int index, int offsetX, int offsetY) {
        String element = elements.get(index);
        FordixConfig.hudOffsetX(element).set(offsetX);
        FordixConfig.hudOffsetY(element).set(offsetY);
    }

    private void save() {
        FordixConfig.hudOffsetX(elements.get(0)).save();
    }

    private List<HudSnap.Rect> othersThan(int index) {
        List<HudSnap.Rect> others = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            if (i != index) {
                others.add(rectOf(i));
            }
        }
        return others;
    }

    /** The topmost (smallest) element under the point, or -1. */
    private int elementAt(double mouseX, double mouseY) {
        int found = -1;
        int smallest = Integer.MAX_VALUE;
        for (int i = 0; i < elements.size(); i++) {
            HudSnap.Rect rect = rectOf(i);
            if (mouseX >= rect.x() - 1 && mouseX < rect.right() + 1 && mouseY >= rect.y() - 1 && mouseY < rect.bottom() + 1
                    && rect.w() * rect.h() < smallest) {
                smallest = rect.w() * rect.h();
                found = i;
            }
        }
        return found;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // The game stays visible and undimmed behind: only a faint veil so the outlines read.
        guiGraphics.fill(0, 0, this.width, this.height, 0x33000000);
        if (showGrid) {
            drawGrid(guiGraphics);
        }
        int hovered = dragging ? selected : elementAt(mouseX, mouseY);
        for (int i = 0; i < elements.size(); i++) {
            drawElement(guiGraphics, i, i == selected, i == hovered);
        }
        if (dragging) {
            if (guideX != null) {
                guiGraphics.fill(guideX, 0, guideX + 1, this.height, 0xCC000000 | (FordixPalette.ACCENT & 0xFFFFFF));
            }
            if (guideY != null) {
                guiGraphics.fill(0, guideY, this.width, guideY + 1, 0xCC000000 | (FordixPalette.ACCENT & 0xFFFFFF));
            }
        }
        if (selected >= 0) {
            drawTag(guiGraphics, selected);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        drawHelp(guiGraphics);
    }

    private void drawGrid(GuiGraphics guiGraphics) {
        int color = 0x22FFFFFF;
        for (int x = this.width / 2 % GRID; x < this.width; x += GRID) {
            guiGraphics.fill(x, 0, x + 1, this.height, color);
        }
        for (int y = this.height % GRID; y < this.height; y += GRID) {
            guiGraphics.fill(0, y, this.width, y + 1, color);
        }
    }

    private void drawElement(GuiGraphics guiGraphics, int index, boolean isSelected, boolean isHovered) {
        HudSnap.Rect rect = rectOf(index);
        int accent = FordixPalette.ACCENT & 0xFFFFFF;
        int fill = isSelected ? 0x55000000 | accent : isHovered ? 0x40000000 | accent : 0x22000000 | accent;
        int border = isSelected ? 0xFF000000 | accent : isHovered ? FordixPalette.TEXT_BRIGHT : 0xAA000000 | accent;
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, border);
        guiGraphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), border);
        guiGraphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), border);
        guiGraphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), border);
        String label = HudBounds.LABELS.get(index);
        int textWidth = this.font.width(label);
        int textY = rect.h() >= 10 ? rect.y() + (rect.h() - 8) / 2 : rect.y() - 9;
        guiGraphics.drawString(this.font, label, rect.x() + (rect.w() - textWidth) / 2, textY, FordixPalette.TEXT_BRIGHT, true);
    }

    /** Offset of the selected element, next to it while it's being moved. */
    private void drawTag(GuiGraphics guiGraphics, int index) {
        int[] offset = FordixConfig.hudOffset(elements.get(index));
        HudSnap.Rect rect = rectOf(index);
        String text = offset[0] + ", " + offset[1];
        int width = this.font.width(text) + 6;
        int x = Math.max(0, Math.min(this.width - width, rect.x()));
        int y = rect.y() >= 22 ? rect.y() - 20 : rect.bottom() + 3;
        guiGraphics.fill(x, y, x + width, y + 11, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
        guiGraphics.drawString(this.font, text, x + 3, y + 2, 0xFF05070A, false);
    }

    private void drawHelp(GuiGraphics guiGraphics) {
        List<FormattedCharSequence> lines = this.font.split(Component.literal(
                "Glisse un element pour le deplacer. Fleches : 1 px (Maj : 10). Double-clic ou Suppr : position par defaut."),
                TOOLBAR_WIDTH + 70);
        int y = 6 + 54 + 22;
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(this.font, line, 6, y, FordixPalette.TEXT_BRIGHT, true);
            y += 10;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        int hit = elementAt(mouseX, mouseY);
        selected = hit;
        if (hit < 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (hit == lastClicked && now - lastClickAt < DOUBLE_CLICK_MS) {
            setOffset(hit, 0, 0);
            save();
            lastClicked = -1;
            return true;
        }
        lastClicked = hit;
        lastClickAt = now;
        HudSnap.Rect rect = rectOf(hit);
        rawX = rect.x();
        rawY = rect.y();
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && selected >= 0 && button == 0) {
            rawX += dragX;
            rawY += dragY;
            HudSnap.Rect base = defaultOf(selected);
            int x = (int) Math.round(rawX);
            int y = (int) Math.round(rawY);
            guideX = null;
            guideY = null;
            if (magnet) {
                HudSnap.Result snap = HudSnap.snap(new HudSnap.Rect(x, y, base.w(), base.h()), othersThan(selected),
                        this.width, this.height, SNAP_DISTANCE);
                x += snap.dx();
                y += snap.dy();
                guideX = snap.guideX();
                guideY = snap.guideY();
            }
            moveTo(selected, x, y);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            guideX = null;
            guideY = null;
            save();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selected >= 0) {
            int step = hasShiftDown() ? 10 : 1;
            int dx = 0;
            int dy = 0;
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> dx = -step;
                case GLFW.GLFW_KEY_RIGHT -> dx = step;
                case GLFW.GLFW_KEY_UP -> dy = -step;
                case GLFW.GLFW_KEY_DOWN -> dy = step;
                case GLFW.GLFW_KEY_DELETE, GLFW.GLFW_KEY_BACKSPACE -> {
                    setOffset(selected, 0, 0);
                    save();
                    return true;
                }
                case GLFW.GLFW_KEY_TAB -> {
                    selected = (selected + (hasShiftDown() ? elements.size() - 1 : 1)) % elements.size();
                    return true;
                }
                default -> {
                }
            }
            if (dx != 0 || dy != 0) {
                HudSnap.Rect rect = rectOf(selected);
                moveTo(selected, rect.x() + dx, rect.y() + dy);
                save();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
