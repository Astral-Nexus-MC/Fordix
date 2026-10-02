package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.layout.HudAnchors;
import com.omneria.fordix.client.gui.layout.HudBounds;
import com.omneria.fordix.client.gui.layout.HudSnap;
import com.omneria.fordix.client.gui.vanilla.ExtraHud;
import com.omneria.fordix.client.gui.vanilla.HudSlots;
import com.omneria.fordix.client.gui.vanilla.JourneyMapHud;
import com.omneria.fordix.client.gui.widget.FordixButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves the HUD by dragging: every element (Voidframe's own, the vanilla chat, hotbar, scoreboard..., the overlays
 * other mods add, JourneyMap's minimap) is outlined where it sits over the live
 * game, you grab it and drop it anywhere. A grid and magnetism (screen center and edges, the other
 * elements' lines) help line things up; arrow keys nudge by 1 px (10 with Shift), a double-click or
 * Delete puts an element back. Placements are saved when the mouse is released: fordix-client.toml ([hud]) for
 * Voidframe's elements, fordix-hud-extras.json for the other overlays, JourneyMap's own settings for its minimap.
 */
public class HudLayoutScreen extends FordixScreen {
    private static final int SNAP_DISTANCE = 4;
    private static final int GRID = 20;
    private static final long DOUBLE_CLICK_MS = 300L;
    private static final int TOOLBAR_WIDTH = 120;
    private static final int HANDLE = 7;
    private static final float WHEEL_STEP = 0.1F;

    /** Kept for the session: the two helpers are a habit, not a setting worth a config file. */
    private static boolean showGrid = true;
    private static boolean magnet = true;

    private final Screen parent;
    private final List<String> elements = new ArrayList<>(FordixConfig.HUD_ELEMENTS);
    private boolean discovered;
    private int selected = -1;
    private boolean dragging;
    private boolean resizing;
    private float resizeBaseWidth;
    private double rawX;
    private double rawY;
    private Integer guideX;
    private Integer guideY;
    private long lastClickAt;
    private int lastClicked = -1;
    private FordixButton gridButton;
    private FordixButton magnetButton;
    private FordixButton presetButton;
    private FordixButton hideButton;
    private int presetIndex;

    public HudLayoutScreen(Screen parent) {
        super(Tr.c("hud.title"));
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
        presetButton = addRenderableWidget(new FordixButton(x, y + 36, TOOLBAR_WIDTH, 16, presetLabel(), () -> {
            applyPreset(HudAnchors.PRESETS.get(presetIndex));
            presetIndex = (presetIndex + 1) % HudAnchors.PRESETS.size();
            presetButton.setMessage(presetLabel());
        }));
        hideButton = addRenderableWidget(new FordixButton(x, y + 54, TOOLBAR_WIDTH, 16, Tr.c("hud.hide"), () -> {
            if (selected >= 0) {
                HudSlots.setHidden(elements.get(selected), !HudSlots.hidden(elements.get(selected)));
                save();
            }
        }));
        addRenderableWidget(new FordixButton(x, y + 72, TOOLBAR_WIDTH, 16, Tr.c("hud.reset"), () -> {
            applyPreset(HudAnchors.PRESETS.get(0));
            presetIndex = 1;
            presetButton.setMessage(presetLabel());
        }));
        addRenderableWidget(new FordixButton(x, y + 90, TOOLBAR_WIDTH, 16, Tr.c("common.close"), this::onClose));
    }

    private Component presetLabel() {
        return Tr.c("hud.preset", Tr.s("hud.preset." + HudAnchors.PRESETS.get(presetIndex).name().toLowerCase(java.util.Locale.ROOT)));
    }

    private void applyPreset(HudAnchors.Preset preset) {
        for (int i = 0; i < elements.size(); i++) {
            // The presets lay out Voidframe's own elements; the other overlays only come back to their spot.
            HudSlots.apply(elements.get(i), i < preset.entries().size() ? preset.entries().get(i) : HudAnchors.Entry.standard());
            if (i >= preset.entries().size() && HudSlots.isJourneyMap(elements.get(i))) {
                HudSlots.reset(elements.get(i));
            }
        }
        save();
    }

    private static Component gridLabel() {
        return Tr.c("hud.grid", showGrid ? "ON" : "OFF");
    }

    private static Component magnetLabel() {
        return Tr.c("hud.magnet", magnet ? "ON" : "OFF");
    }

    private HudSnap.Rect rectOf(int index) {
        return HudSlots.placed(elements.get(index), this.width, this.height);
    }

    private HudSnap.Rect defaultOf(int index) {
        return HudSlots.defaults(elements.get(index), this.width, this.height);
    }

    /** Stores the placement that puts the element's rectangle at (x, y), kept on screen. */
    private void moveTo(int index, int x, int y) {
        HudSlots.moveTo(elements.get(index), x, y, this.width, this.height);
    }

    /** Back to the default spot, visible. */
    private void reset(int index) {
        HudSlots.reset(elements.get(index));
    }

    /** Pins the element to the screen point nearest its final spot, so it keeps its place on any window size. */
    private void pinToNearestAnchor(int index) {
        HudSlots.pinToNearestAnchor(elements.get(index), this.width, this.height);
    }

    private void save() {
        HudSlots.save(HudBounds.ELEMENTS.get(0));
        ExtraHud.save();
        if (elements.contains(JourneyMapHud.ID)) {
            JourneyMapHud.save();
        }
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
        if (!discovered) {
            discovered = true;
            discover(guiGraphics);
        }
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
        hideButton.setMessage(Tr.c(selected >= 0 && HudSlots.hidden(elements.get(selected)) ? "hud.show" : "hud.hide"));
        hideButton.active = selected >= 0 && HudSlots.canHide(elements.get(selected));
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        drawHelp(guiGraphics);
    }

    /** First frame: find the overlays other than Voidframe's (off screen, so before anything of ours is drawn). */
    private void discover(GuiGraphics guiGraphics) {
        guiGraphics.flush();
        elements.addAll(ExtraHud.discover(this.width, this.height));
        if (JourneyMapHud.available()) {
            elements.add(JourneyMapHud.ID);
        }
        // Rare: a mod overlay measured as nothing leaves the vanilla ones; nothing to do for them.
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
        boolean hiddenElement = HudSlots.hidden(elements.get(index));
        int accent = FordixPalette.ACCENT & 0xFFFFFF;
        int fill = isSelected ? 0x55000000 | accent : isHovered ? 0x40000000 | accent : 0x22000000 | accent;
        int border = isSelected ? 0xFF000000 | accent : isHovered ? FordixPalette.TEXT_BRIGHT : 0xAA000000 | accent;
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, border);
        guiGraphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), border);
        guiGraphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), border);
        guiGraphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), border);
        String label = HudSlots.label(elements.get(index)) + (hiddenElement ? Tr.s("hud.hidden_suffix") : "");
        int textWidth = this.font.width(label);
        int textY = rect.h() >= 10 ? rect.y() + (rect.h() - 8) / 2 : rect.y() - 9;
        guiGraphics.drawString(this.font, label, rect.x() + (rect.w() - textWidth) / 2, textY, FordixPalette.TEXT_BRIGHT, true);
        if (isSelected) {
            int hx = handleX(rect);
            int hy = handleY(rect);
            guiGraphics.fill(hx, hy, hx + HANDLE, hy + HANDLE, 0xFF000000 | accent);
            guiGraphics.fill(hx + 2, hy + 2, hx + HANDLE - 1, hy + HANDLE - 1, 0xFF05070A);
        }
    }

    /** The resize handle sits on the selected element's bottom-right corner, half outside it so small ones stay grabbable. */
    private static int handleX(HudSnap.Rect rect) {
        return rect.right() - HANDLE / 2 - 1;
    }

    private static int handleY(HudSnap.Rect rect) {
        return rect.bottom() - HANDLE / 2 - 1;
    }

    private boolean onHandle(double mouseX, double mouseY) {
        if (selected < 0) {
            return false;
        }
        HudSnap.Rect rect = rectOf(selected);
        return mouseX >= handleX(rect) - 1 && mouseX < handleX(rect) + HANDLE + 1
                && mouseY >= handleY(rect) - 1 && mouseY < handleY(rect) + HANDLE + 1;
    }

    private void resize(int index, float scale) {
        HudSlots.setScale(elements.get(index), scale);
    }

    /** Offset of the selected element, next to it while it's being moved. */
    private void drawTag(GuiGraphics guiGraphics, int index) {
        int[] offset = HudSlots.offset(elements.get(index));
        HudSnap.Rect rect = rectOf(index);
        float scale = HudSlots.scale(elements.get(index));
        String text = HudSlots.anchor(elements.get(index)).id() + " " + offset[0] + ", " + offset[1]
                + (Math.abs(scale - 1.0F) > 0.005F ? "  x" + Math.round(scale * 100) / 100.0F : "");
        int width = this.font.width(text) + 6;
        int x = Math.max(0, Math.min(this.width - width, rect.x()));
        int y = rect.y() >= 22 ? rect.y() - 20 : rect.bottom() + 3;
        guiGraphics.fill(x, y, x + width, y + 11, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
        guiGraphics.drawString(this.font, text, x + 3, y + 2, 0xFF05070A, false);
    }

    private void drawHelp(GuiGraphics guiGraphics) {
        List<FormattedCharSequence> lines = this.font.split(Tr.c("hud.help"),
                TOOLBAR_WIDTH + 70);
        int y = 6 + 90 + 22;
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(this.font, line, 6, y, FordixPalette.TEXT_BRIGHT, true);
            y += 10;
        }
    }

    /** For the in-game test: moves a listed element by a few pixels as a drag would. */
    public boolean nudge(String id, int dx, int dy) {
        int index = elements.indexOf(id);
        if (index < 0) {
            return false;
        }
        selected = index;
        HudSnap.Rect rect = rectOf(index);
        moveTo(index, rect.x() + dx, rect.y() + dy);
        pinToNearestAnchor(index);
        save();
        return true;
    }

    public List<String> listed() {
        return List.copyOf(elements);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (onHandle(mouseX, mouseY)) {
            resizing = true;
            resizeBaseWidth = rectOf(selected).w() / HudSlots.scale(elements.get(selected));
            return true;
        }
        int hit = elementAt(mouseX, mouseY);
        selected = hit;
        if (hit < 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (hit == lastClicked && now - lastClickAt < DOUBLE_CLICK_MS) {
            reset(hit);
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
        if (resizing && selected >= 0 && button == 0) {
            HudSnap.Rect rect = rectOf(selected);
            resize(selected, (float) (mouseX - rect.x()) / Math.max(1.0F, resizeBaseWidth));
            return true;
        }
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
        if (resizing) {
            resizing = false;
            save();
        }
        if (dragging) {
            dragging = false;
            guideX = null;
            guideY = null;
            if (selected >= 0) {
                pinToNearestAnchor(selected);
            }
            save();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** The wheel resizes the element under the pointer (Shift: bigger steps). */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int hit = elementAt(mouseX, mouseY);
        if (hit >= 0 && delta != 0.0D) {
            selected = hit;
            resize(hit, HudSlots.scale(elements.get(hit)) + (float) Math.signum(delta) * WHEEL_STEP * (hasShiftDown() ? 2.5F : 1.0F));
            save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selected >= 0 && (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD
                || keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT)) {
            boolean bigger = keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD;
            resize(selected, HudSlots.scale(elements.get(selected)) + (bigger ? WHEEL_STEP : -WHEEL_STEP));
            save();
            return true;
        }
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
                    reset(selected);
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
