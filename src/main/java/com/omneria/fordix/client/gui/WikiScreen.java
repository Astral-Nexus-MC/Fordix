package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.wiki.ClientWiki;
import com.omneria.fordix.client.wiki.WikiMemory;
import com.omneria.fordix.wiki.WikiAnswers;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.guide.PageMarkup;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * A wiki kept on the server, read in the game: a search box over all the server's wikis (on the server, so it covers the
 * text of every page), the list of the wiki's pages, and the page itself with its pictures and links to other pages.
 * Opened by a guide tab whose mod has an imported wiki ({@code /fordix wiki import}), instead of a web page.
 */
public class WikiScreen extends FordixScreen {
    private static final int MAX_WIDTH = 470;
    private static final int MAX_HEIGHT = 270;
    private static final int LIST_WIDTH = 150;
    private static final int PADDING = 10;
    private static final int ROW = 18;
    private static final int SEARCH_DELAY_TICKS = 8;

    private record Row(String wiki, String slug, String title, String detail, boolean header) {
        Row(String wiki, String slug, String title, String detail) {
            this(wiki, slug, title, detail, false);
        }
    }

    /** A clickable line of the wiki's home page (a favourite or a page read last), with the rectangle it was drawn in. */
    private record HomeLink(int x, int y, int width, WikiMemory.Ref ref) {
    }

    private static final int LETTERS_FROM = 40;
    private static final List<String> KNOWN_CATEGORIES = List.of("item", "block", "mob");

    private static WikiMemory memory;

    private static WikiMemory memory() {
        if (memory == null) {
            memory = new WikiMemory(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("fordix-wiki-memory.json"));
        }
        return memory;
    }

    /** A search to start with, left by the guide screen right before it opens this one. */
    public static String nextQuery;

    private final Screen parent;
    private String wikiId;
    private String openSlug;
    private String selectedWiki;
    private String selectedSlug = "";
    private final Deque<String[]> history = new ArrayDeque<>();
    private Panel panel;
    private PageView pageView;
    private EditBox search;
    private String query = "";
    private String activeQuery = "";
    private int idleTicks;
    private int listOffset;
    private double detailScroll;
    private WikiAnswers.Page shownPage;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private FordixButton backButton;
    private FordixButton categoryButton;
    private String category = "";
    private final List<HomeLink> homeLinks = new ArrayList<>();
    private WikiAnswers.Page visitedPage;
    private String zoomImage;

