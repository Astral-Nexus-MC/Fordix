package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.client.wiki.ClientWiki;
import com.omneria.fordix.guide.PageMarkup;
import com.omneria.fordix.wiki.LootSummary;
import com.omneria.fordix.wiki.WikiAnswers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * A creature's sheet: what it can drop, as the server's loot table says (exact for the pack, mods and datapacks included),
 * and the server's wiki page for it (the fight, for a boss) next to it.
 */
public final class EntitySheetScreen extends FordixScreen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 250;
    private static final int PADDING = 10;
    private static final int ROW = 18;
    private static final int LIST_WIDTH = 210;

    private final Screen parent;
    private final EntityType<?> type;
    private final String id;
    private final boolean boss;
    private Panel panel;
    private int panelWidth = PANEL_WIDTH;
    private int panelHeight = PANEL_HEIGHT;
    private double lootScroll;
    private double pageScroll;
    private PageView pageView;
    private WikiAnswers.Page shownPage;
    private ItemStack hovered = ItemStack.EMPTY;

    public EntitySheetScreen(Screen parent, EntityType<?> type) {
        super(Tr.c("entity.title"));
        this.parent = parent;
        this.type = type;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        this.id = key == null ? "" : key.toString();
        this.boss = type.is(Tags.EntityTypes.BOSSES);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(PANEL_WIDTH, this.width - 10);
        panelHeight = Math.min(PANEL_HEIGHT, this.height - 10);
        panel = new Panel(centerX(panelWidth), centerY(panelHeight), panelWidth, panelHeight);
        pageView = new PageView(this.font);
        shownPage = null;
        addRenderableWidget(new FordixButton(panel.getX() + panelWidth - PADDING - 70, panel.getY() + panelHeight - PADDING - 18, 70, 18,
                Tr.c("common.back"), this::onClose).kind(Glass.Kind.PRIMARY));
    }

    private String namespace() {
        int colon = id.indexOf(':');
        return colon < 0 ? "minecraft" : id.substring(0, colon);
    }

    private String path() {
        return id.substring(id.indexOf(':') + 1);
    }

    private WikiAnswers.Page wikiPage() {
        return ClientWiki.hasWiki(namespace()) ? ClientWiki.pageFor(namespace(), path(), type.getDescription().getString()) : null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        panel.render(g, mouseX, mouseY, partialTick);
        hovered = ItemStack.EMPTY;
        int left = panel.getX() + PADDING;
        int top = panel.getY();
        int right = panel.getX() + panelWidth - PADDING;
        g.drawString(this.font, type.getDescription(), left, top + 10, FordixPalette.TEXT_BRIGHT, false);
        String sub = id + (boss ? "  -  " + Tr.s("entity.boss") : "");
        g.drawString(this.font, sub, left, top + 22, boss ? FordixPalette.ACCENT : FordixPalette.TEXT, false);
        Glass.rule(g, left, top + 36, right - left);

        int listBottom = top + panelHeight - PADDING - 22;
        drawLoot(g, left, top + 42, Math.min(LIST_WIDTH, (right - left) / 2), listBottom, mouseX, mouseY);
        int pageX = left + Math.min(LIST_WIDTH, (right - left) / 2) + 12;
        drawPage(g, pageX, top + 42, right, listBottom, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
        if (!hovered.isEmpty()) {
            g.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    private void drawLoot(GuiGraphics g, int x, int top, int width, int bottom, int mouseX, int mouseY) {
        Glass.label(g, this.font, Tr.s("entity.loot"), x, top);
        List<WikiAnswers.Drop> loot = ClientWiki.loot(id);
        int viewTop = top + 14;
        if (loot == null) {
            g.drawString(this.font, Tr.s("wiki.loading"), x, viewTop, FordixPalette.TEXT, false);
            return;
        }
        if (loot.isEmpty()) {
            g.drawString(this.font, Tr.s("entity.noloot"), x, viewTop, FordixPalette.TEXT, false);
            return;
        }
        g.enableScissor(x, viewTop, x + width, bottom);
        int y = viewTop - (int) lootScroll;
        for (WikiAnswers.Drop drop : loot) {
            if (y + ROW >= viewTop && y <= bottom) {
                drawDrop(g, drop, x, y, width, mouseX, mouseY);
            }
            y += ROW;
        }
        g.disableScissor();
        if (lootMax(loot, bottom - viewTop) > 0) {
            g.drawString(this.font, "...", x + width - 12, top, FordixPalette.TEXT, false);
        }
    }

    private int lootMax(List<WikiAnswers.Drop> loot, int visible) {
        return Math.max(0, loot.size() * ROW - visible);
    }

    private void drawDrop(GuiGraphics g, WikiAnswers.Drop drop, int x, int y, int width, int mouseX, int mouseY) {
        boolean isTag = drop.flags().contains(LootSummary.TAG);
        ItemStack stack = ItemStack.EMPTY;
        String name = drop.item();
        ResourceLocation key = ResourceLocation.tryParse(drop.item());
        if (key != null && !isTag && !drop.flags().contains(LootSummary.TABLE) && ForgeRegistries.ITEMS.containsKey(key)) {
            stack = new ItemStack(ForgeRegistries.ITEMS.getValue(key));
            name = stack.getHoverName().getString();
        }
        Glass.card(g, x, y, width, ROW - 1);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 1, y + 1);
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW - 1) {
                hovered = stack;
            }
        }
        String count = drop.min() == drop.max() ? "x" + drop.min() : "x" + drop.min() + "-" + drop.max();
        String chance = (drop.chance() >= 100 ? "100" : trim(drop.chance())) + " %";
        int chanceWidth = this.font.width(chance);
        int countWidth = this.font.width(count);
        g.drawString(this.font, chance, x + width - chanceWidth - 3, y + 5, drop.chance() >= 100 ? FordixPalette.TEXT_BRIGHT : FordixPalette.TEXT, false);
        g.drawString(this.font, count, x + width - chanceWidth - countWidth - 10, y + 5, FordixPalette.TEXT, false);
        int nameWidth = width - 22 - chanceWidth - countWidth - 14;
        g.drawString(this.font, this.font.plainSubstrByWidth(name, nameWidth), x + 22, y + 5, FordixPalette.TEXT_BRIGHT, false);
        String flags = flagsText(drop.flags());
        if (!flags.isEmpty() && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW - 1) {
            g.renderTooltip(this.font, Component.literal(flags), mouseX, mouseY);
        }
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    private static String flagsText(String flags) {
        List<String> parts = new ArrayList<>();
        for (char flag : flags.toCharArray()) {
            parts.add(Tr.s("entity.flag." + flag));
        }
        return String.join(", ", parts);
    }

    private void drawPage(GuiGraphics g, int x, int top, int right, int bottom, int mouseX, int mouseY) {
        Glass.label(g, this.font, Tr.s(boss ? "entity.fight" : "entity.wiki"), x, top);
        int viewTop = top + 14;
        if (!ClientWiki.hasWiki(namespace())) {
            g.drawString(this.font, Tr.s("entity.nowiki"), x, viewTop, FordixPalette.TEXT, false);
            return;
        }
        WikiAnswers.Page page = wikiPage();
        if (page == null) {
            g.drawString(this.font, Tr.s("wiki.loading"), x, viewTop, FordixPalette.TEXT, false);
            return;
        }
        if (ClientWiki.isMissing(page)) {
            g.drawString(this.font, Tr.s("wiki.missing"), x, viewTop, FordixPalette.TEXT, false);
            return;
        }
        if (page != shownPage) {
            shownPage = page;
            pageView.setBlocks(PageMarkup.parse(page.body()));
        }
        pageView.render(g, x, viewTop, right - x, bottom - viewTop, pageScroll, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int left = panel.getX() + PADDING;
        int split = left + Math.min(LIST_WIDTH, (panel.getX() + panelWidth - PADDING - left) / 2) + 6;
        int viewTop = panel.getY() + 42 + 14;
        int bottom = panel.getY() + panelHeight - PADDING - 22;
        if (mouseX < split) {
            List<WikiAnswers.Drop> loot = ClientWiki.loot(id);
            int max = loot == null ? 0 : lootMax(loot, bottom - viewTop);
            lootScroll = Math.max(0, Math.min(max, lootScroll - delta * ROW));
        } else if (shownPage != null) {
            int width = panel.getX() + panelWidth - PADDING - (split + 6);
            int max = Math.max(0, pageView.contentHeight(width) - (bottom - viewTop));
            pageScroll = Math.max(0, Math.min(max, pageScroll - delta * 18));
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && shownPage != null) {
            int left = panel.getX() + PADDING;
            int x = left + Math.min(LIST_WIDTH, (panel.getX() + panelWidth - PADDING - left) / 2) + 12;
            int viewTop = panel.getY() + 42 + 14;
            int bottom = panel.getY() + panelHeight - PADDING - 22;
            PageMarkup.Block block = pageView.click(x, viewTop, panel.getX() + panelWidth - PADDING - x, bottom - viewTop, pageScroll, mouseX, mouseY);
            if (block != null && block.type() == PageMarkup.Type.LINK) {
                this.minecraft.setScreen(new WikiScreen(this, namespace(), block.action()));
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
