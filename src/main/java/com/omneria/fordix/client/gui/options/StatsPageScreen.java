package com.omneria.fordix.client.gui.options;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.FordixScreen;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.TabButton;
import com.omneria.fordix.client.hud.HudTokens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsUpdateListener;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Statistics in Voidframe 2: three tabs (general, items, creatures), a line per figure that is not zero, scrolled with the wheel. The
 * game sends the figures when asked, so the page asks on opening and rebuilds its lines when they arrive.
 */
public final class StatsPageScreen extends FordixScreen implements StatsUpdateListener {
    private record Line(String name, String value, ItemStack icon) {
    }

    private static final int ROW = 16;

    private final Screen parent;
    private final StatsCounter stats;
    private final List<Line> lines = new ArrayList<>();
    private final List<TabButton> tabs = new ArrayList<>();
    private int tab;
    private double scroll;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listTop;
    private int listBottom;

    public StatsPageScreen(Screen parent, StatsCounter stats) {
        super(Component.translatable("gui.stats"));
        this.parent = parent;
        this.stats = stats;
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
        panelW = Math.min(520, this.width - 10);
        panelH = this.height - 12;
        panelX = centerX(panelW);
        panelY = 6;
        listTop = panelY + 46;
        listBottom = panelY + panelH - 34;
        tabs.clear();
        String[] names = {vanilla("stat.generalButton"), vanilla("stat.itemsButton"), vanilla("stat.mobsButton")};
        int x = panelX + 12;
        for (int i = 0; i < names.length; i++) {
            int index = i;
            int width = this.font.width(names[i]) + 14;
            TabButton button = new TabButton(x, panelY + 24, width, 16, Component.literal(names[i]), null, () -> {
                tab = index;
                scroll = 0;
                rebuild();
            });
            button.setSelected(i == tab);
            tabs.add(button);
            addRenderableWidget(button);
            x += width + 2;
        }
        addRenderableWidget(new FordixButton(panelX + panelW - 12 - 80, panelY + panelH - 26, 80, 18, Tr.c("options.page.done"), this::onClose)
                .kind(Glass.Kind.PRIMARY));
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null) {
            connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        }
        rebuild();
    }

    private static String vanilla(String key) {
        return Component.translatable(key).getString();
    }

    @Override
    public void onStatsUpdated() {
        rebuild();
    }

    private static String part(String key, int value) {
        return value > 0 ? Tr.s(key) + " " + value : "";
    }

    private static String join(String... parts) {
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                out.append(out.length() == 0 ? "" : "  ").append(part);
            }
        }
        return out.toString();
    }

    private static <T> int value(StatsCounter counter, StatType<T> type, T key) {
        return counter.getValue(type.get(key));
    }

    private void rebuild() {
        lines.clear();
        switch (tab) {
            case 0 -> {
                for (ResourceLocation id : BuiltInRegistries.CUSTOM_STAT.keySet()) {
                    Stat<ResourceLocation> stat = Stats.CUSTOM.get(id);
                    int value = stats.getValue(stat);
                    if (value > 0) {
                        lines.add(new Line(vanilla("stat." + id.toString().replace(':', '.')), stat.format(value), ItemStack.EMPTY));
                    }
                }
            }
            case 1 -> {
                for (Item item : BuiltInRegistries.ITEM) {
                    int mined = item instanceof BlockItem block ? value(stats, Stats.BLOCK_MINED, block.getBlock()) : 0;
                    String text = join(part("options.stats.mined", mined), part("options.stats.crafted", value(stats, Stats.ITEM_CRAFTED, item)),
                            part("options.stats.used", value(stats, Stats.ITEM_USED, item)), part("options.stats.broken", value(stats, Stats.ITEM_BROKEN, item)),
                            part("options.stats.picked", value(stats, Stats.ITEM_PICKED_UP, item)), part("options.stats.dropped", value(stats, Stats.ITEM_DROPPED, item)));
                    if (!text.isEmpty()) {
                        ItemStack icon = new ItemStack(item);
                        lines.add(new Line(icon.getHoverName().getString(), text, icon));
                    }
                }
            }
            default -> {
                for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                    String text = join(part("options.stats.killed", value(stats, Stats.ENTITY_KILLED, type)),
                            part("options.stats.killed_by", value(stats, Stats.ENTITY_KILLED_BY, type)));
                    if (!text.isEmpty()) {
                        lines.add(new Line(type.getDescription().getString(), text, ItemStack.EMPTY));
                    }
                }
            }
        }
        lines.sort(Comparator.comparing(line -> line.name().toLowerCase(Locale.ROOT)));
        clamp();
    }

    private int contentHeight() {
        return lines.size() * ROW;
    }

    private void clamp() {
        scroll = Math.max(0, Math.min(Math.max(0, contentHeight() - (listBottom - listTop)), scroll));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        Glass.panel(g, panelX, panelY, panelW, panelH);
        Glass.title(g, this.font, getTitle().getString(), panelX + 14, panelY + 9);
        for (int i = 0; i < tabs.size(); i++) {
            if (i == tab) {
                TabButton button = tabs.get(i);
                g.fill(button.getX() + 3, button.getY() + button.getHeight() - 2, button.getX() + button.getWidth() - 3, button.getY() + button.getHeight(), TabButton.accent());
            }
        }
        g.fill(panelX + 12, listBottom + 2, panelX + panelW - 12, listBottom + 3, 0x20FFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
        g.enableScissor(panelX + 2, listTop, panelX + panelW - 2, listBottom);
        if (lines.isEmpty()) {
            g.drawString(this.font, Tr.s("options.stats.empty"), panelX + 14, listTop + 6, Glass.dim(), false);
        }
        int first = (int) (scroll / ROW);
        for (int i = first; i < lines.size(); i++) {
            int y = listTop + i * ROW - (int) scroll;
            if (y >= listBottom) {
                break;
            }
            Line line = lines.get(i);
            if (i % 2 == 0) {
                g.fill(panelX + 8, y, panelX + panelW - 12, y + ROW, 0x0AFFFFFF);
            }
            int textX = panelX + 14;
            if (!line.icon().isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(panelX + 12, y, 0);
                g.pose().scale(0.75F, 0.75F, 1);
                g.renderItem(line.icon(), 0, 0);
                g.pose().popPose();
                textX += 16;
            }
            int valueWidth = this.font.width(line.value());
            g.drawString(this.font, this.font.plainSubstrByWidth(line.name(), Math.max(0, panelW - 50 - valueWidth - (textX - panelX))), textX, y + 4,
                    FordixPalette.TEXT_BRIGHT, false);
            g.drawString(this.font, line.value(), panelX + panelW - 16 - valueWidth, y + 4, FordixPalette.ACCENT, false);
        }
        g.disableScissor();
        int viewport = listBottom - listTop;
        if (contentHeight() > viewport) {
            int barX = panelX + panelW - 6;
            g.fill(barX, listTop, barX + 3, listBottom, 0x12FFFFFF);
            int size = Math.max(12, viewport * viewport / contentHeight());
            int from = listTop + (int) ((viewport - size) * (scroll / Math.max(1, contentHeight() - viewport)));
            g.fill(barX, from, barX + 3, from + size, HudTokens.withAlpha(FordixPalette.ACCENT, 0xB0));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll -= delta * ROW * 2;
        clamp();
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
