package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.wiki.ClientWiki;
import com.omneria.fordix.guide.PageMarkup;
import com.omneria.fordix.guide.PageOutline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private static final int MAX_CONTENTS = 16;
    private static final int CONTENTS_HEADER = 14;
    private static final int CONTENTS_ROW = 11;

    /** Where a heading was laid out (content coordinates), by its block number. */
    private record HeadingSpot(int index, int y) {
    }

    private final Font font;
    private boolean interactive;
    private final Set<Integer> folded = new HashSet<>();
    private List<PageOutline.Heading> contents = List.of();
    private final List<HeadingSpot> headingSpots = new ArrayList<>();
    private List<PageMarkup.Block> blocks = List.of();
    private List<Placed> placed = List.of();
    private int laidWidth = -1;
    private int laidVersion = -1;
    private int contentHeight;

    public PageView(Font font) {
        this.font = font;
    }

    /** Turns on the table of contents and the folding of sections (the wiki screen does; a short guide page doesn't need them). */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        this.laidWidth = -1;
    }

    public void setBlocks(List<PageMarkup.Block> blocks) {
        this.blocks = blocks;
        this.folded.clear();
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
        headingSpots.clear();
        boolean[] hidden = interactive ? PageOutline.hidden(blocks, folded) : new boolean[blocks.size()];
        List<PageOutline.Heading> all = PageOutline.headings(blocks);
        contents = interactive && PageOutline.wantsContents(blocks) ? all.subList(0, Math.min(MAX_CONTENTS, all.size())) : List.of();
        int y = contents.isEmpty() ? 0 : CONTENTS_HEADER + contents.size() * CONTENTS_ROW + 8;
        int first = y;
        for (int index = 0; index < blocks.size(); index++) {
            PageMarkup.Block block = blocks.get(index);
            if (hidden[index]) {
                continue;
            }
            switch (block.type()) {
                case HEADING -> {
                    y += list.isEmpty() || y == first ? 0 : 6;
                    list.add(new Placed(y, 12, block, List.of(), 0, 0));
                    headingSpots.add(new HeadingSpot(index, y));
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
        drawContents(graphics, x, base, width, mouseX, mouseY);
        for (Placed item : placed) {
            int y = base + item.y();
            if (y + item.height() < top || y > top + height) {
                continue;
            }
            switch (item.block().type()) {
                case HEADING -> {
                    String text = item.block().text().equals(RELATED) ? Tr.s("wiki.related") : item.block().text();
                    if (interactive && !item.block().text().equals(RELATED)) {
                        text = (isFolded(item) ? "\u25B6 " : "\u25BC ") + text;
                    }
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

    private boolean isFolded(Placed item) {
        for (HeadingSpot spot : headingSpots) {
            if (spot.y() == item.y() && blocks.get(spot.index()) == item.block()) {
                return folded.contains(spot.index());
            }
        }
        return false;
    }

    private void drawContents(GuiGraphics graphics, int x, int base, int width, int mouseX, int mouseY) {
        if (contents.isEmpty()) {
            return;
        }
        graphics.drawString(font, Tr.s("wiki.contents"), x, base, FordixPalette.ACCENT, false);
        int y = base + CONTENTS_HEADER;
        for (PageOutline.Heading heading : contents) {
            int indent = (heading.level() - 1) * 8;
            boolean hover = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + CONTENTS_ROW;
            String text = font.plainSubstrByWidth(heading.text(), width - indent - 6);
            graphics.drawString(font, text, x + 4 + indent, y + 1, hover ? FordixPalette.TEXT_BRIGHT : FordixPalette.TEXT, false);
            if (hover) {
                graphics.fill(x, y, x + 2, y + CONTENTS_ROW - 1, FordixPalette.ACCENT);
            }
            y += CONTENTS_ROW;
        }
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

    /** Folds or unfolds the n-th heading of the page (dev test). */
    public void toggleHeadingNumber(int number) {
        List<PageOutline.Heading> all = PageOutline.headings(blocks);
        if (number >= 0 && number < all.size()) {
            int block = all.get(number).block();
            if (!folded.remove(block)) {
                folded.add(block);
            }
            laidWidth = -1;
        }
    }

    /** The scroll position of the contents entry under the mouse (it opens the heading's folded section first), or -1. */
    public int contentsClick(int x, int top, int width, int height, double scroll, double mouseX, double mouseY) {
        layout(width);
        if (contents.isEmpty() || mouseY < top || mouseY >= top + height || mouseX < x || mouseX >= x + width) {
            return -1;
        }
        int y = top - (int) scroll + CONTENTS_HEADER;
        for (PageOutline.Heading heading : contents) {
            if (mouseY >= y && mouseY < y + CONTENTS_ROW) {
                if (folded.remove(heading.block())) {
                    laidWidth = -1;
                    layout(width);
                }
                for (HeadingSpot spot : headingSpots) {
                    if (spot.index() == heading.block()) {
                        return spot.y();
                    }
                }
                return -1;
            }
            y += CONTENTS_ROW;
        }
        return -1;
    }

    /** Folds or unfolds the section of the heading under the mouse; true when there was one. */
    public boolean toggleAt(int x, int top, int width, int height, double scroll, double mouseX, double mouseY) {
        layout(width);
        if (!interactive || mouseY < top || mouseY >= top + height || mouseX < x || mouseX >= x + width) {
            return false;
        }
        int base = top - (int) scroll;
        for (HeadingSpot spot : headingSpots) {
            int y = base + spot.y();
            if (mouseY >= y && mouseY < y + 12 && !blocks.get(spot.index()).text().equals(RELATED)) {
                if (!folded.remove(spot.index())) {
                    folded.add(spot.index());
                }
                laidWidth = -1;
                return true;
            }
        }
        return false;
    }

    /** The picture under the mouse, or null. */
    public PageMarkup.Block imageAt(int x, int top, int width, int height, double scroll, double mouseX, double mouseY) {
        layout(width);
        if (mouseY < top || mouseY >= top + height) {
            return null;
        }
        int base = top - (int) scroll;
        for (Placed item : placed) {
            if (item.block().type() == PageMarkup.Type.IMAGE && item.pictureWidth() > 0) {
                int y = base + item.y();
                if (mouseX >= x && mouseX < x + item.pictureWidth() && mouseY >= y && mouseY < y + item.pictureHeight()) {
                    return item.block();
                }
            }
        }
        return null;
    }
}
