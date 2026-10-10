package com.omneria.fordix.client.gui;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.ToggleRow;
import com.omneria.fordix.client.hud.HudStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * The effects list and the alerts (worn out item, no ammunition, slow server, critical health) with their sound: one
 * Voidframe 2 switch row each, written to the config right away.
 */
public class HudAlertsScreen extends FordixScreen {
    private static final int COLUMN_WIDTH = 200;
    private static final int PADDING = 10;
    private static final int GAP = 10;
    private static final int PANEL_WIDTH = PADDING * 2 + COLUMN_WIDTH * 2 + GAP;
    private static final int ROW_HEIGHT = 28;
    private static final int ROW_GAP = 4;

    private record Option(String icon, int tint, String label, String description, ForgeConfigSpec.BooleanValue value) {
    }

    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelHeight;

    public HudAlertsScreen(Screen parent) {
        super(Tr.c("alerts.title"));
        this.parent = parent;
    }

    @Override
    protected int minWidth() {
        return PANEL_WIDTH + 10;
    }

    @Override
    protected int minHeight() {
        return 240;
    }

    @Override
    protected void init() {
        List<Option> options = new ArrayList<>();
        options.add(new Option("=", 0x5ED68A, Tr.s("options.effects"), Tr.s("alerts.desc.effects"), FordixConfig.HUD_EFFECTS_LIST));
        for (String alert : FordixConfig.HUD_ALERTS) {
            int tint = alert.equals("alert_sound") ? 0x4DA3FF : alert.equals("alert_tps") || alert.equals("alert_critical") ? 0xE5534B : 0xE8A64A;
            options.add(new Option(alert.equals("alert_sound") ? "~" : "!", tint, Tr.s("hud.alert." + alert),
                    Tr.s("alerts.desc." + alert), FordixConfig.hudAlertValue(alert)));
        }
        int rows = (options.size() + 1) / 2;
        panelHeight = 24 + 6 + rows * (ROW_HEIGHT + ROW_GAP) + 4 + 34;
        panelX = centerX(PANEL_WIDTH);
        panelY = centerY(panelHeight);
        int top = panelY + 30;
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            addRenderableWidget(new ToggleRow(panelX + PADDING + (i % 2) * (COLUMN_WIDTH + GAP), top + (i / 2) * (ROW_HEIGHT + ROW_GAP),
                    COLUMN_WIDTH, ROW_HEIGHT, option.icon(), option.tint(), option.label(), option.description(), false, option.value()));
        }
        addRenderableWidget(new FordixButton(panelX + PANEL_WIDTH - PADDING - 100, panelY + panelHeight - 27, 100, 20,
                Tr.c("common.back"), this::onClose).kind(Glass.Kind.PRIMARY));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xF00B1118);
        HudStyle.panel(g, panelX, panelY, PANEL_WIDTH, panelHeight, FordixPalette.ACCENT & 0xFFFFFF, 0xEE);
        g.drawString(this.font, this.title, panelX + PADDING + 4, panelY + 8, FordixPalette.ACCENT, false);
        g.drawString(this.font, Tr.s("settings.tab.hud"), panelX + PANEL_WIDTH - PADDING - this.font.width(Tr.s("settings.tab.hud")) - 4,
                panelY + 8, 0xFF7D93A3, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
