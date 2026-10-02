package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.guide.GuideActions;
import com.omneria.fordix.client.guide.GuideEntry;
import com.omneria.fordix.client.guide.GuideTabs;
import com.omneria.fordix.client.guide.SeenPages;
import com.omneria.fordix.client.guide.ServerPages;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.guide.GuidePage;
import com.omneria.fordix.panel.network.PanelNetwork;
import com.omneria.fordix.welcome.ServerboundWelcomePayload;
import com.omneria.fordix.guide.PageMarkup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * All guides in one place, opened from the pause menu: the installed mods' guides (Ponder, Patchouli,
 * wikis) and the pages the server's admins wrote, with a search box. A page edited since the player last
 * read it carries a "nouveau" badge. The detail side shows the mod's guide with its button, or the page
 * with its heading, text and action buttons, scrollable.
 */
public class GuidesScreen extends FordixScreen {
    private static final int MAX_WIDTH = 440;
    private static final int MAX_HEIGHT = 260;
    private static final int LIST_WIDTH = 150;
    private static final int PADDING = 10;
    private static final int ROW = 18;

    private record Row(String section, String title, ItemStack icon, GuideEntry entry, GuidePage page) {
        boolean isNew() {
            return page != null && SeenPages.isNew(page);
        }
    }

    private record Drawn(int y, int height, PageMarkup.Block block, List<FormattedCharSequence> lines) {
    }

