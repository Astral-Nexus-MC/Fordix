package com.omneria.fordix.client.gui.options;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.FordixScreen;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.TabButton;
import com.omneria.fordix.client.hud.HudTokens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The common page of the Voidframe options screens (video, sound): a glass panel as tall as the screen, a title, optional tabs,
 * a list of {@code SettingRow}s that scrolls under its own clip, and a foot with "defaults" and "done". On a small screen the
 * rows lose their explanation line. The options themselves stay the game's: a row reads and writes the same
 * {@code OptionInstance}, and the game's own options file is saved when the page closes.
 */
public abstract class OptionsPageScreen extends FordixScreen {
    private static final int WIDTH = 520;
    private static final int FOOT = 34;

    protected final Screen parent;
    private final List<AbstractWidget> rows = new ArrayList<>();
    private final Map<AbstractWidget, Integer> base = new IdentityHashMap<>();
    private final Set<AbstractWidget> hidden = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Runnable> resets = new ArrayList<>();
    private final List<TabButton> tabButtons = new ArrayList<>();
    protected int tab;
    protected int panelX;
    protected int panelY;
    protected int panelW;
    protected int panelH;
    protected int listTop;
    protected int listBottom;
    /** A small screen: rows without their explanation line, thinner spacing. */
    protected boolean small;
    private int contentHeight;
    private double scroll;

    protected OptionsPageScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** The names of the tabs, or none for a page without tabs. */
    protected List<String> tabNames() {
        return List.of();
    }

    /** Adds the rows of the open tab with {@link #addRow}, the first one {@code listTop} pixels from the screen's top; returns the y after the last. */
    protected abstract int buildRows(int left, int width);

    /** Space between the panel's top and the list: the title, and the tabs when there are some. A page with a search field makes it taller. */
    protected int listOffset() {
        return tabNames().isEmpty() ? 26 : 44;
    }

    /** Adds the widgets of the header (a search field, a filter) once the tabs are in place. */
    protected void buildHeader() {
    }

    protected String defaultsLabel() {
        return Tr.s("options.page.defaults");
    }

    /** What the "defaults" button does: by default every row that gave a reset puts its value back. */
    protected void onDefaults() {
        for (Runnable reset : resets) {
            reset.run();
        }
    }

    /** Moves a row to {@code y} (screen coordinates at no scroll), for a page that filters and re-flows its rows. */
    protected void setRowBase(AbstractWidget row, int y) {
        base.put(row, y);
    }

    /** Shows or hides a row for good (a filter), whatever the scroll. */
    protected void hideRow(AbstractWidget row, boolean hide) {
        if (hide) {
            hidden.add(row);
        } else {
            hidden.remove(row);
        }
    }

    /** After the rows were moved: the list now ends at {@code end}. */
    protected void rowsMoved(int end) {
        contentHeight = Math.max(0, end - listTop);
        clampScroll();
        applyScroll();
    }

    /** Drawn in the header, right of the title. */
    protected void renderHeader(GuiGraphics g, int x, int y, int right) {
    }

    /** Drawn in the foot, between the two buttons. */
    protected void renderFoot(GuiGraphics g, int x, int y, int right) {
    }

    @Override
    protected int minWidth() {
        return 320;
    }

    @Override
    protected int minHeight() {
        return 240;
    }

    protected int rowHeight() {
        return small ? 22 : 28;
    }

    protected int rowGap() {
        return 2;
    }

    @Override
    protected void init() {
        small = this.height < 300 || this.width < 420;
        panelW = Math.min(WIDTH, this.width - 10);
        panelH = this.height - (small ? 10 : 12);
        panelX = centerX(panelW);
        panelY = (this.height - panelH) / 2;
        listTop = panelY + listOffset();
        listBottom = panelY + panelH - FOOT;
        rows.clear();
        base.clear();
        resets.clear();
        tabButtons.clear();
        List<String> names = tabNames();
        if (tab >= names.size()) {
            tab = 0;
        }
        int tx = panelX + 12;
        for (int i = 0; i < names.size(); i++) {
            int index = i;
            int width = this.font.width(names.get(i)) + 14;
            TabButton button = new TabButton(tx, panelY + 24, width, 16, Component.literal(names.get(i)), null, () -> {
                tab = index;
                scroll = 0;
                this.rebuildWidgets();
            });
            button.setSelected(i == tab);
            tabButtons.add(button);
            addRenderableWidget(button);
            tx += width + 2;
        }
        buildHeader();
        int end = buildRows(panelX + 8, panelW - 16);
        contentHeight = Math.max(0, end - listTop);
        for (AbstractWidget row : rows) {
            addWidget(row);
        }
        clampScroll();
        applyScroll();
        int by = panelY + panelH - FOOT + 8;
        addRenderableWidget(new FordixButton(panelX + 12, by, Math.max(small ? 76 : 100, this.font.width(defaultsLabel()) + 16), 18, Component.literal(defaultsLabel()), this::onDefaults));
        addRenderableWidget(new FordixButton(panelX + panelW - 12 - 80, by, 80, 18, Tr.c("options.page.done"), this::onClose).kind(Glass.Kind.PRIMARY));
    }

