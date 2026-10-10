package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.anim.Smoothing;
import com.omneria.fordix.client.gui.vanilla.Reflect;
import com.omneria.fordix.client.gui.vanilla.VoidframeDraw;
import com.omneria.fordix.client.hud.HudStyle;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The advancements screen in the Voidframe style: tabs along the top, a tree you drag or
 * wheel-scroll with eased motion, nodes lit up in the accent color once obtained, a
 * tooltip per node and a progress bar for the tab. It only reads {@link ClientAdvancements}
 * (roots, children and the progress map the client already keeps up to date), so it works
 * with any advancement datapack. Replaces vanilla's screen when Voidframe screens are on.
 */
public class FordixAdvancementsScreen extends FordixScreen {
    private static final int NODE = 26;
    private static final int PITCH_X = 34;
    private static final int PITCH_Y = 32;
    private static final int TAB_HEIGHT = 22;
    private static final int PAD = 8;
    private static final int SCROLL_PADDING = 40;
    private static final double EASE_RATE = 14.0;
    private static final int GOAL_GREEN = 0xFF5ED68A;
    private static final int CHALLENGE_PURPLE = 0xFFB08CFF;
    /** ClientAdvancements keeps exactly one Map: advancement to progress. */
    private static final Field PROGRESS_FIELD = Reflect.uniqueField(ClientAdvancements.class, Map.class);

    private static final class Node {
        private final Advancement advancement;
        private final DisplayInfo display;
        private final Node parent;
        /** Vertical shift that stacks several roots of one mod under each other in one tab. */
        int offsetY;

        Node(Advancement advancement, DisplayInfo display, Node parent) {
            this.advancement = advancement;
            this.display = display;
            this.parent = parent;
        }

        Advancement advancement() {
            return advancement;
        }

        DisplayInfo display() {
            return display;
        }

        Node parent() {
            return parent;
        }

        int worldX() {
            return Math.round(display.getX() * PITCH_X);
        }

        int worldY() {
            return Math.round(display.getY() * PITCH_Y) + offsetY;
        }
    }

    private static final class Tab {
        final Advancement root;
        final List<Node> nodes = new ArrayList<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        Tab(Advancement root) {
            this.root = root;
        }
    }

    /** The column of tabs on the left: one row per mod, with its icon, its name and how far along the player is. */
    private static final int RAIL_WIDTH = 112;
    private static final int RAIL_ROW = 30;
    private static final int RAIL_STEP = 34;

    private final ClientAdvancements advancements;
    private final List<Tab> tabs = new ArrayList<>();
    private int builtFor = -1;
    private Tab selected;

    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int viewX;
    private int viewY;
    private int viewWidth;
    private int viewHeight;
    private int tabsY;
    /** Each tab is as wide as the mod name it carries (icon and padding included): names are never cut. */
    private int[] tabStarts = new int[0];
    private int[] tabWidths = new int[0];

    private double scrollX;
    private double scrollY;
    private double targetX;
    private double targetY;
    private double tabScroll;
    private long lastFrame;
    private boolean dragging;
    private boolean draggingBar;

    public FordixAdvancementsScreen(ClientAdvancements advancements) {
        super(Component.translatable("gui.advancements"));
        this.advancements = advancements;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(this.width - 30, 420);
        panelHeight = Math.min(this.height - 24, 250);
        left = centerX(panelWidth);
        top = centerY(panelHeight);
        tabsY = top + 22;
        viewX = left + PAD + RAIL_WIDTH + 6;
        viewY = tabsY;
        viewWidth = panelWidth - PAD * 2 - RAIL_WIDTH - 6;
        viewHeight = top + panelHeight - PAD - 18 - viewY;
        addRenderableWidget(new FordixButton(left + panelWidth - 22, top + 4, 16, 12, Component.literal("x"), this::onClose));
        rebuild();
        lastFrame = System.nanoTime();
    }