    private final Screen parent;
    private Panel panel;
    private EditBox search;
    private String query = "";
    private final List<Row> rows = new ArrayList<>();
    private int selected;
    private int listOffset;
    private double detailScroll;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public GuidesScreen(Screen parent) {
        super(Tr.c("guides.title"));
        this.parent = parent;
        // Ticks the welcome book's "open the Guides" step, if the server has one.
        PanelNetwork.CHANNEL.sendToServer(new ServerboundWelcomePayload("guides_opened", 0));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_WIDTH, this.width - 10);
        panelHeight = Math.min(MAX_HEIGHT, this.height - 10);
        left = centerX(panelWidth);
        top = centerY(panelHeight);
        panel = new Panel(left, top, panelWidth, panelHeight);
        search = new EditBox(this.font, left + PADDING, top + 24, LIST_WIDTH - 4, 16, Tr.c("guides.search"));
        search.setHint(Tr.c("guides.search_hint"));
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            rebuildRows();
        });
        addRenderableWidget(search);
        addRenderableWidget(new FordixButton(left + panelWidth - PADDING - 60, top + 4, 60, 14, Tr.c("common.close"), this::onClose));
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        String needle = query.toLowerCase(Locale.ROOT).trim();
        for (GuideEntry entry : GuideTabs.installedEntries()) {
            String title = ModNames.of(entry.modId()) + " : " + entry.label();
            if (needle.isEmpty() || title.toLowerCase(Locale.ROOT).contains(needle)) {
                rows.add(new Row(Tr.s("guides.section.mods"), entry.label(), GuideTabs.iconFor(entry.icon()), entry, null));
            }
        }
        for (GuidePage page : ServerPages.pages()) {
            if (needle.isEmpty() || page.title.toLowerCase(Locale.ROOT).contains(needle) || page.body.toLowerCase(Locale.ROOT).contains(needle)) {
                rows.add(new Row(Tr.s("guides.section.server"), page.title, new ItemStack(Items.WRITABLE_BOOK), null, page));
            }
        }
        selected = Math.max(0, Math.min(selected, rows.size() - 1));
        listOffset = 0;
        detailScroll = 0;
        if (!rows.isEmpty()) {
            markRead(rows.get(selected));
        }
    }

    /** Selects a row by index, clamped (also used by the dev test). */
    public void selectRow(int index) {
        if (rows.isEmpty()) {
            return;
        }
        selected = Math.max(0, Math.min(rows.size() - 1, index));
        detailScroll = 0;
        markRead(rows.get(selected));
    }

    private void markRead(Row row) {
        if (row.page() != null) {
            SeenPages.markSeen(row.page());
        }
    }

    private int listTop() {
        return top + 44;
    }

    private int visibleRows() {
        return Math.max(1, (top + panelHeight - PADDING - listTop()) / ROW);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(this.font, this.title, left + PADDING, top + 8, FordixPalette.ACCENT, false);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        drawList(guiGraphics, mouseX, mouseY);
        int detailX = left + PADDING + LIST_WIDTH + 8;
        guiGraphics.fill(detailX - 5, top + 24, detailX - 4, top + panelHeight - PADDING, FordixPalette.BEVEL_HIGHLIGHT);
        if (rows.isEmpty()) {
            guiGraphics.drawString(this.font, Tr.s("guides.none"), detailX, top + 30, FordixPalette.TEXT, false);
        } else {
            drawDetail(guiGraphics, rows.get(selected), detailX, mouseX, mouseY);
        }
    }

    private void drawList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = left + PADDING;
        int y = listTop();
        String section = "";
        int shown = 0;
        for (int i = listOffset; i < rows.size() && shown < visibleRows(); i++, shown++) {
            Row row = rows.get(i);
            boolean hover = mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= y && mouseY < y + ROW;
            if (i == selected) {
                guiGraphics.fill(x, y, x + LIST_WIDTH, y + ROW - 1, FordixPalette.BUTTON_FILL_HOVERED);
                guiGraphics.fill(x, y, x + 2, y + ROW - 1, FordixPalette.ACCENT);
            } else if (hover) {
                guiGraphics.fill(x, y, x + LIST_WIDTH, y + ROW - 1, FordixPalette.BUTTON_FILL);
            }
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + 4, y + 1, 0);
            guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
            guiGraphics.renderItem(row.icon(), 0, 0);
            guiGraphics.pose().popPose();
            int textWidth = LIST_WIDTH - 22 - (row.isNew() ? 34 : 0);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(row.title(), textWidth), x + 20, y + 5,
                    i == selected ? FordixPalette.TEXT_BRIGHT : FordixPalette.TEXT, false);
            if (row.isNew()) {
                int bx = x + LIST_WIDTH - 32;
                guiGraphics.fill(bx, y + 4, bx + 30, y + 14, FordixPalette.ACCENT);
                guiGraphics.drawString(this.font, Tr.s("guides.new_short"), bx + 6, y + 5, 0xFF05070A, false);
            }
            y += ROW;
            section = row.section();
        }
    }

    private void drawDetail(GuiGraphics guiGraphics, Row row, int x, int mouseX, int mouseY) {
        int right = left + panelWidth - PADDING;
        int y = top + 26;
        if (row.entry() != null) {
            GuideEntry entry = row.entry();
            guiGraphics.drawString(this.font, ModNames.of(entry.modId()) + " : " + entry.label(), x, y, FordixPalette.TEXT_BRIGHT, false);
            y += 16;
            for (FormattedCharSequence line : this.font.split(Component.literal(describe(entry)), right - x)) {
                guiGraphics.drawString(this.font, line, x, y, FordixPalette.TEXT, false);
                y += 10;
            }
            y += 8;
            drawButton(guiGraphics, x, y, 130, Tr.s("guides.open"), mouseX, mouseY);
            return;
        }
        GuidePage page = row.page();
        guiGraphics.drawString(this.font, page.title, x, y, FordixPalette.TEXT_BRIGHT, false);
        if (!page.author.isEmpty()) {
            String meta = Tr.s("guides.updated", java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(new java.util.Date(page.updatedAt)), page.author);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(meta, right - x), x, y + 11, FordixPalette.BEVEL_HIGHLIGHT, false);
        }
        int viewTop = y + 24;
        int viewBottom = top + panelHeight - PADDING;
        guiGraphics.enableScissor(x, viewTop, right, viewBottom);
        for (Drawn drawn : layoutPage(page, x, right, viewTop - (int) detailScroll)) {
            int dy = drawn.y();
            switch (drawn.block().type()) {
                case HEADING -> guiGraphics.drawString(this.font, drawn.block().text(), x, dy, FordixPalette.ACCENT, false);
                case TEXT -> {
                    int ly = dy;
                    for (FormattedCharSequence line : drawn.lines()) {
                        guiGraphics.drawString(this.font, line, x, ly, FordixPalette.TEXT, false);
                        ly += 10;
                    }
                }
                case BUTTON -> drawButton(guiGraphics, x, dy, Math.min(right - x, this.font.width(drawn.block().text()) + 24),
                        drawn.block().text(), mouseX, mouseY);
            }
        }
        guiGraphics.disableScissor();
    }

    private void drawButton(GuiGraphics guiGraphics, int x, int y, int w, String label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 16;
        guiGraphics.fill(x, y, x + w, y + 16, hover ? FordixPalette.BUTTON_FILL_HOVERED : FordixPalette.BUTTON_FILL);
        guiGraphics.fill(x, y + 15, x + w, y + 16, hover ? FordixPalette.ACCENT : FordixPalette.BEVEL_SHADOW);
        guiGraphics.drawCenteredString(this.font, label, x + w / 2, y + 4, FordixPalette.TEXT_BRIGHT);
    }

    /** Positions of the page's blocks from {@code startY}, wrapped to the column. */
    private List<Drawn> layoutPage(GuidePage page, int x, int right, int startY) {
        List<Drawn> drawn = new ArrayList<>();
        int y = startY;
        for (PageMarkup.Block block : PageMarkup.parse(page.body)) {
            switch (block.type()) {
                case HEADING -> {
                    y += drawn.isEmpty() ? 0 : 6;
                    drawn.add(new Drawn(y, 12, block, List.of()));
                    y += 14;
                }
                case TEXT -> {
                    List<FormattedCharSequence> lines = this.font.split(Component.literal(block.text()), right - x);
                    drawn.add(new Drawn(y, lines.size() * 10, block, lines));
                    y += lines.size() * 10 + 4;
                }
                case BUTTON -> {
                    drawn.add(new Drawn(y, 16, block, List.of()));
                    y += 20;
                }
            }
        }
        return drawn;
    }

    private int pageContentHeight(GuidePage page, int x, int right) {
        List<Drawn> drawn = layoutPage(page, x, right, 0);
        return drawn.isEmpty() ? 0 : drawn.get(drawn.size() - 1).y() + drawn.get(drawn.size() - 1).height();
    }

    private static String describe(GuideEntry entry) {
        return switch (entry.action()) {
            case PONDER -> Tr.s("guides.describe.ponder");
            case PATCHOULI -> Tr.s("guides.describe.patchouli");
            case URL -> Tr.s("guides.describe.url", entry.target());
            case COMMAND -> Tr.s("guides.describe.command", entry.target());
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int x = left + PADDING;
        if (button == 0 && mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= listTop()) {
            int index = listOffset + (int) ((mouseY - listTop()) / ROW);
            if (index >= 0 && index < rows.size() && index < listOffset + visibleRows()) {
                selected = index;
                detailScroll = 0;
                markRead(rows.get(index));
                return true;
            }
        }
        if (button == 0 && !rows.isEmpty()) {
            return clickDetail(rows.get(selected), mouseX, mouseY);
        }
        return false;
    }

    private boolean clickDetail(Row row, double mouseX, double mouseY) {
        int detailX = left + PADDING + LIST_WIDTH + 8;
        int right = left + panelWidth - PADDING;
        if (row.entry() != null) {
            int y = top + 26 + 16 + this.font.split(Component.literal(describe(row.entry())), right - detailX).size() * 10 + 8;
            if (mouseX >= detailX && mouseX < detailX + 130 && mouseY >= y && mouseY < y + 16) {
                GuideActions.run(row.entry());
                return true;
            }
            return false;
        }
        int viewTop = top + 26 + 24;
        if (mouseY < viewTop || mouseY >= top + panelHeight - PADDING) {
            return false;
        }
        for (Drawn drawn : layoutPage(row.page(), detailX, right, viewTop - (int) detailScroll)) {
            if (drawn.block().type() == PageMarkup.Type.BUTTON) {
                int w = Math.min(right - detailX, this.font.width(drawn.block().text()) + 24);
                if (mouseX >= detailX && mouseX < detailX + w && mouseY >= drawn.y() && mouseY < drawn.y() + 16) {
                    GuideEntry entry = GuideEntry.parse("minecraft | " + drawn.block().text() + " | minecraft:book | " + drawn.block().action());
                    if (entry != null) {
                        GuideActions.run(entry);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int detailX = left + PADDING + LIST_WIDTH + 4;
        if (mouseX < detailX) {
            listOffset = Math.max(0, Math.min(Math.max(0, rows.size() - visibleRows()), listOffset - (int) Math.signum(delta)));
            return true;
        }
        if (!rows.isEmpty() && rows.get(selected).page() != null) {
            int right = left + panelWidth - PADDING;
            int visible = top + panelHeight - PADDING - (top + 26 + 24);
            int max = Math.max(0, pageContentHeight(rows.get(selected).page(), detailX + 4, right) - visible);
            detailScroll = Math.max(0, Math.min(max, detailScroll - delta * 20));
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
