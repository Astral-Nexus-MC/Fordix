package com.omneria.fordix.client.gui.options;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.widget.FieldBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Options > Language in Voidframe 2: every language of the game in a searchable list, the current one marked, the chosen one lit.
 * The language changes (and the resources reload) when the page is closed, like the game's own screen does on Done.
 */
public final class LanguageScreen extends OptionsPageScreen {
    private final List<LanguageRow> languageRows = new ArrayList<>();
    private FieldBox search;
    private String query = "";
    private String chosen;

    public LanguageScreen(Screen parent) {
        super(Component.translatable("options.language"), parent);
    }

    @Override
    protected int listOffset() {
        return 44;
    }

    @Override
    protected int rowHeight() {
        return 15;
    }

    @Override
    protected int rowGap() {
        return 1;
    }

    @Override
    protected void buildHeader() {
        search = new FieldBox(this.font, panelX + 12, panelY + 24, panelW - 24, 16, Tr.c("options.language.search"));
        search.setHint(Tr.c("options.language.search"));
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            relayout();
        });
        addRenderableWidget(search);
    }

    @Override
    protected int buildRows(int left, int width) {
        languageRows.clear();
        if (chosen == null) {
            chosen = this.minecraft.options.languageCode;
        }
        for (Map.Entry<String, LanguageInfo> entry : this.minecraft.getLanguageManager().getLanguages().entrySet()) {
            LanguageRow row = new LanguageRow(left, listTop, width, rowHeight(), entry.getKey(), entry.getValue().toString());
            languageRows.add(row);
            addRow(row, null);
        }
        return relayout();
    }

    @Override
    protected void onDefaults() {
        chosen = "en_us";
    }

    private int relayout() {
        int y = listTop + 2;
        String needle = query.toLowerCase(Locale.ROOT);
        for (LanguageRow row : languageRows) {
            boolean show = needle.isBlank() || row.label.toLowerCase(Locale.ROOT).contains(needle) || row.code.toLowerCase(Locale.ROOT).contains(needle);
            hideRow(row, !show);
            if (show) {
                setRowBase(row, y);
                y += rowHeight() + rowGap();
            }
        }
        rowsMoved(y);
        return y;
    }

    @Override
    public void onClose() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null && chosen != null && !chosen.equals(minecraft.options.languageCode)) {
            LanguageInfo info = minecraft.getLanguageManager().getLanguages().get(chosen);
            if (info != null) {
                minecraft.getLanguageManager().setSelected(chosen);
                minecraft.options.languageCode = chosen;
                minecraft.reloadResourcePacks();
            }
        }
        super.onClose();
    }

    private final class LanguageRow extends AbstractWidget {
        private final String code;
        private final String label;

        LanguageRow(int x, int y, int width, int height, String code, String label) {
            super(x, y, width, height, Component.literal(label));
            this.code = code;
            this.label = label;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean selected = code.equals(chosen);
            Glass.row(g, getX(), getY(), getWidth(), getHeight(), selected, isHoveredOrFocused());
            g.drawString(font, font.plainSubstrByWidth(label, getWidth() - 60), getX() + 8, getY() + (getHeight() - 8) / 2,
                    selected ? 0xFFFFFFFF : FordixPalette.TEXT_BRIGHT, false);
            if (code.equals(Minecraft.getInstance().options.languageCode)) {
                String current = Tr.s("options.language.current");
                g.drawString(font, current, getX() + getWidth() - 10 - font.width(current), getY() + (getHeight() - 8) / 2, FordixPalette.ACCENT, false);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            chosen = code;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