    /** A scrolling row; {@code reset} (may be null) is what "defaults" does for it. */
    protected <T extends AbstractWidget> T addRow(T row, Runnable reset) {
        rows.add(row);
        base.put(row, row.getY());
        if (reset != null) {
            resets.add(reset);
        }
        return row;
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(Math.max(0, contentHeight - (listBottom - listTop) + 4), scroll));
    }

    private void applyScroll() {
        for (AbstractWidget row : rows) {
            row.setY(base.get(row) - (int) scroll);
            row.visible = !hidden.contains(row) && row.getY() + row.getHeight() > listTop && row.getY() < listBottom;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        Glass.panel(g, panelX, panelY, panelW, panelH);
        Glass.title(g, this.font, getTitle().getString(), panelX + 14, panelY + 9);
        renderHeader(g, panelX + 14 + this.font.width(getTitle().getString()) + 12, panelY + 9, panelX + panelW - 12);
        for (int i = 0; i < tabButtons.size(); i++) {
            if (i == tab) {
                TabButton button = tabButtons.get(i);
                g.fill(button.getX() + 3, button.getY() + button.getHeight() - 2, button.getX() + button.getWidth() - 3, button.getY() + button.getHeight(), TabButton.accent());
            }
        }
        g.fill(panelX + 12, listBottom + 2, panelX + panelW - 12, listBottom + 3, 0x20FFFFFF);
        renderFoot(g, panelX + 12 + (small ? 80 : 104), panelY + panelH - FOOT + 12, panelX + panelW - 12 - 84);
        super.render(g, mouseX, mouseY, partialTick);
        g.enableScissor(panelX + 2, listTop, panelX + panelW - 2, listBottom);
        for (AbstractWidget row : rows) {
            if (row.visible) {
                row.render(g, mouseX, mouseY, partialTick);
            }
        }
        g.disableScissor();
        int viewport = listBottom - listTop;
        if (contentHeight > viewport) {
            int barX = panelX + panelW - 6;
            g.fill(barX, listTop, barX + 3, listBottom, 0x12FFFFFF);
            int size = Math.max(12, viewport * viewport / contentHeight);
            int from = listTop + (int) ((viewport - size) * (scroll / Math.max(1, contentHeight - viewport + 4)));
            g.fill(barX, from, barX + 3, from + size, HudTokens.withAlpha(FordixPalette.ACCENT, 0xB0));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= listTop && mouseY < listBottom) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        // A row scrolled half under the foot or the header must not take a click that lands on them. The click may rebuild the page
        // (a tab, a link to another screen), which replaces the rows: only the rows that were there before are put back.
        List<AbstractWidget> before = new ArrayList<>(rows);
        boolean[] shown = new boolean[before.size()];
        for (int i = 0; i < shown.length; i++) {
            shown[i] = before.get(i).visible;
            before.get(i).visible = false;
        }
        try {
            return super.mouseClicked(mouseX, mouseY, button);
        } finally {
            for (int i = 0; i < shown.length; i++) {
                before.get(i).visible = shown[i];
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contentHeight > listBottom - listTop) {
            scroll -= delta * 20;
            clampScroll();
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.options.save();
            this.minecraft.setScreen(parent);
        }
    }

    /** A small capitals heading between two groups of rows. */
    protected static final class Heading extends AbstractWidget {
        private final String text;

        public Heading(int x, int y, int width, String text) {
            super(x, y, width, 12, Component.literal(text));
            this.text = text;
            this.active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = net.minecraft.client.Minecraft.getInstance().font;
            Glass.label(g, font, text, getX() + 6, getY() + 2);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}
