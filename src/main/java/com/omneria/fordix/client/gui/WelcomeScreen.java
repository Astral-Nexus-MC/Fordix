package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.guide.GuideActions;
import com.omneria.fordix.client.guide.GuideEntry;
import com.omneria.fordix.client.gui.vanilla.Chevron;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.panel.network.PanelNetwork;
import com.omneria.fordix.welcome.ClientboundWelcomePayload;
import com.omneria.fordix.welcome.ServerboundWelcomePayload;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * The welcome book: a checklist for new players, ticked by the server as they play (advancements,
 * commands, opening the Guides) or by the player for manual steps. A step can carry a button; a reward
 * may be waiting when everything is done. Opened by right-clicking the book item.
 */
public class WelcomeScreen extends FordixScreen {
    private static final int MAX_WIDTH = 380;
    private static final int ROW = 22;
    private static final int PADDING = 12;

    private List<ClientboundWelcomePayload.Step> steps;
    private boolean rewardPending;
    private Panel panel;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public WelcomeScreen(ClientboundWelcomePayload payload) {
        super(Tr.c("welcome.title"));
        this.steps = payload.steps();
        this.rewardPending = payload.rewardPending();
    }

    public void update(ClientboundWelcomePayload payload) {
        this.steps = payload.steps();
        this.rewardPending = payload.rewardPending();
        rebuildWidgets();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_WIDTH, this.width - 10);
        panelHeight = Math.min(62 + steps.size() * ROW + 26, this.height - 10);
        left = centerX(panelWidth);
        top = centerY(panelHeight);
        panel = new Panel(left, top, panelWidth, panelHeight);
        addRenderableWidget(new FordixButton(left + panelWidth - PADDING - 60, top + 6, 60, 14, Tr.c("common.close"), this::onClose));
        for (int i = 0; i < steps.size(); i++) {
            ClientboundWelcomePayload.Step step = steps.get(i);
            int y = rowY(i);
            if (!step.action().isEmpty() && !step.done()) {
                GuideEntry entry = GuideEntry.parse("minecraft | " + step.label() + " | minecraft:book | " + step.action());
                if (entry != null) {
                    addRenderableWidget(new FordixButton(left + panelWidth - PADDING - 70, y + 2, 70, 16,
                            Tr.c(entry.action() == GuideEntry.Action.URL ? "guides.open" : "welcome.run"), () -> GuideActions.run(entry)));
                }
            }
        }
    }

    private int rowY(int index) {
        return top + 50 + index * ROW;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        Glass.title(guiGraphics, this.font, this.title.getString(), left + PADDING, top + 9);
        long done = steps.stream().filter(ClientboundWelcomePayload.Step::done).count();
        float progress = steps.isEmpty() ? 0 : done / (float) steps.size();
        Chevron.draw(guiGraphics, left + PADDING, top + 26, panelWidth - PADDING * 2, 7, true, 3, progress, 0xFF7DFFB0, 0xFF2BB56A, false);
        String summary = Tr.s("welcome.summary", done, steps.size())
                + (done == steps.size() && !steps.isEmpty() ? Tr.s(rewardPending ? "welcome.summary.reward" : "welcome.summary.done") : "");
        guiGraphics.drawString(this.font, summary, left + PADDING, top + 37, FordixPalette.TEXT, false);
        for (int i = 0; i < steps.size(); i++) {
            ClientboundWelcomePayload.Step step = steps.get(i);
            int y = rowY(i);
            boolean hover = step.manual() && mouseX >= left + PADDING && mouseX < left + panelWidth - PADDING
                    && mouseY >= y && mouseY < y + ROW - 2;
            if (hover) {
                Glass.row(guiGraphics, left + PADDING - 2, y, panelWidth - PADDING * 2 + 4, ROW - 2, false, true);
            }
            int box = 12;
            int by = y + 3;
            Glass.well(guiGraphics, left + PADDING, by, box, box);
            if (step.done()) {
                guiGraphics.fill(left + PADDING, by, left + PADDING + box, by + 1, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
                guiGraphics.fill(left + PADDING, by + box - 1, left + PADDING + box, by + box, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
                guiGraphics.fill(left + PADDING, by + 1, left + PADDING + 1, by + box - 1, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
                guiGraphics.fill(left + PADDING + box - 1, by + 1, left + PADDING + box, by + box - 1, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
            }
            if (step.done()) {
                guiGraphics.drawString(this.font, "v", left + PADDING + 3, by + 2, FordixPalette.ACCENT, false);
            }
            int textRight = left + panelWidth - PADDING - (step.action().isEmpty() || step.done() ? 0 : 76);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(step.label(), textRight - (left + PADDING + 20)),
                    left + PADDING + 20, y + 6, step.done() ? FordixPalette.TEXT : FordixPalette.TEXT_BRIGHT, false);
        }
        if (steps.isEmpty()) {
            guiGraphics.drawString(this.font, Tr.s("welcome.none"), left + PADDING, top + 54, FordixPalette.TEXT, false);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            for (int i = 0; i < steps.size(); i++) {
                int y = rowY(i);
                if (steps.get(i).manual() && mouseX >= left + PADDING && mouseX < left + panelWidth - PADDING
                        && mouseY >= y && mouseY < y + ROW - 2) {
                    PanelNetwork.CHANNEL.sendToServer(new ServerboundWelcomePayload("toggle", i));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