    /** {@code slug} is a page to open as soon as the list is known (the page of an item), or null. */
    public WikiScreen(Screen parent, String wikiId, String slug) {
        super(Tr.c("wiki.title"));
        this.parent = parent;
        this.wikiId = wikiId;
        this.openSlug = slug;
        this.selectedWiki = wikiId;
        if (nextQuery != null) {
            this.query = nextQuery;
            this.activeQuery = nextQuery;
            nextQuery = null;
        }
        ClientWiki.requestList();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_WIDTH, this.width - 10);
        panelHeight = Math.min(MAX_HEIGHT, this.height - 10);
        left = centerX(panelWidth);
        top = centerY(panelHeight);
        panel = new Panel(left, top, panelWidth, panelHeight);
        pageView = new PageView(this.font);
        pageView.setInteractive(true);
        shownPage = null;
        search = new EditBox(this.font, left + PADDING, top + 24, LIST_WIDTH - 4, 16, Tr.c("wiki.search"));
        search.setHint(Tr.c("wiki.search_hint"));
        search.setMaxLength(60);
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            idleTicks = 0;
            listOffset = 0;
        });
        addRenderableWidget(search);
        addRenderableWidget(new FordixButton(left + panelWidth - PADDING - 60, top + 4, 60, 14, Tr.c("common.close"), this::onClose));
        backButton = addRenderableWidget(new FordixButton(left + panelWidth - PADDING - 130, top + 4, 66, 14, Tr.c("wiki.back"), this::goBack));
        categoryButton = addRenderableWidget(new FordixButton(left + PADDING, top + 44, LIST_WIDTH - 4, 14, Component.empty(), this::nextCategory));
    }

    @Override
    public void tick() {
        super.tick();
        if (!query.equals(activeQuery) && ++idleTicks >= SEARCH_DELAY_TICKS) {
            activeQuery = query;
            listOffset = 0;
        }
    }

    private void goBack() {
        String[] previous = history.pollFirst();
        if (previous != null) {
            selectedWiki = previous[0];
            selectedSlug = previous[1];
            detailScroll = 0;
        }
    }

    private void open(String wiki, String slug) {
        if (!selectedSlug.isEmpty()) {
            history.addFirst(new String[]{selectedWiki, selectedSlug});
            while (history.size() > 30) {
                history.removeLast();
            }
        }
        selectedWiki = wiki;
        selectedSlug = slug;
        detailScroll = 0;
    }

    // ---- the list on the left

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        if (!activeQuery.isBlank()) {
            List<WikiAnswers.Hit> hits = ClientWiki.search("", activeQuery.strip());
            if (hits != null) {
                boolean several = ClientWiki.wikis().size() > 1;
                for (WikiAnswers.Hit hit : hits) {
                    WikiAnswers.Info info = ClientWiki.wiki(hit.wiki());
                    rows.add(new Row(hit.wiki(), hit.slug(), hit.title(), several && info != null ? info.title() : ""));
                }
            }
            return rows;
        }
        List<WikiAnswers.Entry> index = ClientWiki.index(wikiId);
        if (index != null) {
            List<WikiAnswers.Entry> kept = new ArrayList<>();
            for (WikiAnswers.Entry entry : index) {
                if (category.isEmpty() || categoryKey(entry).equals(category)) {
                    kept.add(entry);
                }
            }
            String lastLetter = "";
            for (WikiAnswers.Entry entry : kept) {
                String letter = kept.size() > LETTERS_FROM ? letterOf(entry.title()) : "";
                if (!letter.isEmpty() && !letter.equals(lastLetter)) {
                    rows.add(new Row(wikiId, "", letter, "", true));
                    lastLetter = letter;
                }
                rows.add(new Row(wikiId, entry.slug(), entry.title(), ""));
            }
        }
        return rows;
    }

    private static String letterOf(String title) {
        if (title.isEmpty()) {
            return "#";
        }
        char c = Character.toUpperCase(title.charAt(0));
        return Character.isLetter(c) ? Character.toString(c) : "#";
    }

    private static String categoryKey(WikiAnswers.Entry entry) {
        return entry.category().isEmpty() ? "other" : entry.category();
    }

    /** The categories the open wiki's pages are in, the usual ones first; empty when the server doesn't say. */
    private List<String> categories() {
        List<WikiAnswers.Entry> index = ClientWiki.index(wikiId);
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (index != null) {
            for (WikiAnswers.Entry entry : index) {
                counts.merge(categoryKey(entry), 1, Integer::sum);
            }
        }
        if (counts.size() < 2 && !(counts.size() == 1 && !counts.containsKey("other"))) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        for (String known : KNOWN_CATEGORIES) {
            if (counts.containsKey(known)) {
                keys.add(known);
            }
        }
        counts.keySet().stream().filter(key -> !KNOWN_CATEGORIES.contains(key) && !key.equals("other")).sorted().forEach(keys::add);
        if (counts.containsKey("other")) {
            keys.add("other");
        }
        return keys.size() < 2 ? List.of() : keys;
    }

    private static String categoryLabel(String key) {
        if (key.isEmpty()) {
            return Tr.s("wiki.cat.all");
        }
        if (KNOWN_CATEGORIES.contains(key) || key.equals("other")) {
            return Tr.s("wiki.cat." + key);
        }
        String spaced = key.replace('_', ' ').replace('-', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private void nextCategory() {
        List<String> keys = new ArrayList<>(categories());
        keys.add(0, "");
        int at = keys.indexOf(category);
        category = keys.get((at + 1) % keys.size());
        listOffset = 0;
    }

    private boolean showCategories() {
        return activeQuery.isBlank() && !categories().isEmpty();
    }

    private boolean loadingList() {
        return activeQuery.isBlank() ? ClientWiki.index(wikiId) == null : ClientWiki.search("", activeQuery.strip()) == null;
    }

    private int listTop() {
        return top + 44 + (showCategories() ? 18 : 0);
    }

    private int visibleRows() {
        return Math.max(1, (top + panelHeight - PADDING - listTop()) / ROW);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        resolveOpenSlug();
        panel.render(graphics, mouseX, mouseY, partialTick);
        WikiAnswers.Info info = ClientWiki.wiki(wikiId);
        graphics.drawString(this.font, info == null ? this.title.getString() : info.title(), left + PADDING, top + 8, FordixPalette.ACCENT, false);
        backButton.visible = !history.isEmpty();
        categoryButton.visible = showCategories();
        if (categoryButton.visible) {
            categoryButton.setMessage(Component.literal(Tr.s("wiki.category", categoryLabel(category))));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        drawList(graphics, mouseX, mouseY, info);
        int detailX = left + PADDING + LIST_WIDTH + 8;
        graphics.fill(detailX - 5, top + 24, detailX - 4, top + panelHeight - PADDING, FordixPalette.BEVEL_HIGHLIGHT);
        drawDetail(graphics, detailX, mouseX, mouseY, info);
        drawZoom(graphics, mouseX, mouseY);
    }

    private void drawZoom(GuiGraphics graphics, int mouseX, int mouseY) {
        if (zoomImage == null) {
            return;
        }
        ClientWiki.Picture picture = ClientWiki.picture(zoomImage);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        graphics.fill(0, 0, this.width, this.height, 0xD0000000);
        if (picture != null && picture.location() != null && picture.width() > 0) {
            double scale = Math.min((this.width - 40.0) / picture.width(), (this.height - 40.0) / picture.height());
            int w = Math.max(1, (int) Math.round(picture.width() * scale));
            int h = Math.max(1, (int) Math.round(picture.height() * scale));
            int x = (this.width - w) / 2;
            int y = (this.height - h) / 2;
            graphics.blit(picture.location(), x, y, w, h, 0.0F, 0.0F, picture.width(), picture.height(), picture.width(), picture.height());
        }
        graphics.drawCenteredString(this.font, Tr.s("wiki.zoom_close"), this.width / 2, this.height - 14, FordixPalette.TEXT);
        graphics.pose().popPose();
    }

    /** A page asked for by name (an item's) opens once the list tells which page that is. */
    private void resolveOpenSlug() {
        if (openSlug == null) {
            return;
        }
        List<WikiAnswers.Entry> index = ClientWiki.index(wikiId);
        if (index == null) {
            return;
        }
        // The list gives the exact spelling of a page; a name that isn't in it (an old name that redirects) is asked for as is.
        String wanted = openSlug.toLowerCase(Locale.ROOT);
        String slug = openSlug;
        for (WikiAnswers.Entry entry : index) {
            if (entry.slug().toLowerCase(Locale.ROOT).equals(wanted)) {
                slug = entry.slug();
                break;
            }
        }
        open(wikiId, slug);
        openSlug = null;
    }

    private void drawList(GuiGraphics graphics, int mouseX, int mouseY, WikiAnswers.Info info) {
        List<Row> rows = rows();
        int x = left + PADDING;
        int y = listTop();
        if (rows.isEmpty()) {
            String message = loadingList() ? Tr.s("wiki.loading") : info == null && ClientWiki.wikis().isEmpty() ? Tr.s("wiki.unknown") : Tr.s("wiki.no_result");
            graphics.drawString(this.font, message, x + 4, y + 4, FordixPalette.TEXT, false);
            return;
        }
        int shown = 0;
        for (int i = listOffset; i < rows.size() && shown < visibleRows(); i++, shown++) {
            Row row = rows.get(i);
            if (row.header()) {
                graphics.drawString(this.font, row.title(), x + 6, y + 5, FordixPalette.ACCENT, false);
                graphics.fill(x + 6 + this.font.width(row.title()) + 4, y + 9, x + LIST_WIDTH - 4, y + 10, FordixPalette.BEVEL_HIGHLIGHT);
                y += ROW;
                continue;
            }
            boolean selected = row.wiki().equals(selectedWiki) && row.slug().equals(selectedSlug);
            boolean hover = mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= y && mouseY < y + ROW;
            if (selected) {
                graphics.fill(x, y, x + LIST_WIDTH, y + ROW - 1, FordixPalette.BUTTON_FILL_HOVERED);
                graphics.fill(x, y, x + 2, y + ROW - 1, FordixPalette.ACCENT);
            } else if (hover) {
                graphics.fill(x, y, x + LIST_WIDTH, y + ROW - 1, FordixPalette.BUTTON_FILL);
            }
            String label = row.detail().isEmpty() ? row.title() : row.title() + " · " + row.detail();
            graphics.drawString(this.font, this.font.plainSubstrByWidth(label, LIST_WIDTH - 10), x + 6, y + 5,
                    selected ? FordixPalette.TEXT_BRIGHT : FordixPalette.TEXT, false);
            y += ROW;
        }
    }

    private void drawDetail(GuiGraphics graphics, int x, int mouseX, int mouseY, WikiAnswers.Info info) {
        int right = left + panelWidth - PADDING;
        int y = top + 26;
        homeLinks.clear();
        if (selectedSlug.isEmpty()) {
            if (info != null) {
                graphics.drawString(this.font, info.title(), x, y, FordixPalette.TEXT_BRIGHT, false);
                for (FormattedCharSequence line : this.font.split(Component.literal(Tr.s("wiki.home", info.pages(), info.license().isEmpty() ? "?" : info.license())), right - x)) {
                    y += 12;
                    graphics.drawString(this.font, line, x, y + 4, FordixPalette.TEXT, false);
                }
                y += 24;
                y = drawRefs(graphics, x, y, right, mouseX, mouseY, Tr.s("wiki.favorites"), memory().favorites());
                drawRefs(graphics, x, y + 4, right, mouseX, mouseY, Tr.s("wiki.recent"), memory().recent());
            }
            return;
        }
        WikiAnswers.Page page = ClientWiki.page(selectedWiki, selectedSlug);
        if (page == null) {
            graphics.drawString(this.font, Tr.s("wiki.loading"), x, y, FordixPalette.TEXT, false);
            return;
        }
        if (ClientWiki.isMissing(page)) {
            graphics.drawString(this.font, Tr.s("wiki.missing"), x, y, FordixPalette.TEXT, false);
            return;
        }
        if (page != shownPage) {
            shownPage = page;
            pageView.setBlocks(PageMarkup.parse(page.body()));
        }
        if (page != visitedPage) {
            visitedPage = page;
            memory().visited(selectedWiki, selectedSlug, page.title());
        }
        boolean favorite = memory().isFavorite(selectedWiki, selectedSlug);
        graphics.drawString(this.font, favorite ? "\u2605" : "\u2606", right - 10, y, favorite ? FordixPalette.ACCENT : FordixPalette.TEXT, false);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(page.title(), right - x - 14), x, y, FordixPalette.TEXT_BRIGHT, false);
        String meta = Tr.s("wiki.source", host(page.source()), page.license().isEmpty() ? "?" : page.license());
        graphics.drawString(this.font, this.font.plainSubstrByWidth(meta, right - x), x, y + 11, FordixPalette.TEXT, false);
        int viewTop = y + 24;
        pageView.render(graphics, x, viewTop, right - x, top + panelHeight - PADDING - viewTop, detailScroll, mouseX, mouseY);
    }

    /** A titled list of remembered pages on the home page; returns the y under it. */
    private int drawRefs(GuiGraphics graphics, int x, int y, int right, int mouseX, int mouseY, String title, List<WikiMemory.Ref> refs) {
        List<WikiMemory.Ref> shown = refs.stream().filter(ref -> ClientWiki.wiki(ref.wiki()) != null).limit(8).toList();
        if (shown.isEmpty()) {
            return y;
        }
        graphics.drawString(this.font, title, x, y, FordixPalette.ACCENT, false);
        y += 12;
        for (WikiMemory.Ref ref : shown) {
            boolean hover = mouseX >= x && mouseX < right && mouseY >= y && mouseY < y + 11;
            if (hover) {
                graphics.fill(x, y, x + 2, y + 10, FordixPalette.ACCENT);
            }
            graphics.drawString(this.font, this.font.plainSubstrByWidth(ref.title(), right - x - 8), x + 5, y + 1,
                    hover ? FordixPalette.TEXT_BRIGHT : FordixPalette.TEXT, false);
            homeLinks.add(new HomeLink(x, y, right - x, ref));
            y += 11;
        }
        return y + 4;
    }

    private static String host(String url) {
        try {
            String host = java.net.URI.create(url).getHost();
            return host == null ? url : host;
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    // ---- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (zoomImage != null) {
            zoomImage = null;
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            for (HomeLink link : homeLinks) {
                if (mouseX >= link.x() && mouseX < link.x() + link.width() && mouseY >= link.y() && mouseY < link.y() + 11) {
                    open(link.ref().wiki(), link.ref().slug());
                    return true;
                }
            }
        }
        int listX = left + PADDING;
        if (button == 0 && mouseX >= listX && mouseX < listX + LIST_WIDTH && mouseY >= listTop()) {
            List<Row> rows = rows();
            int index = listOffset + (int) ((mouseY - listTop()) / ROW);
            if (index >= 0 && index < rows.size() && index < listOffset + visibleRows()) {
                if (!rows.get(index).header()) {
                    open(rows.get(index).wiki(), rows.get(index).slug());
                }
                return true;
            }
        }
        if (button == 0 && shownPage != null && !selectedSlug.isEmpty()) {
            int detailX = left + PADDING + LIST_WIDTH + 8;
            int right = left + panelWidth - PADDING;
            int viewTop = top + 26 + 24;
            int viewHeight = top + panelHeight - PADDING - viewTop;
            if (mouseX >= right - 12 && mouseX < right && mouseY >= top + 26 && mouseY < top + 26 + 11) {
                WikiAnswers.Page current = ClientWiki.page(selectedWiki, selectedSlug);
                if (current != null) {
                    memory().toggleFavorite(selectedWiki, selectedSlug, current.title());
                }
                return true;
            }
            int target = pageView.contentsClick(detailX, viewTop, right - detailX, viewHeight, detailScroll, mouseX, mouseY);
            if (target >= 0) {
                int max = Math.max(0, pageView.contentHeight(right - detailX) - viewHeight);
                detailScroll = Math.max(0, Math.min(max, target));
                return true;
            }
            if (pageView.toggleAt(detailX, viewTop, right - detailX, viewHeight, detailScroll, mouseX, mouseY)) {
                int max = Math.max(0, pageView.contentHeight(right - detailX) - viewHeight);
                detailScroll = Math.max(0, Math.min(max, detailScroll));
                return true;
            }
            PageMarkup.Block picture = pageView.imageAt(detailX, viewTop, right - detailX, viewHeight, detailScroll, mouseX, mouseY);
            if (picture != null) {
                zoomImage = picture.action();
                return true;
            }
            PageMarkup.Block block = pageView.click(detailX, viewTop, right - detailX, top + panelHeight - PADDING - viewTop, detailScroll, mouseX, mouseY);
            if (block != null && block.type() == PageMarkup.Type.LINK) {
                open(selectedWiki, block.action());
                return true;
            }
            if (block != null && block.type() == PageMarkup.Type.BUTTON) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int detailX = left + PADDING + LIST_WIDTH + 4;
        if (mouseX < detailX) {
            int max = Math.max(0, rows().size() - visibleRows());
            listOffset = Math.max(0, Math.min(max, listOffset - (int) Math.signum(delta) * (hasShiftDown() ? 5 : 1)));
            return true;
        }
        if (shownPage != null) {
            int right = left + panelWidth - PADDING;
            int visible = top + panelHeight - PADDING - (top + 26 + 24);
            int max = Math.max(0, pageView.contentHeight(right - (detailX + 4)) - visible);
            detailScroll = Math.max(0, Math.min(max, detailScroll - delta * 18));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (zoomImage != null && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            zoomImage = null;
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !search.isFocused() && !history.isEmpty()) {
            goBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Dev test hooks: the open page becomes a favourite, a category is picked, a section of the page is folded. */
    public void testFavorite() {
        WikiAnswers.Page current = ClientWiki.page(selectedWiki, selectedSlug);
        if (current != null) {
            memory().toggleFavorite(selectedWiki, selectedSlug, current.title());
        }
    }

    public void testCategory(String key) {
        category = key;
        listOffset = 0;
    }

    public void testFold(int heading) {
        pageView.toggleHeadingNumber(heading);
    }

    public void testScroll(double scroll) {
        detailScroll = scroll;
    }

    public void testHome() {
        selectedSlug = "";
    }

    /** Opens the page on the web, after vanilla's confirmation. */
    public void openOnWeb() {
        WikiAnswers.Page page = ClientWiki.page(selectedWiki, selectedSlug);
        if (page != null && page.source().startsWith("http")) {
            Screen previous = this.minecraft.screen;
            this.minecraft.setScreen(new ConfirmLinkScreen(accepted -> {
                if (accepted) {
                    Util.getPlatform().openUri(page.source());
                }
                this.minecraft.setScreen(previous);
            }, page.source(), true));
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
