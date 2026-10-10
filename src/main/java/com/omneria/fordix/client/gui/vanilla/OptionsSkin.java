package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.widget.FieldBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The options menu in Voidframe 2: a glass panel with the title and the world name, a search field that filters the settings,
 * and the settings as rows (icon, title, current value, chevron) in two columns, then a main "Done" button. The game's own
 * screen and the screens its buttons open are untouched: its buttons are laid out again, drawn as rows, and filtered by the
 * search; the field of view and the difficulty (a slider and a cycle button, which cannot be drawn as rows) are wrapped in a
 * row that forwards the mouse to them.
 */
public final class OptionsSkin {
    private static final int FULL_W = 680;
    private static final int FULL_H = 412;
    private static final Field TITLE = Reflect.uniqueField(Screen.class, Component.class);

    private static final String[] ORDER = {"fov", "difficulty", "options.video", "options.sounds", "options.controls", "options.language",
            "options.resourcepack", "resourcePack", "options.skinCustomisation", "options.accessibility", "options.chat", "options.telemetry",
            "options.credits"};

    /** One entry of the grid: the widget that does the work and its place. */
    private record Entry(AbstractWidget widget, String title, java.util.function.Supplier<String> detail, String key) {
    }

    private static OptionsScreen laidOut;
    private static int panelX;
    private static int panelY;
    private static int panelW;
    private static int panelH;
    private static FieldBox search;
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static String query = "";

