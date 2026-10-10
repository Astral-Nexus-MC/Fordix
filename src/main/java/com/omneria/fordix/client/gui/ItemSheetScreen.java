package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.guide.GuideActions;
import com.omneria.fordix.client.guide.GuideEntry;
import com.omneria.fordix.client.guide.GuideHints;
import com.omneria.fordix.client.guide.ItemFacts;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.client.wiki.ClientWiki;
import com.omneria.fordix.guide.PageMarkup;
import com.omneria.fordix.wiki.WikiAnswers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * An item's sheet, drawn from what the game knows (see {@link ItemFacts}): how to make it, what it is
 * used in and its figures. Clicking an ingredient or a result opens that item's sheet, Back returns.
 */
public final class ItemSheetScreen extends FordixScreen {
    private static final int PANEL_WIDTH = 714;
    private static final int PANEL_HEIGHT = 400;
    private static final int PADDING = 10;
    private static final int SLOT = 18;
    private static final int USES_PER_ROW = 16;

    private final Screen parent;
    private final ItemFacts.Sheet sheet;
    private final GuideEntry guide;
    private int recipeIndex;
    private boolean showWiki;
    private PageView pageView;
    private WikiAnswers.Page shownPage;
    private double pageScroll;
    private int panelWidth = PANEL_WIDTH;
    private int panelHeight = PANEL_HEIGHT;
    private Panel panel;
    private final List<Hit> hits = new ArrayList<>();
    private ItemStack hovered = ItemStack.EMPTY;

    private record Hit(int x, int y, ItemStack stack) {
    }

    public ItemSheetScreen(Screen parent, ItemStack stack) {
        super(Tr.c("sheet.title"));
        this.parent = parent;
        this.sheet = ItemFacts.of(stack);
        this.guide = GuideHints.entryFor(stack);
    }

    @Override
    protected int minWidth() {
        return 380;
    }

