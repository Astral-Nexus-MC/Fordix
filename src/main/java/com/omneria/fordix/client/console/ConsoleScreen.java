package com.omneria.fordix.client.console;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.FordixScreen;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.console.ConsoleData;
import com.omneria.fordix.console.ConsoleTrack;
import com.omneria.fordix.console.ServerboundConsolePayload;
import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A levelled-settings console in Voidframe: one row per setting with its level on a track from -max to +max, the effect it has now,
 * and two buttons showing the item that lowers it and the item that raises it, with how many the player carries. A click asks the
 * server for one step (the mod behind the console decides and answers with the refreshed data).
 */
public class ConsoleScreen extends FordixScreen {
    private static final int MAX_WIDTH = 440;
    private static final int ROW = 28;
    private static final int PADDING = 12;
    private static final int GOOD = 0xFF6FE08A;
    private static final int BAD = 0xFFF06A5C;
    private static final int CELL = 8;

    private ConsoleData data;
    private Panel panel;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private boolean wide;

    public ConsoleScreen(ConsoleData data) {
        super(data.title());
        this.data = data;
    }

    public boolean isFor(ConsoleData other) {
        return data.kind().equals(other.kind()) && data.pos().equals(other.pos());
    }

    public void update(ConsoleData next) {
        this.data = next;
        rebuildWidgets();
    }

    @Override
    protected int minWidth() {
        return 320;
    }

