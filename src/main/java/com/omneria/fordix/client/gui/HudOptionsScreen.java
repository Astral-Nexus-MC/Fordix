package com.omneria.fordix.client.gui;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Alerts and look of the HUD: which warnings fire (item about to break, no ammunition, laggy server, with or without a
 * sound), the opacity and density of the information panels, the effects list, the combat layout that loads by itself
 * and whether the compass and the clock need their item. Every switch is written to the config right away.
 */
public class HudOptionsScreen extends FordixScreen {
    private static final int COLUMN_WIDTH = 200;
    private static final int PADDING = 10;
    private static final int GAP = 10;
    private static final int PANEL_WIDTH = PADDING * 2 + COLUMN_WIDTH * 2 + GAP;
    private static final int ROW = 18;
    private static final int BUTTON_HEIGHT = 16;

    private final Screen parent;
    private Panel panel;

    public HudOptionsScreen(Screen parent) {
        super(Tr.c("options.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int rows = 5;
        int panelHeight = 24 + ROW * rows + 4 + BUTTON_HEIGHT + PADDING;
        int panelX = centerX(PANEL_WIDTH);
        int panelY = centerY(panelHeight);
        this.panel = new Panel(panelX, panelY, PANEL_WIDTH, panelHeight);
        int left = panelX + PADDING;
        int right = left + COLUMN_WIDTH + GAP;
        int y = panelY + 24;
        // Left: the four alerts, then the effects list. Right: the look and the automatic behaviours.
        for (int i = 0; i < FordixConfig.HUD_ALERTS.size(); i++) {
            String alert = FordixConfig.HUD_ALERTS.get(i);
            addRenderableWidget(toggle(left, y + ROW * i, Tr.s("hud.alert." + alert), FordixConfig.hudAlertValue(alert)));
        }
        addRenderableWidget(toggle(left, y + ROW * 4, Tr.s("settings.hud_items"), FordixConfig.HUD_REQUIRE_ITEMS));
        addRenderableWidget(toggle(right, y, Tr.s("options.compact"), FordixConfig.HUD_COMPACT));
        addRenderableWidget(opacity(right, y + ROW));
        addRenderableWidget(toggle(right, y + ROW * 2, Tr.s("options.effects"), FordixConfig.HUD_EFFECTS_LIST));
        addRenderableWidget(toggle(right, y + ROW * 3, Tr.s("options.auto_combat"), FordixConfig.AUTO_COMBAT_PROFILE));
        addRenderableWidget(new FordixButton(panelX + PADDING, y + ROW * rows + 4, PANEL_WIDTH - PADDING * 2, BUTTON_HEIGHT,
                Tr.c("common.close"), this::onClose));
    }

    private FordixButton toggle(int x, int y, String label, ForgeConfigSpec.BooleanValue option) {
        FordixButton[] self = new FordixButton[1];
        self[0] = new FordixButton(x, y, COLUMN_WIDTH, BUTTON_HEIGHT, text(label, option.get() ? "ON" : "OFF"), () -> {
            boolean next = !option.get();
            option.set(next);
            option.save();
            self[0].setMessage(text(label, next ? "ON" : "OFF"));
        });
        return self[0];
    }

    private FordixButton opacity(int x, int y) {
        FordixButton[] self = new FordixButton[1];
        String label = Tr.s("options.opacity");
        self[0] = new FordixButton(x, y, COLUMN_WIDTH, BUTTON_HEIGHT, text(label, FordixConfig.HUD_OPACITY.get() + " %"), () -> {
            List<String> values = FordixConfig.HUD_OPACITIES;
            String next = values.get((Math.max(0, values.indexOf(FordixConfig.HUD_OPACITY.get())) + 1) % values.size());
            FordixConfig.HUD_OPACITY.set(next);
            FordixConfig.HUD_OPACITY.save();
            self[0].setMessage(text(label, next + " %"));
        });
        return self[0];
    }

    private static Component text(String label, String value) {
        return Component.literal(label + " : " + value);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panel.getY() + 8, FordixPalette.ACCENT);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