    /** Reads the roots and their trees; redone whenever the client learns of new advancements. */
    private void rebuild() {
        Advancement previous = selected != null ? selected.root : null;
        tabs.clear();
        selected = null;
        // One tab per mod: a mod's several roots (Minecraft has five) are stacked in the same tab.
        Map<String, Tab> byMod = new LinkedHashMap<>();
        for (Advancement root : advancements.getAdvancements().getRoots()) {
            if (root.getDisplay() == null) {
                continue;
            }
            Tab tab = byMod.computeIfAbsent(root.getId().getNamespace(), key -> new Tab(root));
            List<Node> tree = new ArrayList<>();
            ArrayDeque<Node> pending = new ArrayDeque<>();
            pending.add(new Node(root, root.getDisplay(), null));
            while (!pending.isEmpty()) {
                Node node = pending.poll();
                tree.add(node);
                for (Advancement child : node.advancement().getChildren()) {
                    if (child.getDisplay() != null) {
                        pending.add(new Node(child, child.getDisplay(), node));
                    }
                }
            }
            int treeMin = Integer.MAX_VALUE;
            for (Node node : tree) {
                treeMin = Math.min(treeMin, node.worldY());
            }
            int shift = tab.nodes.isEmpty() ? 0 : tab.maxY + 2 * PITCH_Y - treeMin;
            for (Node node : tree) {
                node.offsetY = shift;
                tab.nodes.add(node);
                tab.minX = Math.min(tab.minX, node.worldX());
                tab.minY = Math.min(tab.minY, node.worldY());
                tab.maxX = Math.max(tab.maxX, node.worldX() + NODE);
                tab.maxY = Math.max(tab.maxY, node.worldY() + NODE);
            }
        }
        tabs.addAll(byMod.values());
        builtFor = advancements.getAdvancements().getAllAdvancements().size();
        for (Tab tab : tabs) {
            if (tab.root == previous) {
                selected = tab;
            }
        }
        if (selected == null && !tabs.isEmpty()) {
            select(tabs.get(0));
        }
    }

    private void select(Tab tab) {
        selected = tab;
        advancements.setSelectedTab(tab.root, true);
        // Frame the root, vertically centered, and let the view glide there.
        double[] xs = xRange(tab);
        double[] ys = yRange(tab);
        Node root = tab.nodes.get(0);
        targetX = xs[0];
        targetY = Math.max(ys[0], Math.min(ys[1], root.worldY() + NODE / 2.0 - viewHeight / 2.0));
    }

    private double[] xRange(Tab tab) {
        return Smoothing.scrollRange(tab.minX, tab.maxX, viewWidth, SCROLL_PADDING);
    }

    private double[] yRange(Tab tab) {
        return Smoothing.scrollRange(tab.minY, tab.maxY, viewHeight, SCROLL_PADDING);
    }

    private void clampTargets() {
        if (selected == null) {
            return;
        }
        double[] xs = xRange(selected);
        double[] ys = yRange(selected);
        targetX = Math.max(xs[0], Math.min(xs[1], targetX));
        targetY = Math.max(ys[0], Math.min(ys[1], targetY));
    }

    private AdvancementProgress progressOf(Advancement advancement) {
        if (Reflect.get(PROGRESS_FIELD, advancements) instanceof Map<?, ?> map
                && map.get(advancement) instanceof AdvancementProgress progress) {
            return progress;
        }
        return null;
    }

    private boolean isDone(Node node) {
        AdvancementProgress progress = progressOf(node.advancement());
        return progress != null && progress.isDone();
    }

