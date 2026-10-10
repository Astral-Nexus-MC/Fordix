package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.layout.HudAnchors;
import com.omneria.fordix.client.gui.layout.HudBounds;
import com.omneria.fordix.client.gui.layout.HudChecks;
import com.omneria.fordix.client.gui.layout.HudLayout;
import com.omneria.fordix.client.gui.layout.HudSnap;
import com.omneria.fordix.client.gui.vanilla.ExtraHud;
import com.omneria.fordix.client.gui.vanilla.HudProfileActions;
import com.omneria.fordix.client.gui.vanilla.HudSlots;
import com.omneria.fordix.client.gui.vanilla.JourneyMapHud;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.hud.HudRules;
import com.omneria.fordix.client.hud.HudStyle;
import com.omneria.fordix.client.hud.HudToast;
import net.minecraft.client.Minecraft;
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
    private static final int PANEL_WIDTH = 132;
    private static final int PANEL_MARGIN = 6;
    private static final int ROW_HEIGHT = 11;
    private static final int HISTORY = 40;
    private static final int GUIDE_COLOR = 0xFFFF4FA3;
    private static final int HANDLE = 7;
    private static final float WHEEL_STEP = 0.05F;
    private static final float FINE_STEP = 0.01F;

    /** Kept for the session: the two helpers are a habit, not a setting worth a config file. */
    private static boolean showGrid = true;
    private static boolean magnet = true;

    private final Screen parent;
    private final List<String> elements = new ArrayList<>(FordixConfig.HUD_ELEMENTS);
    private boolean discovered;
    private int selected = -1;
    private boolean dragging;
    private boolean resizing;
    /** Which handle is being dragged: the corner (both sizes), the right edge (width) or the bottom edge (height). */
    private int resizeMode;
    private float resizeBaseWidth;
    private float resizeBaseHeight;
    private float resizeRatio = 1.0F;
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
    private FordixButton undoButton;
    private FordixButton redoButton;
    private final List<FordixButton> propertyButtons = new ArrayList<>();
    private final List<FordixButton> leftWidgets = new ArrayList<>();
    private static final int HEADER = 16;
    /** Kept for the session: the panels fold to their title so the whole screen stays free to drag the elements. */
    private static boolean leftOpen = true;
    private static boolean rightOpen = true;
    private int presetIndex;
    private int listScroll;
    private int listTop;
    private int listBottom;
    private int propsTop;
    private int anchorTop;
    private final java.util.ArrayDeque<java.util.Map<String, HudLayout.Placement>> undo = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<java.util.Map<String, HudLayout.Placement>> redo = new java.util.ArrayDeque<>();

    public HudLayoutScreen(Screen parent) {
        super(Tr.c("hud.title"));
        this.parent = parent;
    }

    /** The first time the editor opens, a notice says how it works; a marker file remembers that it was shown. */
    private static void firstOpenHint() {
        java.nio.file.Path marker = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("fordix-hud-editor-seen.txt");
        if (java.nio.file.Files.exists(marker)) {
            return;
        }
        HudToast.show(Tr.s("hud.editor.first.title"), Tr.s("hud.editor.first.detail"), 0x3FD1C7);
        try {
            java.nio.file.Files.writeString(marker, "1");
        } catch (java.io.IOException ignored) {
            // Shown again next time: harmless.
        }
    }

    @Override
    protected void init() {
        firstOpenHint();
        propertyButtons.clear();
        leftWidgets.clear();
        int x = PANEL_MARGIN + 6;
        int half = (PANEL_WIDTH - 16) / 2;
        int y = PANEL_MARGIN + 16;
        gridButton = left(new FordixButton(x, y, half, 13, gridLabel(), () -> {
            showGrid = !showGrid;
            gridButton.setMessage(gridLabel());
        }));
        magnetButton = left(new FordixButton(x + half + 4, y, half, 13, magnetLabel(), () -> {
            magnet = !magnet;
            magnetButton.setMessage(magnetLabel());
        }));
        y += 15;
        presetButton = left(new FordixButton(x, y, PANEL_WIDTH - 12, 13, presetLabel(), () -> {
            record();
            applyPreset(HudAnchors.PRESETS.get(presetIndex));
            presetIndex = (presetIndex + 1) % HudAnchors.PRESETS.size();
            presetButton.setMessage(presetLabel());
        }));
        y += 15;
        undoButton = left(new FordixButton(x, y, half, 13, Tr.c("hud.undo"), this::undo));
        redoButton = left(new FordixButton(x + half + 4, y, half, 13, Tr.c("hud.redo"), this::redo));
        y += 15;
        left(new FordixButton(x, y, half, 13, Tr.c("hud.check"), this::verify));
        left(new FordixButton(x + half + 4, y, half, 13, Tr.c("hud.reset"), () -> {
            record();
            applyPreset(HudAnchors.PRESETS.get(0));
            presetIndex = 1;
            presetButton.setMessage(presetLabel());
        }));
        y += 15;
        // Share: the layout on screen as one line of text, and the reverse (how a server's layouts are prepared).
        left(new FordixButton(x, y, half, 13, Tr.c("hud.profile.copy"), () -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(HudProfileActions.exportCode());
            HudToast.show(Tr.s("hud.toast.share"), Tr.s("hud.profile.copied"), 0x5ED68A);
        }));
        left(new FordixButton(x + half + 4, y, half, 13, Tr.c("hud.profile.paste"), () -> {
            record();
            boolean ok = HudProfileActions.importCode(Minecraft.getInstance().keyboardHandler.getClipboard());
            HudToast.show(Tr.s("hud.toast.share"), Tr.s(ok ? "hud.profile.pasted" : "hud.profile.bad_code"), ok ? 0x5ED68A : 0xE5534B);
        }));
        y += 17;
        listTop = y + 12;
        listBottom = this.height - PANEL_MARGIN - 18;
        left(new FordixButton(PANEL_MARGIN + 6, this.height - PANEL_MARGIN - 17, PANEL_WIDTH - 12, 13,
                Tr.c("common.close"), this::onClose));

        // Right panel: the selected element's properties.
        int rx = this.width - PANEL_MARGIN - PANEL_WIDTH + 6;
        int ry = PANEL_MARGIN + 16;
        anchorTop = ry + 12;
        propsTop = anchorTop + 3 * 13 + 6;
        int rowY = propsTop;
        for (int row = 0; row < 4; row++) {
            final int r = row;
            propertyButtons.add(addRenderableWidget(new FordixButton(rx + PANEL_WIDTH - 12 - 30, rowY, 14, 12, Component.literal("-"), () -> adjust(r, -1))));
            propertyButtons.add(addRenderableWidget(new FordixButton(rx + PANEL_WIDTH - 12 - 14, rowY, 14, 12, Component.literal("+"), () -> adjust(r, 1))));
            rowY += 15;
        }
        hideButton = addRenderableWidget(new FordixButton(rx, rowY + 2, PANEL_WIDTH - 12, 13, Tr.c("hud.hide"), () -> {
            if (selected >= 0 && !HudRules.locked(elements.get(selected))) {
                record();
                HudSlots.setHidden(elements.get(selected), !HudSlots.hidden(elements.get(selected)));
                save();
            }
        }));
        propertyButtons.add(hideButton);
        propertyButtons.add(addRenderableWidget(new FordixButton(rx, rowY + 17, PANEL_WIDTH - 12, 13, Tr.c("hud.reset_one"), () -> {
            if (selected >= 0) {
                record();
                reset(selected);
                save();
            }
        })));
    }

    private FordixButton left(FordixButton button) {
        leftWidgets.add(button);
        return addRenderableWidget(button);
    }

    private int leftHeight() {
        return leftOpen ? this.height - 2 * PANEL_MARGIN : HEADER;
    }

    private int rightHeight() {
        return rightOpen ? propsTop + 4 * 15 + 36 - PANEL_MARGIN : HEADER;
    }

    /** Rows of the properties panel: 0 X, 1 Y, 2 width, 3 height; one step, ten pixels or 5 % with Shift/normal. */
    private void adjust(int row, int direction) {
        if (selected < 0) {
            return;
        }
        String id = elements.get(selected);
        record();
        int px = (hasShiftDown() ? 10 : 1) * direction;
        float factor = (hasShiftDown() ? 0.01F : WHEEL_STEP) * direction;
        HudSnap.Rect rect = rectOf(selected);
        switch (row) {
            case 0 -> moveTo(selected, rect.x() + px, rect.y());
            case 1 -> moveTo(selected, rect.x(), rect.y() + px);
            case 2 -> resize(selected, HudSlots.scale(id) + factor, HudSlots.scaleY(id));
            default -> {
                if (HudSlots.isJourneyMap(id)) {
                    resize(selected, HudSlots.scale(id) + factor);
                } else {
                    resize(selected, HudSlots.scale(id), HudSlots.scaleY(id) + factor);
                }
            }
        }
        save();
    }

    // ---- history: every change is filed first, Ctrl + Z and Ctrl + Y walk through it

    private void record() {
        undo.push(HudProfileActions.capture());
        while (undo.size() > HISTORY) {
            undo.removeLast();
        }
        redo.clear();
    }

    private void undo() {
        if (!undo.isEmpty()) {
            redo.push(HudProfileActions.capture());
            HudProfileActions.applyLayout(undo.pop());
        }
    }

    private void redo() {
        if (!redo.isEmpty()) {
            undo.push(HudProfileActions.capture());
            HudProfileActions.applyLayout(redo.pop());
        }
    }

    /** The verifier: visible elements that sit on top of each other. */
    private void verify() {
        List<Integer> visible = new ArrayList<>();
        List<HudSnap.Rect> rects = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            if (!HudSlots.hidden(elements.get(i))) {
                visible.add(i);
                rects.add(rectOf(i));
            }
        }
        List<int[]> pairs = HudChecks.overlaps(rects);
        if (pairs.isEmpty()) {
            HudToast.show(Tr.s("hud.check.title"), Tr.s("hud.check.ok"), 0x5ED68A);
        } else {
            int[] first = pairs.get(0);
            String detail = Tr.s("hud.check.overlap", HudSlots.label(elements.get(visible.get(first[0]))),
                    HudSlots.label(elements.get(visible.get(first[1]))));
            HudToast.show(Tr.s("hud.check.title") + " (" + pairs.size() + ")", detail, 0xE8A64A);
            selected = visible.get(first[0]);
        }
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
        int hovered = dragging ? selected : overPanel(mouseX, mouseY) ? -1 : elementAt(mouseX, mouseY);
        for (int i = 0; i < elements.size(); i++) {
            if (i != selected) {
                drawElement(guiGraphics, i, false, i == hovered);
            }
        }
        if (selected >= 0) {
            drawElement(guiGraphics, selected, true, false);
        }
        if (dragging) {
            if (guideX != null) {
                guiGraphics.fill(guideX, 0, guideX + 1, this.height, GUIDE_COLOR);
            }
            if (guideY != null) {
                guiGraphics.fill(0, guideY, this.width, guideY + 1, GUIDE_COLOR);
            }
        }
        if (selected >= 0) {
            drawTag(guiGraphics, selected);
        }
        // The panels and their buttons go in front of every outline and label drawn above, whatever the depth buffer says.
        guiGraphics.flush();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
        boolean busy = dragging || resizing;
        if (!busy) {
            drawPanels(guiGraphics, mouseX, mouseY);
        }
        for (FordixButton button : leftWidgets) {
            button.visible = leftOpen && !busy;
        }
        for (FordixButton button : propertyButtons) {
            button.visible = rightOpen && !busy;
        }
        boolean locked = selected >= 0 && HudRules.locked(elements.get(selected));
        hideButton.setMessage(Tr.c(selected >= 0 && HudSlots.hidden(elements.get(selected)) ? "hud.show" : "hud.hide"));
        for (FordixButton button : propertyButtons) {
            button.active = selected >= 0 && !(button == hideButton && locked);
        }
        undoButton.active = !undo.isEmpty();
        redoButton.active = !redo.isEmpty();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();
    }

    /** Whether the point is on a panel (only what a panel really covers, and nothing while an element is being dragged). */
    private boolean overPanel(double mouseX, double mouseY) {
        if (dragging || resizing) {
            return false;
        }
        int rx = this.width - PANEL_MARGIN - PANEL_WIDTH;
        return mouseX >= PANEL_MARGIN && mouseX < PANEL_MARGIN + PANEL_WIDTH && mouseY >= PANEL_MARGIN && mouseY < PANEL_MARGIN + leftHeight()
                || mouseX >= rx && mouseX < rx + PANEL_WIDTH && mouseY >= PANEL_MARGIN && mouseY < PANEL_MARGIN + rightHeight();
    }

    /** The two glass panels: the element list (with the tools above it) on the left, the properties on the right. */
    private void drawPanels(GuiGraphics g, int mouseX, int mouseY) {
        int accent = FordixPalette.ACCENT & 0xFFFFFF;
        int leftH = leftHeight();
        g.fill(PANEL_MARGIN, PANEL_MARGIN, PANEL_MARGIN + PANEL_WIDTH, PANEL_MARGIN + leftH, 0xF00B1118);
        HudStyle.panel(g, PANEL_MARGIN, PANEL_MARGIN, PANEL_WIDTH, leftH, accent, 0xE6);
        g.drawString(this.font, Tr.s("hud.editor.elements"), PANEL_MARGIN + 8, PANEL_MARGIN + 5, FordixPalette.ACCENT, false);
        g.drawString(this.font, leftOpen ? "[-]" : "[+]", PANEL_MARGIN + PANEL_WIDTH - 40, PANEL_MARGIN + 5, 0xFF8AA0AD, false);
        g.drawString(this.font, "x", PANEL_MARGIN + PANEL_WIDTH - 12, PANEL_MARGIN + 5, 0xFFE5534B, false);
        if (leftOpen) {
        g.drawString(this.font, Tr.s("hud.editor.list"), PANEL_MARGIN + 8, listTop - 11, 0xFF8AA0AD, false);
        g.enableScissor(PANEL_MARGIN, listTop, PANEL_MARGIN + PANEL_WIDTH, listBottom);
        int y = listTop - listScroll;
        for (int i = 0; i < elements.size(); i++) {
            String id = elements.get(i);
            if (y + ROW_HEIGHT > listTop && y < listBottom) {
                boolean on = i == selected;
                boolean hover = mouseX >= PANEL_MARGIN + 4 && mouseX < PANEL_MARGIN + PANEL_WIDTH - 4 && mouseY >= y && mouseY < y + ROW_HEIGHT;
                if (on) {
                    g.fillGradient(PANEL_MARGIN + 4, y, PANEL_MARGIN + PANEL_WIDTH - 4, y + ROW_HEIGHT, 0x553FD1C7, 0x113FD1C7);
                    g.fill(PANEL_MARGIN + 4, y, PANEL_MARGIN + 6, y + ROW_HEIGHT, 0xFF000000 | accent);
                } else if (hover) {
                    g.fill(PANEL_MARGIN + 4, y, PANEL_MARGIN + PANEL_WIDTH - 4, y + ROW_HEIGHT, 0x22FFFFFF);
                }
                boolean hiddenElement = HudSlots.hidden(id);
                boolean locked = HudRules.locked(id);
                g.drawString(this.font, hiddenElement ? "\u25CB" : "\u25CF", PANEL_MARGIN + 9, y + 2,
                        locked ? 0xFFE8A64A : hiddenElement ? 0xFF55606A : 0xFF5ED68A, false);
                String label = this.font.plainSubstrByWidth(HudSlots.label(id), PANEL_WIDTH - 40);
                g.drawString(this.font, label, PANEL_MARGIN + 21, y + 2, hiddenElement ? 0xFF7D93A3 : FordixPalette.TEXT_BRIGHT, false);
                if (locked) {
                    g.drawString(this.font, "\u2022", PANEL_MARGIN + PANEL_WIDTH - 11, y + 2, 0xFFE8A64A, false);
                }
            }
            y += ROW_HEIGHT;
        }
        g.disableScissor();
        }

        int rx = this.width - PANEL_MARGIN - PANEL_WIDTH;
        int rightHeight = rightHeight();
        g.fill(rx, PANEL_MARGIN, rx + PANEL_WIDTH, PANEL_MARGIN + rightHeight, 0xF00B1118);
        HudStyle.panel(g, rx, PANEL_MARGIN, PANEL_WIDTH, rightHeight, accent, 0xE6);
        g.drawString(this.font, Tr.s("hud.editor.properties"), rx + 8, PANEL_MARGIN + 5, FordixPalette.ACCENT, false);
        g.drawString(this.font, rightOpen ? "[-]" : "[+]", rx + PANEL_WIDTH - 24, PANEL_MARGIN + 5, 0xFF8AA0AD, false);
        if (!rightOpen) {
            return;
        }
        if (selected < 0) {
            g.drawString(this.font, Tr.s("hud.editor.none"), rx + 8, anchorTop + 2, FordixPalette.TEXT_BRIGHT, false);
            int hy = anchorTop + 16;
            for (FormattedCharSequence line : this.font.split(Tr.c("hud.help.short"), PANEL_WIDTH - 16)) {
                g.drawString(this.font, line, rx + 8, hy, 0xFF8AA0AD, false);
                hy += 10;
            }
            return;
        }
        String id = elements.get(selected);
        g.drawString(this.font, this.font.plainSubstrByWidth(HudSlots.label(id), PANEL_WIDTH - 16), rx + 8, PANEL_MARGIN + 17,
                FordixPalette.TEXT_BRIGHT, false);
        // The nine anchors; the current one is lit.
        HudAnchors.Anchor current = HudSlots.anchor(id);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cx = rx + 8 + col * 13;
                int cy = anchorTop + row * 13;
                boolean lit = ANCHORS[row][col] == current;
                g.fill(cx, cy, cx + 11, cy + 11, lit ? 0xFF000000 | accent : 0x66000000);
                g.fill(cx, cy, cx + 11, cy + 1, 0x33FFFFFF);
                if (lit) {
                    g.fill(cx - 1, cy - 1, cx + 12, cy + 12, 0x333FD1C7);
                }
            }
        }
        g.drawString(this.font, current.id(), rx + 8 + 3 * 13 + 6, anchorTop + 14, 0xFF8AA0AD, false);
        int[] offset = HudSlots.offset(id);
        HudSnap.Rect rect = rectOf(selected);
        String[] labels = {Tr.s("hud.editor.x"), Tr.s("hud.editor.y"), Tr.s("hud.editor.width"), Tr.s("hud.editor.height")};
        String[] values = {Integer.toString(rect.x()), Integer.toString(rect.y()),
                Math.round(HudSlots.scale(id) * 100) + " %", Math.round(HudSlots.scaleY(id) * 100) + " %"};
        for (int row = 0; row < 4; row++) {
            int ty = propsTop + row * 15 + 2;
            g.drawString(this.font, labels[row], rx + 8, ty, 0xFF8AA0AD, false);
            g.drawString(this.font, values[row], rx + 50, ty, FordixPalette.TEXT_BRIGHT, false);
        }
        if (HudRules.locked(id)) {
            g.drawString(this.font, Tr.s("hud.editor.server"), rx + 8, propsTop + 4 * 15 + 36 - PANEL_MARGIN - 12, 0xFFE8A64A, false);
        }
    }

    private static final HudAnchors.Anchor[][] ANCHORS = {
            {HudAnchors.Anchor.TOP_LEFT, HudAnchors.Anchor.TOP_CENTER, HudAnchors.Anchor.TOP_RIGHT},
            {HudAnchors.Anchor.MIDDLE_LEFT, HudAnchors.Anchor.CENTER, HudAnchors.Anchor.MIDDLE_RIGHT},
            {HudAnchors.Anchor.BOTTOM_LEFT, HudAnchors.Anchor.BOTTOM_CENTER, HudAnchors.Anchor.BOTTOM_RIGHT}};

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
        if (isSelected) {
            // A soft glow around the selected element.
            for (int i = 3; i >= 1; i--) {
                guiGraphics.fill(rect.x() - i, rect.y() - i, rect.right() + i, rect.bottom() + i, ((0x22 >> (3 - i)) << 24) | accent);
            }
        }
        int top = isSelected ? 0x66000000 | accent : isHovered ? 0x4C000000 | accent : 0x2C000000 | accent;
        int bottom = isSelected ? 0x2A000000 | accent : isHovered ? 0x22000000 | accent : 0x10000000 | accent;
        guiGraphics.fillGradient(rect.x(), rect.y(), rect.right(), rect.bottom(), top, bottom);
        int border = isSelected ? 0xFF000000 | accent : isHovered ? FordixPalette.TEXT_BRIGHT : 0x99000000 | accent;
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, border);
        guiGraphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), border);
        guiGraphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), border);
        guiGraphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), border);
        String label = HudSlots.label(elements.get(index)) + (hiddenElement ? Tr.s("hud.hidden_suffix") : "");
        int textWidth = this.font.width(label);
        int textY = rect.h() >= 10 ? rect.y() + (rect.h() - 8) / 2 : rect.y() - 9;
        guiGraphics.drawString(this.font, label, rect.x() + (rect.w() - textWidth) / 2, textY, FordixPalette.TEXT_BRIGHT, true);
        if (isSelected) {
            for (int mode = 1; mode <= 3; mode++) {
                int hx = handleX(rect, mode);
                int hy = handleY(rect, mode);
                guiGraphics.fill(hx, hy, hx + HANDLE, hy + HANDLE, 0xFF000000 | accent);
                guiGraphics.fill(hx + 2, hy + 2, hx + HANDLE - 1, hy + HANDLE - 1, 0xFFFFFFFF);
            }
        }
    }

    /** The resize handle sits on the selected element's bottom-right corner, half outside it so small ones stay grabbable. */
    private static int handleX(HudSnap.Rect rect, int mode) {
        return (mode == 3 ? rect.x() + rect.w() / 2 : rect.right()) - HANDLE / 2 - 1;
    }

    private static int handleY(HudSnap.Rect rect, int mode) {
        return (mode == 2 ? rect.y() + rect.h() / 2 : rect.bottom()) - HANDLE / 2 - 1;
    }

    /** The handle under the pointer: 1 corner (both sizes), 2 right edge (width), 3 bottom edge (height), 0 none. */
    private int handleAt(double mouseX, double mouseY) {
        if (selected < 0) {
            return 0;
        }
        HudSnap.Rect rect = rectOf(selected);
        for (int mode = 1; mode <= 3; mode++) {
            if (mouseX >= handleX(rect, mode) - 1 && mouseX < handleX(rect, mode) + HANDLE + 1
                    && mouseY >= handleY(rect, mode) - 1 && mouseY < handleY(rect, mode) + HANDLE + 1) {
                return mode;
            }
        }
        return 0;
    }

    private void resize(int index, float scale) {
        HudSlots.setScale(elements.get(index), scale);
    }

    /** Width and height factors apart (JourneyMap's minimap only has one size). */
    private void resize(int index, float scaleX, float scaleY) {
        HudSlots.setScales(elements.get(index), scaleX, scaleY);
    }

    /**
     * A step of the wheel or of + and -: both sizes together, only the width with Ctrl, only the height with Alt;
     * 5 % a step, 1 % with Shift.
     */
    private void step(int index, float direction) {
        String id = elements.get(index);
        float amount = direction * (hasShiftDown() ? FINE_STEP : WHEEL_STEP);
        float sx = HudSlots.scale(id);
        float sy = HudSlots.scaleY(id);
        if (hasControlDown() && !HudSlots.isJourneyMap(id)) {
            resize(index, sx + amount, sy);
        } else if (hasAltDown() && !HudSlots.isJourneyMap(id)) {
            resize(index, sx, sy + amount);
        } else {
            resize(index, sx + amount, sy + amount);
        }
    }

    /** Offset of the selected element, next to it while it's being moved. */
    private void drawTag(GuiGraphics guiGraphics, int index) {
        int[] offset = HudSlots.offset(elements.get(index));
        HudSnap.Rect rect = rectOf(index);
        float scale = HudSlots.scale(elements.get(index));
        float scaleY = HudSlots.scaleY(elements.get(index));
        String size = "";
        if (Math.abs(scale - scaleY) > 0.005F) {
            size = "  " + Math.round(scale * 100) + "% x " + Math.round(scaleY * 100) + "%";
        } else if (Math.abs(scale - 1.0F) > 0.005F) {
            size = "  " + Math.round(scale * 100) + "%";
        }
        String text = HudSlots.anchor(elements.get(index)).id() + " " + offset[0] + ", " + offset[1] + size;
        int width = this.font.width(text) + 6;
        int x = Math.max(0, Math.min(this.width - width, rect.x()));
        int y = rect.y() >= 22 ? rect.y() - 20 : rect.bottom() + 3;
        guiGraphics.fill(x, y, x + width, y + 11, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
        guiGraphics.drawString(this.font, text, x + 3, y + 2, 0xFF05070A, false);
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
        if (clickPanels(mouseX, mouseY)) {
            return true;
        }
        int handle = handleAt(mouseX, mouseY);
        if (handle != 0) {
            record();
            resizing = true;
            resizeMode = handle;
            String id = elements.get(selected);
            resizeBaseWidth = rectOf(selected).w() / HudSlots.scale(id);
            resizeBaseHeight = rectOf(selected).h() / HudSlots.scaleY(id);
            resizeRatio = HudSlots.scaleY(id) / HudSlots.scale(id);
            return true;
        }
        int hit = elementAt(mouseX, mouseY);
        selected = hit;
        if (hit < 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (hit == lastClicked && now - lastClickAt < DOUBLE_CLICK_MS) {
            record();
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
        record();
        dragging = true;
        return true;
    }

    /** A click in a panel: a row of the element list (select, or the dot hides it) or one of the nine anchors. */
    private boolean clickPanels(double mouseX, double mouseY) {
        int rx0 = this.width - PANEL_MARGIN - PANEL_WIDTH;
        if (mouseY >= PANEL_MARGIN && mouseY < PANEL_MARGIN + HEADER) {
            if (mouseX >= PANEL_MARGIN && mouseX < PANEL_MARGIN + PANEL_WIDTH) {
                if (mouseX >= PANEL_MARGIN + PANEL_WIDTH - 16) {
                    onClose();
                } else {
                    leftOpen = !leftOpen;
                }
                return true;
            }
            if (mouseX >= rx0 && mouseX < rx0 + PANEL_WIDTH) {
                rightOpen = !rightOpen;
                return true;
            }
        }
        if (leftOpen && mouseX >= PANEL_MARGIN && mouseX < PANEL_MARGIN + PANEL_WIDTH && mouseY >= listTop && mouseY < listBottom) {
            int row = (int) ((mouseY - listTop + listScroll) / ROW_HEIGHT);
            if (row >= 0 && row < elements.size()) {
                selected = row;
                String id = elements.get(row);
                if (mouseX < PANEL_MARGIN + 20 && !HudRules.locked(id)) {
                    record();
                    HudSlots.setHidden(id, !HudSlots.hidden(id));
                    save();
                }
            }
            return true;
        }
        int rx = this.width - PANEL_MARGIN - PANEL_WIDTH;
        if (rightOpen && selected >= 0 && mouseX >= rx + 8 && mouseX < rx + 8 + 39 && mouseY >= anchorTop && mouseY < anchorTop + 39) {
            int col = (int) ((mouseX - rx - 8) / 13);
            int row = (int) ((mouseY - anchorTop) / 13);
            if (col < 3 && row < 3) {
                record();
                HudSlots.setAnchorKeepingPlace(elements.get(selected), ANCHORS[row][col], this.width, this.height);
                save();
            }
            return true;
        }
        return overPanel(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (resizing && selected >= 0 && button == 0) {
            HudSnap.Rect rect = rectOf(selected);
            String id = elements.get(selected);
            float width = (float) (mouseX - rect.x()) / Math.max(1.0F, resizeBaseWidth);
            float height = (float) (mouseY - rect.y()) / Math.max(1.0F, resizeBaseHeight);
            if (resizeMode == 2 && !HudSlots.isJourneyMap(id)) {
                resize(selected, width, HudSlots.scaleY(id));
            } else if (resizeMode == 3 && !HudSlots.isJourneyMap(id)) {
                resize(selected, HudSlots.scale(id), height);
            } else {
                resize(selected, width, width * resizeRatio);
            }
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
        if (leftOpen && mouseX < PANEL_MARGIN + PANEL_WIDTH && mouseY >= listTop && mouseY < listBottom) {
            int max = Math.max(0, elements.size() * ROW_HEIGHT - (listBottom - listTop));
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.signum(delta) * ROW_HEIGHT * 2));
            return true;
        }
        if (overPanel(mouseX, mouseY)) {
            return true;
        }
        int hit = elementAt(mouseX, mouseY);
        if (hit >= 0 && delta != 0.0D) {
            record();
            selected = hit;
            step(hit, (float) Math.signum(delta));
            save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_H && !hasControlDown()) {
            boolean open = !(leftOpen || rightOpen);
            leftOpen = open;
            rightOpen = open;
            return true;
        }
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            if (hasShiftDown()) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) {
            redo();
            return true;
        }
        if (selected >= 0 && (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD
                || keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT)) {
            boolean bigger = keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD;
            record();
            step(selected, bigger ? 1.0F : -1.0F);
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
                    record();
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
                record();
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
