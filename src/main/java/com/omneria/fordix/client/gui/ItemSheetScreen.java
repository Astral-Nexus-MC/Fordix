package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.guide.GuideActions;
import com.omneria.fordix.client.guide.GuideEntry;
import com.omneria.fordix.client.guide.GuideHints;
import com.omneria.fordix.client.guide.ItemFacts;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.client.gui.widget.SegmentedControl;
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
    private static final int PANEL_WIDTH = 600;
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
    /** Where the "used in" band starts under the panel's top. */
    private int usesTop = 156;
    /** A small screen shows one section at a time (make, figures, uses) behind tabs instead of two columns and a band. */
    private boolean tabs;
    private int section = 1;
    private int contentTop;
    private FordixButton previous;
    private FordixButton next;
    private int barHeight = 5;
    private int barPitch = 25;
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
        tabs = this.width < 440 || this.height < 280;
        barHeight = tabs ? 4 : 5;
        barPitch = tabs ? 20 : 25;
        int available = this.height - 10;
        if (tabs) {
            panelHeight = Math.min(available, PANEL_HEIGHT);
            contentTop = 58;
            usesTop = contentTop;
        } else {
            // The sheet is as tall as what it shows (the recipe, the figures, the uses): no empty middle. The wiki page takes all it can.
            int figures = 22 + figuresHeight();
            int recipe = sheet.makes().isEmpty() ? 24 : 40 + sheet.makes().stream().mapToInt(m -> m.rows()).max().orElse(1) * SLOT + 14;
            int columns = Math.max(figures, recipe) + 8;
            int useRows = Math.max(1, Math.min(3, (sheet.uses().size() + USES_PER_ROW - 1) / USES_PER_ROW));
            int wanted = 46 + columns + 14 + 12 + useRows * SLOT + 40;
            panelHeight = showWiki ? Math.min(PANEL_HEIGHT, available) : Math.min(available, Math.max(260, Math.min(PANEL_HEIGHT, wanted)));
            usesTop = 46 + columns + 14;
            contentTop = 48;
        }
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
        previous = null;
        next = null;
        if (sheet.makes().size() > 1) {
            previous = addRenderableWidget(new FordixButton(left + PADDING + 150, top + contentTop + 14, 16, 14, Component.literal("<"),
                    () -> recipeIndex = (recipeIndex + sheet.makes().size() - 1) % sheet.makes().size()));
            next = addRenderableWidget(new FordixButton(left + PADDING + 170, top + contentTop + 14, 16, 14, Component.literal(">"),
                    () -> recipeIndex = (recipeIndex + 1) % sheet.makes().size()));
        }
        if (tabs) {
            addRenderableWidget(new SegmentedControl(left + PADDING, top + 38, 14,
                    List.of(Tr.s("sheet.make"), Tr.s("sheet.facts"), Tr.s("sheet.tab.uses")), () -> section, index -> {
                        section = index;
                        showWiki = false;
                    }));
        }
    }

    /** Height the figures column takes: a line and a bar per stat, a heading before each group. */
    private int figuresHeight() {
        int height = 0;
        int group = 0;
        for (ItemFacts.Fact fact : sheet.facts()) {
            if (fact.group() != 0 && fact.group() != group) {
                height += 14;
            }
            group = fact.group();
            height += fact.fraction() >= 0.0F ? barPitch : 12;
        }
        return height + (hasMarks() ? 12 : 0);
    }

    private boolean hasMarks() {
        for (ItemFacts.Fact fact : sheet.facts()) {
            if (fact.best() >= 0.0F || fact.held() >= 0.0F) {
                return true;
            }
        }
        return false;
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
        if (previous != null) {
            boolean makeVisible = !showWiki && (!tabs || section == 0);
            previous.visible = makeVisible;
            next.visible = makeVisible;
        }
        drawHeader(g, left, top, right, mouseX, mouseY);

        if (tabs) {
            int y = top + contentTop;
            if (showWiki) {
                drawWiki(g, left, y, right, mouseX, mouseY);
            } else if (section == 0) {
                drawMake(g, left, y, mouseX, mouseY);
            } else if (section == 1) {
                drawFacts(g, left, y, right - left, top + panelHeight - PADDING - 28);
            } else {
                drawUses(g, left, y, right, top + panelHeight - PADDING - 24, true, mouseX, mouseY);
            }
        } else {
            int columnWidth = (right - left) / 2 - 6;
            drawMake(g, left, top + 48, mouseX, mouseY);
            int fx = left + columnWidth + 12;
            Glass.divider(g, fx - 6, top + 50, top + usesTop - 8);
            drawFacts(g, fx, top + 48, right - fx, top + usesTop - 8);
            if (showWiki) {
                drawWiki(g, left, top + usesTop, right, mouseX, mouseY);
            } else {
                drawUses(g, left, top + usesTop, right, top + panelHeight - PADDING - 22, false, mouseX, mouseY);
            }
        }

        super.render(g, mouseX, mouseY, partialTick);
        if (!hovered.isEmpty()) {
            g.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    /** The head of the sheet: the item in a well, its name, its id, its rarity and type. */
    private void drawHeader(GuiGraphics g, int left, int top, int right, int mouseX, int mouseY) {
        int box = tabs ? 26 : 36;
        float itemScale = tabs ? 1.25F : 2.0F;
        float nameScale = tabs ? 1.2F : 1.6F;
        Glass.well(g, left - 2, top + 6, box, box);
        g.pose().pushPose();
        g.pose().translate(left - 2 + (box - 16 * itemScale) / 2, top + 6 + (box - 16 * itemScale) / 2, 0);
        g.pose().scale(itemScale, itemScale, 1);
        g.renderItem(sheet.stack(), 0, 0);
        g.pose().popPose();
        int textX = left + box + 4;
        g.pose().pushPose();
        g.pose().translate(textX, top + 8, 0);
        g.pose().scale(nameScale, nameScale, 1);
        g.drawString(this.font, this.font.plainSubstrByWidth(sheet.name(), (int) ((right - textX) / nameScale)), 0, 0, FordixPalette.TEXT_BRIGHT, false);
        g.pose().popPose();
        int infoY = top + (tabs ? 22 : 26);
        int pillX = textX;
        if (tabs) {
            pillX += Glass.pill(g, this.font, com.omneria.fordix.client.tooltip.ItemRarity.label(sheet.stack()), pillX, infoY - 1,
                    com.omneria.fordix.client.tooltip.ItemRarity.color(sheet.stack())) + 3;
            pillX += Glass.pill(g, this.font, com.omneria.fordix.client.tooltip.ItemTooltipRenderer.typeOf(sheet.stack()), pillX, infoY - 1, FordixPalette.TEXT) + 4;
            g.drawString(this.font, this.font.plainSubstrByWidth(sheet.id(), Math.max(0, right - pillX)), pillX, infoY, Glass.dim(), false);
        } else {
            g.drawString(this.font, sheet.id(), textX, infoY, Glass.dim(), false);
            int pillY = top + 24;
            int x = textX + Math.max(this.font.width(sheet.id()), 0) + 10;
            x += Glass.pill(g, this.font, com.omneria.fordix.client.tooltip.ItemRarity.label(sheet.stack()), x, pillY - 1,
                    com.omneria.fordix.client.tooltip.ItemRarity.color(sheet.stack())) + 4;
            Glass.pill(g, this.font, com.omneria.fordix.client.tooltip.ItemTooltipRenderer.typeOf(sheet.stack()), x, pillY - 1, FordixPalette.TEXT);
            String mod = sheet.mod();
            if (!mod.isEmpty()) {
                g.drawString(this.font, mod, right - this.font.width(mod), top + 10, Glass.dim(), false);
            }
        }
        if (!tabs) {
            Glass.rule(g, left, top + 42, right - left);
        }
        hit(left, top + 6, box, box, sheet.stack(), mouseX, mouseY, false);
    }

    /** How to make it: the station, the recipe grid, its result and the other ways. */
    private void drawMake(GuiGraphics g, int left, int top, int mouseX, int mouseY) {
        Glass.label(g, this.font, Tr.s("sheet.make"), left, top);
        List<ItemFacts.RecipeView> makes = sheet.makes();
        if (makes.isEmpty()) {
            g.drawString(this.font, Tr.s("sheet.nomake"), left, top + 18, FordixPalette.TEXT, false);
            return;
        }
        ItemFacts.RecipeView view = makes.get(Math.min(recipeIndex, makes.size() - 1));
        String head = view.station() + (makes.size() > 1 ? "  " + (recipeIndex + 1) + "/" + makes.size() : "");
        g.drawString(this.font, this.font.plainSubstrByWidth(head, 140), left, top + 16, FordixPalette.TEXT, false);
        drawRecipe(g, view, left, top + 32, mouseX, mouseY);
    }

    /** The figures, under a heading per group: a line, a bar with its marks, the gap to the item in hand. */
    private void drawFacts(GuiGraphics g, int x, int top, int width, int limit) {
        Glass.label(g, this.font, Tr.s("sheet.facts"), x, top);
        int right = x + width;
        if (hasMarks()) {
            String held = Tr.s("sheet.legend.held");
            String best = Tr.s("sheet.legend.best");
            int hx = right - this.font.width(held);
            g.drawString(this.font, held, hx, top, Glass.dim(), false);
            g.fill(hx - 8, top + 4, hx - 3, top + 5, FordixPalette.TEXT_BRIGHT);
            int bx = hx - 8 - 8 - this.font.width(best);
            g.drawString(this.font, best, bx, top, Glass.dim(), false);
            g.fill(bx - 5, top, bx - 4, top + 9, 0x99FFFFFF);
        }
        int fy = top + 16;
        int group = 0;
        for (ItemFacts.Fact fact : sheet.facts()) {
            boolean bar = fact.fraction() >= 0.0F;
            int heading = fact.group() != 0 && fact.group() != group ? 14 : 0;
            if (fy + heading + (bar ? barPitch - 6 : 10) > limit) {
                break;
            }
            if (heading > 0) {
                Glass.label(g, this.font, Tr.s(fact.group() == 1 ? "sheet.group.combat" : "sheet.group.sturdy"), x, fy);
                fy += heading;
            }
            group = fact.group();
            String value = this.font.plainSubstrByWidth(fact.value(), width / 3);
            int valueWidth = this.font.width(value);
            int valueX = right - valueWidth;
            if (!fact.delta().isEmpty()) {
                int pillWidth = this.font.width(fact.delta()) + 10;
                Glass.pill(g, this.font, fact.delta(), valueX - pillWidth - 4, fy - 2, fact.deltaColor());
                valueX -= pillWidth + 4;
            }
            g.drawString(this.font, this.font.plainSubstrByWidth(fact.label(), Math.max(0, valueX - x - 6)), x, fy, FordixPalette.TEXT, false);
            g.drawString(this.font, value, right - valueWidth, fy, fact.color() == 0 ? FordixPalette.TEXT_BRIGHT : fact.color(), false);
            if (bar) {
                Glass.statBar(g, x, fy + 11, width, barHeight, fact.fraction(), fact.barColor(), fact.best(), fact.held());
                fy += barPitch;
            } else {
                fy += 12;
            }
        }
    }

    /** What it is used in: a strip of items, each opening its own sheet. */
    private void drawUses(GuiGraphics g, int left, int uy, int right, int bottom, boolean tabbed, int mouseX, int mouseY) {
        if (!tabbed) {
            g.fill(left, uy - 4, right, uy - 3, 0x20FFFFFF);
        }
        Glass.label(g, this.font, Tr.s(sheet.uses().isEmpty() ? "sheet.uses_none" : "sheet.uses"), left, uy);
        int perRow = Math.max(1, Math.min(USES_PER_ROW, (right - left) / SLOT));
        int shown = 0;
        for (ItemStack use : sheet.uses()) {
            int column = shown % perRow;
            int row = shown / perRow;
            int ux = left + column * SLOT;
            int uyy = uy + 12 + row * SLOT;
            if (uyy + SLOT > bottom) {
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
        if (showWiki && shownPage != null && mouseY > panel.getY() + usesTop - 4) {
            int width = panelWidth - 2 * PADDING;
            int max = Math.max(0, pageView.contentHeight(width) - wikiHeight(panel.getY() + usesTop));
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
