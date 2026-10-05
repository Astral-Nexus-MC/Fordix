package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.wiki.ClientWiki;
import com.omneria.fordix.guide.PageMarkup;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws and hit-tests a page written in {@link PageMarkup}: headings, wrapped paragraphs, bullets, pictures (fetched from the
 * server as they scroll into the layout) and buttons or links. One instance shows one page at a time in a column; the layout
 * is recomputed when the width changes or a picture arrives. Used by the guide screen (server pages) and the wiki screen.
 */
public final class PageView {
    /** The headline of the "pages this one links to" section: replaced by its translation. */
    public static final String RELATED = "§related";

    private static final int BULLET_INDENT = 9;
    private static final int PICTURE_PLACEHOLDER = 36;
    private static final int MAX_PICTURE_HEIGHT = 120;

    private record Placed(int y, int height, PageMarkup.Block block, List<FormattedCharSequence> lines, int pictureWidth, int pictureHeight) {
    }

    private final Font font;
    private List<PageMarkup.Block> blocks = List.of();
    private List<Placed> placed = List.of();
    private int laidWidth = -1;
    private int laidVersion = -1;
    private int contentHeight;

    public PageView(Font font) {
        this.font = font;
    }

    public void setBlocks(List<PageMarkup.Block> blocks) {
        this.blocks = blocks;
        this.laidWidth = -1;
    }

    public int contentHeight(int width) {
        layout(width);
        return contentHeight;
    }

    private void layout(int width) {
        if (width == laidWidth && laidVersion == ClientWiki.version()) {
            return;
        }
        laidWidth = width;
        laidVersion = ClientWiki.version();
        List<Placed> list = new ArrayList<>();
        int y = 0;
        for (PageMarkup.Block block : blocks) {
            switch (block.type()) {
                case HEADING -> {
                    y += list.isEmpty() ? 0 : 6;
                    list.add(new Placed(y, 12, block, List.of(), 0, 0));
                    y += 14;
                }
                case TEXT -> {
                    List<FormattedCharSequence> lines = font.split(Component.literal(block.text()), width);
                    list.add(new Placed(y, lines.size() * 10, block, lines, 0, 0));
                    y += lines.size() * 10 + 4;
                }
                case BULLET -> {
                    List<FormattedCharSequence> lines = font.split(Component.literal(block.text()), width - BULLET_INDENT);
                    list.add(new Placed(y, lines.size() * 10, block, lines, 0, 0));
                    y += lines.size() * 10 + 2;
                }
                case BUTTON, LINK -> {
                    list.add(new Placed(y, 16, block, List.of(), 0, 0));
                    y += 20;
                }
                case IMAGE -> {
                    ClientWiki.Picture picture = ClientWiki.picture(block.action());
                    int w = 0;
                    int h = PICTURE_PLACEHOLDER;
                    if (picture != null && picture.location() != null && picture.width() > 0) {
                        double scale = Math.min(1.0, Math.min((double) width / picture.width(), (double) MAX_PICTURE_HEIGHT / picture.height()));
                        w = Math.max(1, (int) Math.round(picture.width() * scale));
                        h = Math.max(1, (int) Math.round(picture.height() * scale));
                    } else if (picture != null) {
                        h = 0; // a picture the server doesn't have takes no room
                    }
                    List<FormattedCharSequence> caption = block.text().isEmpty() || h == 0 ? List.of() : font.split(Component.literal(block.text()), width);
                    int total = h + (caption.isEmpty() ? 0 : caption.size() * 10 + 2);
                    list.add(new Placed(y, total, block, caption, w, h));
                    y += total + (total == 0 ? 0 : 6);
                }
            }
        }
        placed = list;
        contentHeight = y;
    }