    /** Hidden advancements only show once obtained, like vanilla. */
    private boolean isShown(Node node) {
        return !node.display().isHidden() || isDone(node);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (advancements.getAdvancements().getAllAdvancements().size() != builtFor) {
            rebuild();
        }
        long now = System.nanoTime();
        double seconds = Math.min(0.1, (now - lastFrame) / 1.0e9);
        lastFrame = now;
        clampTargets();
        scrollX = Smoothing.step(scrollX, targetX, seconds, EASE_RATE);
        scrollY = Smoothing.step(scrollY, targetY, seconds, EASE_RATE);

        this.renderBackground(guiGraphics);
        Panel.draw(guiGraphics, left, top, panelWidth, panelHeight);
        VoidframeDraw.titleBand(guiGraphics, left, top, panelWidth);
        Glass.title(guiGraphics, this.font, this.title.getString(), left + PAD, top + 5);

        drawTabs(guiGraphics, mouseX, mouseY);
        VoidframeDraw.well(guiGraphics, viewX - 1, viewY - 1, viewWidth + 2, viewHeight + 2);
        Node hovered = null;
        if (selected == null) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("advancements.empty"),
                    viewX + viewWidth / 2, viewY + viewHeight / 2 - 4, FordixPalette.TEXT);
        } else {
            hovered = drawTree(guiGraphics, mouseX, mouseY);
            drawScrollbars(guiGraphics);
            drawInfoBar(guiGraphics);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (hovered != null) {
            drawTooltip(guiGraphics, hovered, mouseX, mouseY);
        }
    }

    private void drawTabs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (tabs.isEmpty()) {
            return;
        }
        int x = left + PAD;
        int railHeight = top + panelHeight - PAD - 18 - tabsY;
        double maxScroll = Math.max(0, tabs.size() * RAIL_STEP - railHeight);
        tabScroll = Math.max(0, Math.min(maxScroll, tabScroll));
        guiGraphics.enableScissor(x, tabsY, x + RAIL_WIDTH, tabsY + railHeight);
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            int y = tabsY + i * RAIL_STEP - (int) tabScroll;
            boolean active = tab == selected;
            boolean hover = mouseX >= x && mouseX < x + RAIL_WIDTH && mouseY >= y && mouseY < y + RAIL_ROW;
            Glass.card(guiGraphics, x, y, RAIL_WIDTH, RAIL_ROW);
            Glass.row(guiGraphics, x, y, RAIL_WIDTH, RAIL_ROW, active, hover);
            guiGraphics.renderFakeItem(tab.root.getDisplay().getIcon(), x + 6, y + 7);
            String title = ModNames.of(tab.root.getId().getNamespace());
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(title, RAIL_WIDTH - 32), x + 28, y + 5,
                    active ? FordixPalette.TEXT_BRIGHT : FordixPalette.TEXT, false);
            int done = 0;
            int total = 0;
            for (Node node : tab.nodes) {
                if (isShown(node)) {
                    total++;
                    if (isDone(node)) {
                        done++;
                    }
                }
            }
            guiGraphics.drawString(this.font, done + " / " + total, x + 28, y + 16, Glass.dim(), false);
            HudStyle.bar(guiGraphics, x + 28, y + RAIL_ROW - 5, RAIL_WIDTH - 36, 2, total == 0 ? 0.0F : done / (float) total, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
        }
        guiGraphics.disableScissor();
    }

    /** Draws the selected tab's tree and returns the node under the mouse, if any. */
    private Node drawTree(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int offsetX = viewX - (int) Math.round(scrollX);
        int offsetY = viewY - (int) Math.round(scrollY);
        guiGraphics.enableScissor(viewX, viewY, viewX + viewWidth, viewY + viewHeight);
        // Dotted grid that moves with the tree.
        int spacing = 17;
        int firstX = viewX + Math.floorMod(offsetX - viewX, spacing);
        int firstY = viewY + Math.floorMod(offsetY - viewY, spacing);
        for (int x = firstX; x < viewX + viewWidth; x += spacing) {
            for (int y = firstY; y < viewY + viewHeight; y += spacing) {
                guiGraphics.fill(x, y, x + 1, y + 1, FordixPalette.PROGRESS_TRACK);
            }
        }
        for (Node node : selected.nodes) {
            if (node.parent() != null && isShown(node) && isShown(node.parent())) {
                drawLink(guiGraphics, node.parent(), node, offsetX, offsetY);
            }
        }
        Node hovered = null;
        boolean mouseInView = mouseX >= viewX && mouseX < viewX + viewWidth && mouseY >= viewY && mouseY < viewY + viewHeight;
        for (Node node : selected.nodes) {
            if (!isShown(node)) {
                continue;
            }
            int x = offsetX + node.worldX();
            int y = offsetY + node.worldY();
            if (x + NODE < viewX || x > viewX + viewWidth || y + NODE < viewY || y > viewY + viewHeight) {
                continue;
            }
            boolean hover = mouseInView && mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE;
            if (hover) {
                hovered = node;
            }
            drawNode(guiGraphics, node, x, y, hover);
        }
        guiGraphics.disableScissor();
        return hovered;
    }

    private double[] barThumb() {
        double[] ys = yRange(selected);
        double range = ys[1] - ys[0];
        if (range <= 0) {
            return null;
        }
        double content = range + viewHeight;
        int thumb = (int) Math.max(14, viewHeight * viewHeight / content);
        double ratio = Math.max(0, Math.min(1, (scrollY - ys[0]) / range));
        return new double[]{viewY + ratio * (viewHeight - thumb), thumb};
    }

    private void drawScrollbars(GuiGraphics guiGraphics) {
        double[] thumb = barThumb();
        if (thumb != null) {
            int x = viewX + viewWidth - 5;
            guiGraphics.fill(x, viewY, x + 4, viewY + viewHeight, 0x88000000 | (FordixPalette.PROGRESS_TRACK & 0xFFFFFF));
            guiGraphics.fill(x, (int) thumb[0], x + 4, (int) (thumb[0] + thumb[1]),
                    draggingBar ? FordixPalette.TEXT_BRIGHT : FordixPalette.ACCENT);
        }
        double[] xs = xRange(selected);
        double range = xs[1] - xs[0];
        if (range > 0) {
            int thumbW = (int) Math.max(14, viewWidth * (double) viewWidth / (range + viewWidth));
            double ratio = Math.max(0, Math.min(1, (scrollX - xs[0]) / range));
            int y = viewY + viewHeight - 5;
            int endX = viewX + viewWidth - (thumb != null ? 6 : 0);
            guiGraphics.fill(viewX, y, endX, y + 4, 0x88000000 | (FordixPalette.PROGRESS_TRACK & 0xFFFFFF));
            int tx = viewX + (int) (ratio * (endX - viewX - thumbW));
            guiGraphics.fill(tx, y, tx + thumbW, y + 4, FordixPalette.ACCENT);
        }
    }

    private void drawLink(GuiGraphics guiGraphics, Node from, Node to, int offsetX, int offsetY) {
        int startX = offsetX + from.worldX() + NODE / 2;
        int startY = offsetY + from.worldY() + NODE / 2;
        int endX = offsetX + to.worldX() + NODE / 2;
        int endY = offsetY + to.worldY() + NODE / 2;
        int middle = (startX + endX) / 2;
        boolean done = isDone(to);
        int color = done ? FordixPalette.ACCENT : 0x40FFFFFF;
        if (done) {
            int glow = 0x30000000 | (FordixPalette.ACCENT & 0xFFFFFF);
            guiGraphics.fill(Math.min(startX, middle), startY - 2, Math.max(startX, middle) + 2, startY + 2, glow);
            guiGraphics.fill(middle - 2, Math.min(startY, endY), middle + 2, Math.max(startY, endY) + 1, glow);
            guiGraphics.fill(Math.min(middle, endX), endY - 2, Math.max(middle, endX) + 1, endY + 2, glow);
        }
        guiGraphics.fill(Math.min(startX, middle), startY - 1, Math.max(startX, middle) + 1, startY + 1, color);
        guiGraphics.fill(middle - 1, Math.min(startY, endY), middle + 1, Math.max(startY, endY) + 1, color);
        guiGraphics.fill(Math.min(middle, endX), endY - 1, Math.max(middle, endX) + 1, endY + 1, color);
    }

    private void drawNode(GuiGraphics guiGraphics, Node node, int x, int y, boolean hover) {
        boolean done = isDone(node);
        int frame = frameColor(node.display().getFrame());
        int border = hover ? FordixPalette.TEXT_BRIGHT : done ? frame : 0x40FFFFFF;
        if (done) {
            guiGraphics.fill(x - 2, y - 2, x + NODE + 2, y + NODE + 2, 0x28000000 | (frame & 0xFFFFFF));
        }
        guiGraphics.fill(x, y, x + NODE, y + NODE, border);
        guiGraphics.fillGradient(x + 1, y + 1, x + NODE - 1, y + NODE - 1,
                done ? FordixPalette.BUTTON_FILL_HOVERED : FordixPalette.BUTTON_FILL, FordixPalette.SLOT_FILL);
        guiGraphics.renderFakeItem(node.display().getIcon(), x + 5, y + 5);
        if (!done) {
            guiGraphics.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 2, 0x99000000 | (FordixPalette.SLOT_FILL & 0xFFFFFF));
            AdvancementProgress progress = progressOf(node.advancement());
            if (progress != null && progress.getPercent() > 0.0F) {
                int width = Math.round((NODE - 6) * progress.getPercent());
                guiGraphics.fill(x + 3, y + NODE - 5, x + 3 + width, y + NODE - 3, FordixPalette.ACCENT);
            }
        }
    }

    private static int frameColor(FrameType frame) {
        return switch (frame) {
            case TASK -> FordixPalette.ACCENT;
            case GOAL -> GOAL_GREEN;
            case CHALLENGE -> CHALLENGE_PURPLE;
        };
    }

    private void drawInfoBar(GuiGraphics guiGraphics) {
        int done = 0;
        int total = 0;
        for (Node node : selected.nodes) {
            if (isShown(node)) {
                total++;
                if (isDone(node)) {
                    done++;
                }
            }
        }
        int y = top + panelHeight - PAD - 12;
        String text = done + " / " + total;
        guiGraphics.drawString(this.font, text, viewX, y + 2, FordixPalette.TEXT_BRIGHT, false);
        int barX = viewX + this.font.width(text) + 8;
        int barWidth = viewX + viewWidth - barX;
        guiGraphics.fill(barX, y + 3, barX + barWidth, y + 7, FordixPalette.PROGRESS_TRACK);
        if (total > 0) {
            guiGraphics.fill(barX, y + 3, barX + Math.round(barWidth * (done / (float) total)), y + 7, FordixPalette.ACCENT);
        }
    }

    /** The detail card of a node under the mouse: title in its frame color, description, and whether it is done or how far it is. */
    private void drawTooltip(GuiGraphics guiGraphics, Node node, int mouseX, int mouseY) {
        DisplayInfo display = node.display();
        int accent = frameColor(display.getFrame());
        List<FormattedCharSequence> description = this.font.split(display.getDescription(), 190);
        AdvancementProgress progress = progressOf(node.advancement());
        boolean done = progress != null && progress.isDone();
        String state = done ? Tr.s("advancements.done") : progress != null && progress.getProgressText() != null ? progress.getProgressText() : "";
        String title = display.getTitle().getString();
        int width = Math.max(this.font.width(title), 150);
        int height = 12 + 12 + description.size() * 10 + (state.isEmpty() ? 0 : 16) + 8;
        int w = width + 20;
        int x = Math.min(mouseX + 12, this.width - w - 4);
        int y = Math.min(mouseY + 12, this.height - height - 4);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
        HudStyle.panel(guiGraphics, x, y, w, height, accent & 0xFFFFFF, 246, FordixPalette.PANEL_FILL & 0xFFFFFF);
        guiGraphics.drawString(this.font, title, x + 10, y + 8, 0xFF000000 | accent, false);
        int line = y + 22;
        for (FormattedCharSequence text : description) {
            guiGraphics.drawString(this.font, text, x + 10, line, FordixPalette.TEXT, false);
            line += 10;
        }
        if (!state.isEmpty()) {
            Glass.pill(guiGraphics, this.font, state, x + 10, line + 3, done ? 0xFF5ED68A : FordixPalette.ACCENT);
        }
        guiGraphics.flush();
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && mouseX >= left + PAD && mouseX < left + PAD + RAIL_WIDTH && mouseY >= tabsY) {
            int index = (int) ((mouseY - tabsY + tabScroll) / RAIL_STEP);
            if (index >= 0 && index < tabs.size() && tabs.get(index) != selected) {
                select(tabs.get(index));
            }
            return true;
        }
        if (button == 0 && selected != null && mouseX >= viewX + viewWidth - 7 && mouseX < viewX + viewWidth
                && mouseY >= viewY && mouseY < viewY + viewHeight && barThumb() != null) {
            draggingBar = true;
            dragBar(mouseY);
            return true;
        }
        dragging = button == 0 && insideView(mouseX, mouseY);
        return dragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar && button == 0) {
            dragBar(mouseY);
            return true;
        }
        if (dragging && button == 0) {
            // Follow the cursor immediately; the easing only smooths wheel and tab moves.
            targetX -= dragX;
            targetY -= dragY;
            clampTargets();
            scrollX = targetX;
            scrollY = targetY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void dragBar(double mouseY) {
        double[] thumb = barThumb();
        double[] ys = yRange(selected);
        double free = viewHeight - thumb[1];
        double ratio = free <= 0 ? 0 : Math.max(0, Math.min(1, (mouseY - viewY - thumb[1] / 2) / free));
        targetY = ys[0] + ratio * (ys[1] - ys[0]);
        scrollY = targetY;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        draggingBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < viewX - 3 && mouseY >= tabsY) {
            tabScroll -= delta * 30;
            return true;
        }
        if (insideView(mouseX, mouseY)) {
            if (hasShiftDown()) {
                targetX -= delta * 45;
            } else {
                targetY -= delta * 45;
            }
            clampTargets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean insideView(double mouseX, double mouseY) {
        return mouseX >= viewX && mouseX < viewX + viewWidth && mouseY >= viewY && mouseY < viewY + viewHeight;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft.options.keyAdvancements.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Tells the server the tab screen is closed, as vanilla's does, so it stops marking advancements as unseen. */
    @Override
    public void removed() {
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null) {
            connection.send(ServerboundSeenAdvancementsPacket.closedScreen());
        }
        super.removed();
    }
}