    @Override
    protected int minHeight() {
        return 240;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_WIDTH, this.width - 10);
        wide = panelWidth >= 400;
        panelHeight = Math.min(46 + data.rows().size() * ROW + 26, this.height - 10);
        left = centerX(panelWidth);
        top = centerY(panelHeight);
        panel = new Panel(left, top, panelWidth, panelHeight);
        addRenderableWidget(new FordixButton(left + panelWidth - PADDING - 60, top + 6, 60, 14, Tr.c("common.close"), this::onClose));
        int buttonWidth = wide ? 66 : 54;
        for (int i = 0; i < data.rows().size(); i++) {
            ConsoleData.Row row = data.rows().get(i);
            int y = rowY(i);
            int right = left + panelWidth - PADDING;
            addRenderableWidget(new SlotButton(right - buttonWidth * 2 - 4, y + 4, buttonWidth, 20, row, row.malus(), false));
            addRenderableWidget(new SlotButton(right - buttonWidth, y + 4, buttonWidth, 20, row, row.bonus(), true));
        }
    }

    private int rowY(int index) {
        return top + 40 + index * ROW;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        panel.render(g, mouseX, mouseY, partialTick);
        Glass.title(g, this.font, this.title.getString(), left + PADDING, top + 9);
        g.drawString(this.font, this.font.plainSubstrByWidth(data.subtitle().getString(), panelWidth - PADDING * 2 - 140), left + PADDING, top + 23, Glass.dim(), false);
        if (!data.pill().getString().isEmpty()) {
            String pill = data.pill().getString();
            Glass.pill(g, this.font, pill, left + panelWidth - PADDING - 60 - 8 - (this.font.width(pill) + 10), top + 8, GOOD);
        }
        int buttonsWidth = (wide ? 66 : 54) * 2 + 4;
        int trackX = left + PADDING + (wide ? 150 : 84);
        for (int i = 0; i < data.rows().size(); i++) {
            ConsoleData.Row row = data.rows().get(i);
            int y = rowY(i);
            g.fill(left + PADDING, y, left + panelWidth - PADDING, y + 1, 0x16FFFFFF);
            int nameWidth = trackX - (left + PADDING) - 8;
            g.drawString(this.font, this.font.plainSubstrByWidth(row.name().getString(), nameWidth), left + PADDING, y + 5, FordixPalette.TEXT_BRIGHT, false);
            if (wide) {
                g.drawString(this.font, this.font.plainSubstrByWidth(row.effect().getString(), nameWidth), left + PADDING, y + 16, Glass.dim(), false);
            }
            drawTrack(g, trackX, y + 5, row.level());
            int level = row.level();
            String signed = ConsoleTrack.signed(level);
            int levelColor = level > 0 ? GOOD : level < 0 ? BAD : Glass.dim();
            g.drawString(this.font, signed, trackX, y + 16, levelColor, false);
            int valueX = trackX + this.font.width(signed) + 6;
            int valueRight = left + panelWidth - PADDING - buttonsWidth - 6;
            g.drawString(this.font, this.font.plainSubstrByWidth(row.value().getString(), Math.max(0, valueRight - valueX)), valueX, y + 16, FordixPalette.TEXT, false);
        }
        int footY = top + panelHeight - 18;
        g.fill(left + PADDING, footY - 4, left + panelWidth - PADDING, footY - 3, 0x16FFFFFF);
        int chipX = left + PADDING;
        for (Component chip : data.chips()) {
            String text = chip.getString();
            int width = this.font.width(text) + 10;
            if (chipX + width > left + panelWidth - PADDING) {
                break;
            }
            chipX += Glass.pill(g, this.font, text, chipX, footY, FordixPalette.TEXT_BRIGHT) + 4;
        }
        String footer = data.footer().getString();
        if (!footer.isEmpty() && wide) {
            int footerWidth = this.font.width(footer);
            if (chipX + footerWidth + 8 < left + panelWidth - PADDING) {
                g.drawString(this.font, footer, left + panelWidth - PADDING - footerWidth, footY + 2, Glass.dim(), false);
            }
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawTrack(GuiGraphics g, int x, int y, int level) {
        int[] cells = ConsoleTrack.cells(level, data.maxLevel());
        int now = ConsoleTrack.index(level, data.maxLevel());
        for (int i = 0; i < cells.length; i++) {
            int cx = x + i * (CELL + 1);
            int color = cells[i] == ConsoleTrack.UP ? GOOD : cells[i] == ConsoleTrack.DOWN ? BAD : (i == data.maxLevel() ? 0x38FFFFFF : 0x18FFFFFF);
            g.fill(cx, y, cx + CELL, y + 8, color);
            if (i == now) {
                g.renderOutline(cx - 1, y - 1, CELL + 2, 10, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean matches(ItemStack stack, String spec) {
        if (stack.isEmpty() || spec.isEmpty()) {
            return false;
        }
        if (spec.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(spec.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        ResourceLocation id = ResourceLocation.tryParse(spec);
        return id != null && BuiltInRegistries.ITEM.getOptional(id).map(stack::is).orElse(false);
    }

    private static int carried(String spec) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        }
        Inventory inventory = minecraft.player.getInventory();
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (matches(stack, spec)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (matches(stack, spec)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** The item drawn on a button: the item itself, or one of a tag's items in turn. */
    private static ItemStack iconOf(String spec) {
        if (spec.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(spec.substring(1));
            if (tag == null) {
                return ItemStack.EMPTY;
            }
            List<Item> items = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, tag)).forEach(holder -> items.add(holder.value()));
            return items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.get((int) ((System.currentTimeMillis() / 1000L) % items.size())));
        }
        ResourceLocation id = ResourceLocation.tryParse(spec);
        return id == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** A button for the item that lowers or raises a setting: its icon, the sign and how many the player carries. */
    private final class SlotButton extends AbstractWidget {
        private final ConsoleData.Row row;
        private final ConsoleData.Slot slot;
        private final boolean bonus;
        private final Glass.Fade fade = new Glass.Fade();

        SlotButton(int x, int y, int width, int height, ConsoleData.Row row, ConsoleData.Slot slot, boolean bonus) {
            super(x, y, width, height, slot.name());
            this.row = row;
            this.slot = slot;
            this.bonus = bonus;
        }

        private boolean creative() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.player != null && minecraft.player.getAbilities().instabuild;
        }

        private boolean atLimit() {
            return bonus ? row.level() >= data.maxLevel() : row.level() <= -data.maxLevel();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int have = carried(slot.spec());
            boolean usable = !atLimit() && (have > 0 || creative());
            float hover = fade.update(usable && isHoveredOrFocused());
            Glass.button(g, getX(), getY(), getWidth(), getHeight(), Glass.Kind.NORMAL, hover, usable);
            int edge = bonus ? GOOD : BAD;
            g.fill(getX(), getY() + 2, getX() + 1, getY() + getHeight() - 2, (usable ? 0xFF000000 : 0x70000000) | (edge & 0xFFFFFF));
            Font font = Minecraft.getInstance().font;
            ItemStack icon = iconOf(slot.spec());
            if (!icon.isEmpty()) {
                g.renderItem(icon, getX() + 3, getY() + 2);
            }
            String sign = bonus ? "+" : "-";
            g.drawString(font, sign, getX() + 21, getY() + 6, usable ? edge : (Glass.dim()), false);
            String count = creative() ? "∞" : have > 0 ? "x" + have : Tr.s("console.none");
            g.drawString(font, font.plainSubstrByWidth(count, getWidth() - 31), getX() + 29, getY() + 6, usable ? FordixPalette.TEXT_BRIGHT : Glass.dim(), false);
            setTooltip(Tooltip.create(tooltipText(have)));
        }

        private Component tooltipText(int have) {
            Component base = slot.name().copy().append(Component.literal(" (" + (bonus ? "+1 " : "-1 ") + row.name().getString() + ")"));
            if (atLimit()) {
                return base.copy().append(Component.literal("\n")).append(Tr.c("console.limit"));
            }
            if (have <= 0 && !creative()) {
                return base.copy().append(Component.literal("\n")).append(Tr.c("console.missing"));
            }
            return base;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (atLimit() || (carried(slot.spec()) <= 0 && !creative())) {
                return;
            }
            CompoundTag action = new CompoundTag();
            action.putIntArray("pos", new int[] {data.pos().getX(), data.pos().getY(), data.pos().getZ()});
            action.putString("row", row.id());
            action.putBoolean("bonus", bonus);
            PanelNetwork.CHANNEL.sendToServer(new ServerboundConsolePayload(data.kind(), action));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