    /** Draws the page in the rectangle, scrolled by {@code scroll} pixels. */
    public void render(GuiGraphics graphics, int x, int top, int width, int height, double scroll, int mouseX, int mouseY) {
        layout(width);
        graphics.enableScissor(x, top, x + width, top + height);
        int base = top - (int) scroll;
        for (Placed item : placed) {
            int y = base + item.y();
            if (y + item.height() < top || y > top + height) {
                continue;
            }
            switch (item.block().type()) {
                case HEADING -> {
                    String text = item.block().text().equals(RELATED) ? Tr.s("wiki.related") : item.block().text();
                    int level = item.block().action().isEmpty() ? 1 : Integer.parseInt(item.block().action());
                    graphics.drawString(font, text, x, y, level == 1 ? FordixPalette.ACCENT : level == 2 ? FordixPalette.TEXT_BRIGHT : FordixPalette.TEXT, false);
                    if (level == 1) {
                        graphics.fill(x, y + 10, x + Math.min(width, font.width(text)), y + 11, FordixPalette.BEVEL_HIGHLIGHT);
                    }
                }
                case TEXT -> {
                    int ly = y;
                    for (FormattedCharSequence line : item.lines()) {
                        graphics.drawString(font, line, x, ly, FordixPalette.TEXT, false);
                        ly += 10;
                    }
                }
                case BULLET -> {
                    graphics.drawString(font, "•", x + 1, y, FordixPalette.ACCENT, false);
                    int ly = y;
                    for (FormattedCharSequence line : item.lines()) {
                        graphics.drawString(font, line, x + BULLET_INDENT, ly, FordixPalette.TEXT, false);
                        ly += 10;
                    }
                }
                case BUTTON, LINK -> drawButton(graphics, x, y, buttonWidth(item.block(), width), item.block().text(), mouseX, mouseY, item.block().type() == PageMarkup.Type.LINK);
                case IMAGE -> drawPicture(graphics, x, y, item);
            }
        }
        graphics.disableScissor();
    }

    private void drawPicture(GuiGraphics graphics, int x, int y, Placed item) {
        ClientWiki.Picture picture = ClientWiki.picture(item.block().action());
        if (picture == null) {
            graphics.fill(x, y, x + 60, y + item.pictureHeight(), FordixPalette.BUTTON_FILL);
            graphics.drawString(font, Tr.s("wiki.loading"), x + 6, y + item.pictureHeight() / 2 - 4, FordixPalette.TEXT, false);
            return;
        }
        if (picture.location() == null || item.pictureWidth() == 0) {
            return;
        }
        graphics.blit(picture.location(), x, y, item.pictureWidth(), item.pictureHeight(), 0.0F, 0.0F, picture.width(), picture.height(),
                picture.width(), picture.height());
        int ly = y + item.pictureHeight() + 2;
        for (FormattedCharSequence line : item.lines()) {
            graphics.drawString(font, line, x, ly, FordixPalette.TEXT, false);
            ly += 10;
        }
    }

    private int buttonWidth(PageMarkup.Block block, int width) {
        return Math.min(width, font.width(block.text()) + 24);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, String label, int mouseX, int mouseY, boolean link) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 16;
        graphics.fill(x, y, x + w, y + 16, hover ? FordixPalette.BUTTON_FILL_HOVERED : FordixPalette.BUTTON_FILL);
        graphics.fill(x, y + 15, x + w, y + 16, hover ? FordixPalette.ACCENT : FordixPalette.BEVEL_SHADOW);
        if (link) {
            graphics.fill(x, y, x + 2, y + 16, FordixPalette.ACCENT);
        }
        graphics.drawCenteredString(font, font.plainSubstrByWidth(label, w - 8), x + w / 2, y + 4, FordixPalette.TEXT_BRIGHT);
    }

    /** The button or link under the mouse, or null. */
    public PageMarkup.Block click(int x, int top, int width, int height, double scroll, double mouseX, double mouseY) {
        layout(width);
        if (mouseY < top || mouseY >= top + height) {
            return null;
        }
        int base = top - (int) scroll;
        for (Placed item : placed) {
            PageMarkup.Type type = item.block().type();
            if (type != PageMarkup.Type.BUTTON && type != PageMarkup.Type.LINK) {
                continue;
            }
            int y = base + item.y();
            if (mouseX >= x && mouseX < x + buttonWidth(item.block(), width) && mouseY >= y && mouseY < y + 16) {
                return item.block();
            }
        }
        return null;
    }
}