    private OptionsSkin() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        ENTRIES.clear();
        laidOut = null;
        if (!FordixConfig.voidframeScreens() || event.getScreen().getClass() != OptionsScreen.class) {
            return;
        }
        OptionsScreen screen = (OptionsScreen) event.getScreen();
        Button done = null;
        List<AbstractWidget> others = new ArrayList<>();
        AbstractWidget lock = null;
        for (GuiEventListener listener : event.getListenersList()) {
            if (!(listener instanceof AbstractWidget widget)) {
                continue;
            }
            String key = keyOf(widget);
            if (widget.getClass() == Button.class && key.equals("gui.done")) {
                done = (Button) widget;
            } else if (widget.getClass() == Button.class) {
                ENTRIES.add(new Entry(widget, titleOf(widget), () -> detailOf(key), key));
            } else if (widget instanceof net.minecraft.client.gui.components.AbstractSliderButton && isOption(widget, "options.fov")) {
                ENTRIES.add(new Entry(widget, Component.translatable("options.fov").getString(), () -> valueOf(widget), "fov"));
            } else if (widget instanceof CycleButton<?> && isOption(widget, "options.difficulty")) {
                ENTRIES.add(new Entry(widget, Component.translatable("options.difficulty").getString(), () -> valueOf(widget), "difficulty"));
            } else if (widget.getClass().getSimpleName().contains("Lock")) {
                lock = widget;
            } else {
                others.add(widget);
            }
        }
        if (ENTRIES.isEmpty() || done == null) {
            return;
        }
        // The game's own order is replaced by the mockup's; anything unknown (another mod's button) goes last.
        ENTRIES.sort(java.util.Comparator.comparingInt(entry -> {
            for (int i = 0; i < ORDER.length; i++) {
                if (entry.key().startsWith(ORDER[i])) {
                    return i;
                }
            }
            return ORDER.length;
        }));
        boolean full = screen.width >= FULL_W + 16 && screen.height >= FULL_H + 16;
        panelW = full ? FULL_W : Math.min(FULL_W, screen.width - 16);
        panelH = full ? FULL_H : Math.min(FULL_H, screen.height - 16);
        panelX = (screen.width - panelW) / 2;
        panelY = (screen.height - panelH) / 2;
        int columnWidth = (panelW - 28 - 8) / 2;
        int rows = (ENTRIES.size() + 1) / 2;
        int available = panelH - 76 - 54;
        int step = Math.min(46, available / Math.max(1, rows));
        // Too little room for two-line rows: single-line rows at button height, the detail line dropped.
        boolean slim = step - 6 < 26;
        if (slim) {
            step = Math.max(16, Math.min(22, (panelH - 58 - 30) / Math.max(1, rows)));
        }
        int height = slim ? step - 2 : step - 6;
        int top = slim ? panelY + 58 : panelY + 76;
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry entry = ENTRIES.get(i);
            int x = panelX + 14 + i % 2 * (columnWidth + 8);
            int y = top + i / 2 * step;
            AbstractWidget original = entry.widget();
            if (original.getClass() == Button.class) {
                original.setX(x);
                original.setY(y);
                original.setWidth(columnWidth);
                original.setHeight(height);
                RowButtons.register((Button) original, "", slim ? () -> "" : entry.detail());
            } else {
                // Wrapped: the original keeps working but is not drawn; the row draws it and passes the mouse on.
                original.setX(x);
                original.setY(y);
                original.setWidth(columnWidth);
                original.setHeight(height);
                ProxyRow proxy = new ProxyRow(original, entry.title(), slim ? () -> "" : entry.detail(), x, y, columnWidth, height);
                event.removeListener(original);
                event.addListener(proxy);
                if (entry.key().equals("difficulty") && lock != null) {
                    lock.setX(x + columnWidth - 24);
                    lock.setY(y + (height - 20) / 2);
                }
            }
        }
        done.setWidth(200);
        done.setHeight(slim ? 20 : 30);
        done.setX(panelX + (panelW - 200) / 2);
        done.setY(panelY + panelH - (slim ? 26 : 48));
        search = new FieldBox(Minecraft.getInstance().font, panelX + 14, panelY + 34, panelW - 28, 22, Component.translatable("fordix.options.search"));
        search.setHint(Component.translatable("fordix.options.search"));
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            filter();
        });
        event.addListener(search);
        filter();
        laidOut = screen;
        if (TITLE != null) {
            try {
                TITLE.set(screen, Component.empty());
            } catch (IllegalAccessException | RuntimeException ignored) {
                // The game's centered title stays: harmless.
            }
        }
    }

    /** Hides the settings that do not match the search. */
    private static void filter() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        for (Entry entry : ENTRIES) {
            boolean match = needle.isEmpty() || entry.title().toLowerCase(Locale.ROOT).contains(needle)
                    || entry.detail().get().toLowerCase(Locale.ROOT).contains(needle);
            entry.widget().visible = match;
            entry.widget().active = match || entry.widget().active;
        }
    }

    public static void onBackgroundRendered(ScreenEvent.BackgroundRendered event) {
        if (event.getScreen() != laidOut) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        Glass.panel(g, panelX, panelY, panelW, panelH);
        Glass.title(g, font, Tr.s("options.title"), panelX + 14, panelY + 10);
        String world = Minecraft.getInstance().getCurrentServer() != null ? Minecraft.getInstance().getCurrentServer().name
                : Minecraft.getInstance().getSingleplayerServer() != null
                ? Minecraft.getInstance().getSingleplayerServer().getWorldData().getLevelName() : "";
        g.drawString(font, world.toUpperCase(Locale.ROOT), panelX + panelW - 14 - font.width(world), panelY + 10, Glass.dim(), false);
    }

    private static String keyOf(AbstractWidget widget) {
        return widget.getMessage().getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
    }

    private static boolean isOption(AbstractWidget widget, String caption) {
        if (widget.getMessage().getContents() instanceof TranslatableContents translatable && translatable.getArgs().length > 0
                && translatable.getArgs()[0] instanceof Component first && first.getContents() instanceof TranslatableContents inner) {
            return inner.getKey().equals(caption);
        }
        return false;
    }

    /** The value part of a "Caption: value" message. */
    private static String valueOf(AbstractWidget widget) {
        if (widget.getMessage().getContents() instanceof TranslatableContents translatable && translatable.getArgs().length > 1
                && translatable.getArgs()[1] instanceof Component value) {
            return value.getString();
        }
        String text = widget.getMessage().getString();
        int colon = text.indexOf(": ");
        return colon >= 0 ? text.substring(colon + 2) : text;
    }

    private static String titleOf(AbstractWidget widget) {
        return widget.getMessage().getString().replace("...", "").replace("…", "");
    }

    private static String detailOf(String key) {
        Options options = Minecraft.getInstance().options;
        if (options == null) {
            return "";
        }
        return switch (key) {
            case "options.video" -> options.renderDistance().get() + " chunks · "
                    + (options.framerateLimit().get() >= 260 ? "FPS max" : options.framerateLimit().get() + " FPS");
            case "options.sounds" -> Math.round(options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER) * 100.0F) + " %";
            case "options.language" -> {
                var language = Minecraft.getInstance().getLanguageManager().getLanguage(options.languageCode);
                yield language == null ? options.languageCode : language.name();
            }
            default -> {
                if (key.startsWith("options.resourcepack") || key.startsWith("resourcePack")) {
                    yield Tr.s("options.packs", options.resourcePacks.size());
                } else if (key.startsWith("options.chat")) {
                    yield Component.translatable(options.chatVisibility().get().getKey()).getString();
                } else if (key.startsWith("options.accessibility")) {
                    yield options.narrator().get().getName().getString();
                }
                yield "";
            }
        };
    }

    /** A row wrapping a widget that cannot be drawn as one (a slider, a cycle button): drawn here, driven by the original. */
    private static final class ProxyRow extends AbstractWidget {
        private final AbstractWidget original;
        private final String title;
        private final java.util.function.Supplier<String> detail;
        private final Glass.Fade fade = new Glass.Fade();

        ProxyRow(AbstractWidget original, String title, java.util.function.Supplier<String> detail, int x, int y, int width, int height) {
            super(x, y, width, height, CommonComponents.EMPTY);
            this.original = original;
            this.title = title;
            this.detail = detail;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            Glass.button(g, getX(), getY(), getWidth(), getHeight(), Glass.Kind.NORMAL, fade.update(isHoveredOrFocused()), true);
            int box = Math.min(18, getHeight() - 6);
            int bx = getX() + 6;
            int by = getY() + (getHeight() - box) / 2;
            g.fill(bx, by, bx + box, by + box, com.omneria.fordix.client.hud.HudTokens.withAlpha(com.omneria.fordix.client.gui.FordixPalette.ACCENT, 0x24));
            String icon = title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase(Locale.ROOT);
            g.drawString(font, icon, bx + (box - font.width(icon)) / 2, by + (box - 8) / 2 + 1, com.omneria.fordix.client.gui.FordixPalette.ACCENT, false);
            int textX = bx + box + 7;
            String detailText = detail.get();
            g.drawString(font, title, textX, detailText.isEmpty() ? getY() + (getHeight() - 8) / 2 : getY() + getHeight() / 2 - 9,
                    Glass.buttonText(Glass.Kind.NORMAL, true), false);
            if (!detailText.isEmpty()) {
                g.drawString(font, detailText, textX, getY() + getHeight() / 2 + 1, Glass.dim(), false);
            }
        }

        private <T> T through(java.util.function.Supplier<T> action) {
            boolean visible = original.visible;
            original.visible = true;
            try {
                return action.get();
            } finally {
                original.visible = visible;
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible || !isMouseOver(mouseX, mouseY)) {
                return false;
            }
            return through(() -> original.mouseClicked(mouseX, mouseY, button));
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return through(() -> original.mouseDragged(mouseX, mouseY, button, dragX, dragY));
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return through(() -> original.mouseReleased(mouseX, mouseY, button));
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return isFocused() && through(() -> original.keyPressed(keyCode, scanCode, modifiers));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
