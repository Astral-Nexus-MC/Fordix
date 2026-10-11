package com.omneria.fordix.client.gui.options;

import com.mojang.blaze3d.platform.InputConstants;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.FordixClient;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.widget.FieldBox;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.SegmentedControl;
import com.omneria.fordix.client.hud.HudTokens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.settings.KeyModifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Options > Controls in Voidframe 2: every key binding of the game and of the mods in one list under its category, a search field
 * (Ctrl + F), the filters All, Conflicts and Changed, a change marked with an accent edge and a button back to the default, a
 * conflict in amber that names the other category, and a key chip that waits for the next key. The bindings are the game's
 * own {@link KeyMapping}s, written to the game's options file like the vanilla screen does.
 */
public final class KeyControlsScreen extends OptionsPageScreen {
    private static final int FLAME = 0xFFE8A64A;

    private final List<KeyRow> keyRows = new ArrayList<>();
    private final Map<String, List<KeyRow>> groups = new LinkedHashMap<>();
    private final Map<String, Heading> headings = new LinkedHashMap<>();
    private KeyMapping selected;
    private FieldBox search;
    private String query = "";
    private int filter;
    private long lastAssigned;

    public KeyControlsScreen(Screen parent) {
        super(Component.translatable("controls.title"), parent);
    }

    @Override
    protected int listOffset() {
        return 44;
    }

    @Override
    protected int rowHeight() {
        return small ? 13 : 15;
    }

    @Override
    protected int rowGap() {
        return 1;
    }

    private static boolean conflicts(KeyMapping mapping) {
        return conflictWith(mapping) != null;
    }

