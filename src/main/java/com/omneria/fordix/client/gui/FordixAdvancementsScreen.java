package com.omneria.fordix.client.gui;

import com.omneria.fordix.client.gui.anim.Smoothing;
import com.omneria.fordix.client.gui.vanilla.Reflect;
import com.omneria.fordix.client.gui.vanilla.VoidframeDraw;
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

    private record Node(Advancement advancement, DisplayInfo display, Node parent) {
        int worldX() {
            return Math.round(display.getX() * PITCH_X);
        }

        int worldY() {
            return Math.round(display.getY() * PITCH_Y);
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
    private int tabWidth;

    private double scrollX;
    private double scrollY;
    private double targetX;
    private double targetY;
    private double tabScroll;
    private long lastFrame;
    private boolean dragging;

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
        tabsY = top + 20;
        viewX = left + PAD;
        viewY = tabsY + TAB_HEIGHT + 2;
        viewWidth = panelWidth - PAD * 2;
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
        for (Advancement root : advancements.getAdvancements().getRoots()) {
            if (root.getDisplay() == null) {
                continue;
            }
            Tab tab = new Tab(root);
            ArrayDeque<Node> pending = new ArrayDeque<>();
            pending.add(new Node(root, root.getDisplay(), null));
            while (!pending.isEmpty()) {
                Node node = pending.poll();
                tab.nodes.add(node);
                tab.minX = Math.min(tab.minX, node.worldX());
                tab.minY = Math.min(tab.minY, node.worldY());
                tab.maxX = Math.max(tab.maxX, node.worldX() + NODE);
                tab.maxY = Math.max(tab.maxY, node.worldY() + NODE);
                for (Advancement child : node.advancement().getChildren()) {
                    if (child.getDisplay() != null) {
                        pending.add(new Node(child, child.getDisplay(), node));
                    }
                }
            }
            tabs.add(tab);
        }
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
        guiGraphics.drawString(this.font, this.title, left + PAD, top + 5, FordixPalette.ACCENT, false);

        drawTabs(guiGraphics, mouseX, mouseY);
        VoidframeDraw.well(guiGraphics, viewX - 1, viewY - 1, viewWidth + 2, viewHeight + 2);
        Node hovered = null;
        if (selected == null) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("advancements.empty"),
                    viewX + viewWidth / 2, viewY + viewHeight / 2 - 4, FordixPalette.TEXT);
        } else {
            hovered = drawTree(guiGraphics, mouseX, mouseY);
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
        int available = panelWidth - PAD * 2;
        tabWidth = Math.max(64, Math.min(110, available / tabs.size()));
        double maxScroll = Math.max(0, tabs.size() * tabWidth - available);
        tabScroll = Math.max(0, Math.min(maxScroll, tabScroll));
        guiGraphics.enableScissor(left + PAD, tabsY, left + PAD + available, tabsY + TAB_HEIGHT);
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            int x = left + PAD + i * tabWidth - (int) tabScroll;
            boolean active = tab == selected;
            boolean hover = mouseX >= x && mouseX < x + tabWidth && mouseY >= tabsY && mouseY < tabsY + TAB_HEIGHT;
            guiGraphics.fill(x, tabsY, x + tabWidth - 2, tabsY + TAB_HEIGHT,
                    active || hover ? FordixPalette.BUTTON_FILL_HOVERED : FordixPalette.BUTTON_FILL);
            guiGraphics.fill(x, tabsY + TAB_HEIGHT - 2, x + tabWidth - 2, tabsY + TAB_HEIGHT,
                    active ? FordixPalette.ACCENT : FordixPalette.BEVEL_SHADOW);
            guiGraphics.renderFakeItem(tab.root.getDisplay().getIcon(), x + 4, tabsY + 3);
            String title = this.font.plainSubstrByWidth(tab.root.getDisplay().getTitle().getString(), tabWidth - 28);
            guiGraphics.drawString(this.font, title, x + 23, tabsY + 7,
                    active ? FordixPalette.TEXT_BRIGHT : FordixPalette.TEXT, false);
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

    private void drawLink(GuiGraphics guiGraphics, Node from, Node to, int offsetX, int offsetY) {
        int startX = offsetX + from.worldX() + NODE / 2;
        int startY = offsetY + from.worldY() + NODE / 2;
        int endX = offsetX + to.worldX() + NODE / 2;
        int endY = offsetY + to.worldY() + NODE / 2;
        int middle = (startX + endX) / 2;
        int color = isDone(to) ? FordixPalette.ACCENT : FordixPalette.BEVEL_HIGHLIGHT;
        guiGraphics.fill(Math.min(startX, middle), startY - 1, Math.max(startX, middle) + 1, startY + 1, color);
        guiGraphics.fill(middle - 1, Math.min(startY, endY), middle + 1, Math.max(startY, endY) + 1, color);
        guiGraphics.fill(Math.min(middle, endX), endY - 1, Math.max(middle, endX) + 1, endY + 1, color);
    }

    private void drawNode(GuiGraphics guiGraphics, Node node, int x, int y, boolean hover) {
        boolean done = isDone(node);
        int frame = frameColor(node.display().getFrame());
        int border = hover ? FordixPalette.TEXT_BRIGHT : done ? frame : FordixPalette.BEVEL_HIGHLIGHT;
        guiGraphics.fill(x, y, x + NODE, y + NODE, border);
        guiGraphics.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 2,
                done ? FordixPalette.BUTTON_FILL_HOVERED : FordixPalette.SLOT_FILL);
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

    private void drawTooltip(GuiGraphics guiGraphics, Node node, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        DisplayInfo display = node.display();
        lines.add(display.getTitle().getVisualOrderText());
        lines.addAll(this.font.split(display.getDescription(), 200));
        AdvancementProgress progress = progressOf(node.advancement());
        if (progress != null && progress.isDone()) {
            lines.add(Component.literal("Obtenu").getVisualOrderText());
        } else if (progress != null && progress.getProgressText() != null) {
            lines.add(Component.literal(progress.getProgressText()).getVisualOrderText());
        }
        guiGraphics.renderTooltip(this.font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && mouseY >= tabsY && mouseY < tabsY + TAB_HEIGHT
                && mouseX >= left + PAD && mouseX < left + panelWidth - PAD) {
            int index = (int) ((mouseX - left - PAD + tabScroll) / tabWidth);
            if (index >= 0 && index < tabs.size() && tabs.get(index) != selected) {
                select(tabs.get(index));
            }
            return true;
        }
        dragging = button == 0 && insideView(mouseX, mouseY);
        return dragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
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

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= tabsY && mouseY < tabsY + TAB_HEIGHT) {
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