    @Override
    protected int minHeight() {
        return 240;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(PANEL_WIDTH, this.width - 10);
        panelHeight = Math.min(PANEL_HEIGHT, this.height - 10);
        int left = centerX(panelWidth);
        int top = centerY(panelHeight);
        panel = new Panel(left, top, panelWidth, panelHeight);
        pageView = new PageView(this.font);
        shownPage = null;
        int by = top + panelHeight - PADDING - 18;
        int x = left + PADDING;
        String namespace = sheet.id().contains(":") ? sheet.id().substring(0, sheet.id().indexOf(':')) : "minecraft";
        if (ClientWiki.hasWiki(namespace)) {
            addRenderableWidget(new FordixButton(x, by, 96, 18, Tr.c(showWiki ? "sheet.tab.uses" : "sheet.tab.wiki"), () -> {
                showWiki = !showWiki;
                this.rebuildWidgets();
            }));
        } else {
            addRenderableWidget(new FordixButton(x, by, 96, 18, Tr.c("sheet.wiki"), () -> {
                GuideActions.run(GuideEntry.parse("minecraft | Wiki | minecraft:book | url:https://minecraft.wiki/w/{objet}"), sheet.stack());
            }));
        }
        x += 100;
        if (guide != null && !(guide.action() == GuideEntry.Action.URL && guide.target().contains("{objet}"))) {
            String label = this.font.plainSubstrByWidth(guide.label(), 140);
            addRenderableWidget(new FordixButton(x, by, 150, 18, Component.literal(label), () -> GuideActions.run(guide, sheet.stack())));
        }
        addRenderableWidget(new FordixButton(left + panelWidth - PADDING - 70, by, 70, 18, Tr.c("common.back"), this::onClose).kind(Glass.Kind.PRIMARY));
        if (sheet.makes().size() > 1) {
            addRenderableWidget(new FordixButton(left + PADDING + 150, top + 62, 16, 14, Component.literal("<"),
                    () -> recipeIndex = (recipeIndex + sheet.makes().size() - 1) % sheet.makes().size()));
            addRenderableWidget(new FordixButton(left + PADDING + 170, top + 62, 16, 14, Component.literal(">"),
                    () -> recipeIndex = (recipeIndex + 1) % sheet.makes().size()));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        panel.render(g, mouseX, mouseY, partialTick);
        hits.clear();
        hovered = ItemStack.EMPTY;
        int left = panel.getX() + PADDING;
        int top = panel.getY();
        int right = panel.getX() + panelWidth - PADDING;

        // Header: big icon in a well, name, id and mod.
        Glass.well(g, left - 2, top + 6, 36, 36);
        g.pose().pushPose();
        g.pose().translate(left, top + 8, 0);
        g.pose().scale(2, 2, 1);
        g.renderItem(sheet.stack(), 0, 0);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(left + 40, top + 8, 0);
        g.pose().scale(1.6F, 1.6F, 1);
        g.drawString(this.font, sheet.name(), 0, 0, FordixPalette.TEXT_BRIGHT, false);
        g.pose().popPose();
        g.drawString(this.font, sheet.id(), left + 40, top + 26, Glass.dim(), false);
        int pillX = left + 40 + Math.round(this.font.width(sheet.name()) * 1.6F) + 10;
        pillX += Glass.pill(g, this.font, com.omneria.fordix.client.tooltip.ItemRarity.label(sheet.stack()), pillX, top + 10,
                com.omneria.fordix.client.tooltip.ItemRarity.color(sheet.stack())) + 4;
        Glass.pill(g, this.font, com.omneria.fordix.client.tooltip.ItemTooltipRenderer.typeOf(sheet.stack()), pillX, top + 10, FordixPalette.TEXT);
        Glass.rule(g, left, top + 42, right - left);
        hit(left, top + 8, 32, 32, sheet.stack(), mouseX, mouseY, false);

        boolean tall = panelHeight >= 360;
        // Left column: how to make it.
        int columnWidth = (right - left) / 2 - 6;
        Glass.label(g, this.font, Tr.s("sheet.make"), left, top + 48);
        List<ItemFacts.RecipeView> makes = sheet.makes();
        if (makes.isEmpty()) {
            g.drawString(this.font, Tr.s("sheet.nomake"), left, top + 66, FordixPalette.TEXT, false);
        } else {
            ItemFacts.RecipeView view = makes.get(Math.min(recipeIndex, makes.size() - 1));
            String head = view.station() + (makes.size() > 1 ? "  " + (recipeIndex + 1) + "/" + makes.size() : "");
            g.drawString(this.font, this.font.plainSubstrByWidth(head, 140), left, top + 64, FordixPalette.TEXT, false);
            drawRecipe(g, view, left, top + 80, mouseX, mouseY);
        }

        // Right column: figures.
        int fx = left + columnWidth + 12;
        Glass.label(g, this.font, Tr.s("sheet.facts"), fx, top + 48);
        int fy = top + (tall ? 70 : 62);
        for (ItemFacts.Fact fact : sheet.facts()) {
            if (fy > top + (tall ? panelHeight - 90 : 150)) {
                break;
            }
            int available = right - fx;
            String value = this.font.plainSubstrByWidth(fact.value(), available * 2 / 3);
            int valueWidth = this.font.width(value);
            g.drawString(this.font, this.font.plainSubstrByWidth(fact.label(), available - valueWidth - 6), fx, fy, FordixPalette.TEXT, false);
            g.drawString(this.font, value, right - valueWidth, fy, fact.color() == 0 ? FordixPalette.TEXT_BRIGHT : fact.color(), false);
            if (fact.fraction() >= 0.0F) {
                com.omneria.fordix.client.hud.HudStyle.bar(g, fx, fy + 10, available, tall ? 5 : 3, fact.fraction(), fact.barColor());
                fy += tall ? 30 : 15;
            } else {
                fy += tall ? 22 : 11;
            }
        }

        if (showWiki) {
            drawWiki(g, left, top + (tall ? 206 : 156), right, mouseX, mouseY);
        } else {
            // Uses.
            int uy = top + (tall ? 206 : 156);
            g.fill(left, uy - 4, right, uy - 3, 0x20FFFFFF);
            Glass.label(g, this.font, Tr.s(sheet.uses().isEmpty() ? "sheet.uses_none" : "sheet.uses"), left, uy);
            int shown = 0;
            for (ItemStack use : sheet.uses()) {
                int column = shown % USES_PER_ROW;
                int row = shown / USES_PER_ROW;
                int ux = left + column * SLOT;
                int uyy = uy + 12 + row * SLOT;
                if (uyy + SLOT > top + panelHeight - PADDING - 22) {
                    break;
                }
                g.renderItem(use, ux + 1, uyy + 1);
                hit(ux, uyy, SLOT, SLOT, use, mouseX, mouseY, true);
                shown++;
            }
            if (shown < sheet.uses().size()) {
                String more = "+" + (sheet.uses().size() - shown);
                g.drawString(this.font, more, right - this.font.width(more), uy, FordixPalette.TEXT, false);
            }
        }

        super.render(g, mouseX, mouseY, partialTick);
        if (!hovered.isEmpty()) {
            g.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    private void drawWiki(GuiGraphics g, int left, int uy, int right, int mouseX, int mouseY) {
        g.fill(left, uy - 4, right, uy - 3, 0x20FFFFFF);
        String path = sheet.id().substring(sheet.id().indexOf(':') + 1);
        String namespace = sheet.id().contains(":") ? sheet.id().substring(0, sheet.id().indexOf(':')) : "minecraft";
        WikiAnswers.Page page = ClientWiki.pageFor(namespace, path, sheet.name());
        if (page == null) {
            g.drawString(this.font, Tr.s("wiki.loading"), left, uy, FordixPalette.TEXT, false);
            return;
        }
        if (ClientWiki.isMissing(page)) {
            g.drawString(this.font, Tr.s("sheet.nowiki"), left, uy, FordixPalette.TEXT, false);
            return;
        }
        if (page != shownPage) {
            shownPage = page;
            pageScroll = 0;
            pageView.setBlocks(PageMarkup.parse(page.body()));
        }
        pageView.render(g, left, uy, right - left, wikiHeight(uy), pageScroll, mouseX, mouseY);
    }

    private int wikiHeight(int uy) {
        return panel.getY() + panelHeight - PADDING - 22 - uy;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showWiki && shownPage != null && mouseY > panel.getY() + 152) {
            int width = panelWidth - 2 * PADDING;
            int max = Math.max(0, pageView.contentHeight(width) - wikiHeight(panel.getY() + 156));
            pageScroll = Math.max(0, Math.min(max, pageScroll - delta * 18));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void drawRecipe(GuiGraphics g, ItemFacts.RecipeView view, int x, int y, int mouseX, int mouseY) {
        long tick = System.currentTimeMillis() / 1000;
        for (int i = 0; i < view.cells().size(); i++) {
            int cx = x + (i % view.columns()) * SLOT;
            int cy = y + (i / view.columns()) * SLOT;
            Glass.well(g, cx, cy, SLOT - 1, SLOT - 1);
            List<ItemStack> options = view.cells().get(i);
            if (!options.isEmpty()) {
                ItemStack stack = options.get((int) (tick % options.size()));
                g.renderItem(stack, cx + 1, cy + 1);
                hit(cx, cy, SLOT - 1, SLOT - 1, stack, mouseX, mouseY, true);
            }
        }
        int ax = x + view.columns() * SLOT + 6;
        int ay = y + (view.rows() * SLOT) / 2 - 4;
        g.drawString(this.font, "->", ax, ay, FordixPalette.ACCENT, false);
        int rx = ax + 18;
        int ry = y + (view.rows() * SLOT) / 2 - SLOT / 2;
        Glass.well(g, rx, ry, SLOT - 1, SLOT - 1);
        g.renderItem(view.result(), rx + 1, ry + 1);
        g.renderItemDecorations(this.font, view.result(), rx + 1, ry + 1);
        hit(rx, ry, SLOT - 1, SLOT - 1, view.result(), mouseX, mouseY, false);
        if (!view.note().isEmpty()) {
            g.drawString(this.font, view.note(), x, y + view.rows() * SLOT + 4, FordixPalette.TEXT, false);
        }
    }

    /** Registers a hoverable item; {@code link} makes a click open that item's sheet. */
    private void hit(int x, int y, int w, int h, ItemStack stack, int mouseX, int mouseY, boolean link) {
        if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
            hovered = stack;
            if (link && !stack.is(sheet.stack().getItem())) {
                hits.add(new Hit(x, y, stack));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && !hits.isEmpty()) {
            this.minecraft.setScreen(new ItemSheetScreen(this, hits.get(0).stack()));
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