    private static KeyMapping conflictWith(KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return null;
        }
        for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
            if (other != mapping && mapping.same(other)) {
                return other;
            }
        }
        return null;
    }

    @Override
    protected void buildHeader() {
        int count = 0;
        int changed = 0;
        for (KeyMapping mapping : this.minecraft.options.keyMappings) {
            if (conflicts(mapping)) {
                count++;
            }
            if (!mapping.isDefault()) {
                changed++;
            }
        }
        int right = panelX + panelW - 12;
        addRenderableWidget(new FordixButton(right - 78, panelY + 6, 78, 16, Tr.c("options.controls.mouse"),
                () -> this.minecraft.setScreen(new net.minecraft.client.gui.screens.MouseSettingsScreen(this, this.minecraft.options))));
        int searchWidth = panelW - 24;
        if (!small) {
            SegmentedControl filters = new SegmentedControl(0, panelY + 24, 16, List.of(Tr.s("options.controls.all"),
                    Tr.s("options.controls.conflicts", count), Tr.s("options.controls.changed", changed)), () -> filter, index -> {
                        filter = index;
                        relayout();
                    });
            filters.setX(right - filters.getWidth());
            searchWidth = right - filters.getWidth() - 8 - (panelX + 12);
            addRenderableWidget(filters);
        }
        search = new FieldBox(this.font, panelX + 12, panelY + 24, searchWidth, 16, Tr.c("options.controls.search"));
        search.setHint(Tr.c("options.controls.search"));
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            relayout();
        });
        addRenderableWidget(search);
    }

    @Override
    protected int buildRows(int left, int width) {
        keyRows.clear();
        groups.clear();
        headings.clear();
        KeyMapping[] all = Arrays.copyOf(this.minecraft.options.keyMappings, this.minecraft.options.keyMappings.length);
        Arrays.sort(all);
        for (KeyMapping mapping : all) {
            String category = Component.translatable(mapping.getCategory()).getString();
            KeyRow row = new KeyRow(left, listTop, width, rowHeight(), mapping);
            keyRows.add(row);
            groups.computeIfAbsent(category, key -> new ArrayList<>()).add(row);
            addRow(row, null);
        }
        for (String category : groups.keySet()) {
            headings.put(category, addRow(new Heading(left, listTop, width, category), null));
        }
        return relayout();
    }

    @Override
    protected void onDefaults() {
        for (KeyMapping mapping : this.minecraft.options.keyMappings) {
            mapping.setToDefault();
            this.minecraft.options.setKey(mapping, mapping.getDefaultKey());
        }
        KeyMapping.resetMapping();
    }

    @Override
    protected String defaultsLabel() {
        return Tr.s("options.controls.reset_all");
    }

    private boolean matches(KeyRow row) {
        KeyMapping mapping = row.mapping;
        if (filter == 1 && !conflicts(mapping)) {
            return false;
        }
        if (filter == 2 && mapping.isDefault()) {
            return false;
        }
        if (query.isBlank()) {
            return true;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return Component.translatable(mapping.getName()).getString().toLowerCase(Locale.ROOT).contains(needle)
                || mapping.getTranslatedKeyMessage().getString().toLowerCase(Locale.ROOT).contains(needle);
    }

    /** Re-flows the list for the filter and the search: a group with no match loses its heading. */
    private int relayout() {
        int y = listTop + 2;
        for (Map.Entry<String, List<KeyRow>> group : groups.entrySet()) {
            List<KeyRow> shown = new ArrayList<>();
            for (KeyRow row : group.getValue()) {
                boolean show = matches(row);
                hideRow(row, !show);
                if (show) {
                    shown.add(row);
                }
            }
            Heading heading = headings.get(group.getKey());
            if (heading != null) {
                hideRow(heading, shown.isEmpty());
                if (!shown.isEmpty()) {
                    setRowBase(heading, y);
                    y += 13;
                }
            }
            for (KeyRow row : shown) {
                setRowBase(row, y);
                y += rowHeight() + rowGap();
            }
            if (!shown.isEmpty()) {
                y += 4;
            }
        }
        rowsMoved(y);
        return y;
    }

    // ----- capturing the next key -----

    private void assign(KeyMapping mapping, KeyModifier modifier, InputConstants.Key key) {
        mapping.setKeyModifierAndCode(modifier, key);
        this.minecraft.options.setKey(mapping, key);
        KeyMapping.resetMapping();
        lastAssigned = net.minecraft.Util.getMillis();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selected != null) {
            if (keyCode == 256) {
                assign(selected, KeyModifier.NONE, InputConstants.UNKNOWN);
                selected = null;
            } else {
                InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
                assign(selected, KeyModifier.getActiveModifier(), key);
                // A modifier key alone stays the choice until it is let go or another key joins it.
                if (!KeyModifier.isKeyCodeModifier(key)) {
                    selected = null;
                }
            }
            return true;
        }
        if (FordixClient.pressed(FordixClient.SEARCH_FIELD, keyCode, scanCode) && search != null) {
            setFocused(search);
            search.setFocused(true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (selected != null && KeyModifier.isKeyCodeModifier(InputConstants.getKey(keyCode, scanCode))) {
            selected = null;
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selected != null) {
            assign(selected, KeyModifier.getActiveModifier(), InputConstants.Type.MOUSE.getOrCreate(button));
            selected = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (selected != null) {
            selected = null;
            return;
        }
        super.onClose();
    }

    @Override
    protected void renderHeader(GuiGraphics g, int x, int y, int right) {
        if (selected != null) {
            String hint = Tr.s("options.controls.waiting", Component.translatable(selected.getName()).getString());
            g.drawString(this.font, this.font.plainSubstrByWidth(hint, Math.max(0, right - 84 - x)), x, y, FordixPalette.ACCENT, false);
        }
    }

    /** One binding: its name, an amber note when another binding has the same key, the key chip and a button back to the default. */
    private final class KeyRow extends AbstractWidget {
        private final KeyMapping mapping;

        KeyRow(int x, int y, int width, int height, KeyMapping mapping) {
            super(x, y, width, height, Component.translatable(mapping.getName()));
            this.mapping = mapping;
        }

        private int chipWidth(Font font, String text) {
            return Math.max(60, font.width(text) + 12);
        }

        private String chipText() {
            return selected == mapping ? Tr.s("options.controls.press") : mapping.getTranslatedKeyMessage().getString();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            boolean waiting = selected == mapping;
            Glass.row(g, x, y, w, h, waiting, isHoveredOrFocused());
            String chip = chipText();
            int chipW = chipWidth(font, chip);
            int chipX = x + w - 18 - chipW;
            KeyMapping other = conflictWith(mapping);
            boolean changed = !mapping.isDefault();
            int edge = waiting ? FordixPalette.ACCENT : other != null ? FLAME : changed ? HudTokens.withAlpha(FordixPalette.ACCENT, 0x80) : 0x24FFFFFF;
            int ink = waiting ? FordixPalette.ACCENT : other != null ? FLAME : FordixPalette.TEXT_BRIGHT;
            int top = y + (h - 13) / 2;
            g.fill(chipX, top, chipX + chipW, top + 13, other != null ? 0x24E8A64A : 0x78000000);
            g.renderOutline(chipX, top, chipW, 13, edge);
            g.drawString(font, chip, chipX + (chipW - font.width(chip)) / 2, top + 3, ink, false);
            int right = chipX - 6;
            if (other != null && !waiting && !small) {
                String note = Tr.s("options.controls.conflict", Component.translatable(other.getCategory()).getString());
                note = font.plainSubstrByWidth(note, 110);
                int pill = font.width(note) + 10;
                Glass.pill(g, font, note, right - pill, y + (h - 11) / 2, FLAME);
                right -= pill + 6;
            }
            g.drawString(font, font.plainSubstrByWidth(getMessage().getString(), Math.max(0, right - x - 10)), x + 6, y + (h - 8) / 2,
                    FordixPalette.TEXT_BRIGHT, false);
            if (changed) {
                boolean hotReset = mouseX >= x + w - 16 && mouseX < x + w && mouseY >= y && mouseY < y + h;
                g.drawString(font, "↺", x + w - 13, y + (h - 8) / 2, hotReset ? FordixPalette.ACCENT : Glass.dim(), false);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (!mapping.isDefault() && mouseX >= getX() + getWidth() - 16) {
                mapping.setToDefault();
                Minecraft.getInstance().options.setKey(mapping, mapping.getDefaultKey());
                KeyMapping.resetMapping();
                return;
            }
            // A click that only ended a capture a moment ago must not start the next one.
            if (net.minecraft.Util.getMillis() - lastAssigned > 150) {
                selected = selected == mapping ? null : mapping;
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    /** True when the screen is the game's own controls screen and none of the mods that replace it is installed. */
    public static boolean replaces(Screen next) {
        return next.getClass() == ControlsScreen.class && !net.minecraftforge.fml.ModList.get().isLoaded("controlling");
    }
}
