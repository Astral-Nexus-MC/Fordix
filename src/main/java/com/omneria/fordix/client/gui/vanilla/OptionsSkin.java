package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.hud.HudTokens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The options menu as a list of rows (Voidframe 2): each category button becomes a row with an icon square, its title and,
 * under it, what is currently set in there (render distance, volume, language...), a chevron on the right. The game's screen,
 * its buttons and the screens they open are untouched; only the buttons are made taller and drawn as rows.
 */
public final class OptionsSkin {
    private static final int ROW_HEIGHT = 26;
    private static final int ROW_STEP = 30;

    /** What a row says about itself: a letter for the icon and the current value, read each frame. */
    private static final Map<Button, String> KEYS = new WeakHashMap<>();

    private OptionsSkin() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        KEYS.clear();
        if (!FordixConfig.VOIDFRAME_VANILLA_SCREENS.get() || event.getScreen().getClass() != OptionsScreen.class) {
            return;
        }
        List<Button> rows = new ArrayList<>();
        Button done = null;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener.getClass() != Button.class) {
                continue;
            }
            Button button = (Button) listener;
            String key = keyOf(button);
            if (key.equals("gui.done")) {
                done = button;
            } else {
                rows.add(button);
                KEYS.put(button, key);
            }
        }
        if (rows.isEmpty()) {
            return;
        }
        rows.sort(Comparator.<AbstractWidget>comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getX));
        int firstY = rows.get(0).getY();
        int lastBottom = firstY;
        int previousY = Integer.MIN_VALUE;
        int line = -1;
        for (Button button : rows) {
            if (Math.abs(button.getY() - previousY) > 3) {
                line++;
                previousY = button.getY();
            }
            button.setY(firstY + line * ROW_STEP);
            button.setHeight(ROW_HEIGHT);
            lastBottom = Math.max(lastBottom, button.getY() + ROW_HEIGHT);
        }
        if (done != null) {
            done.setY(Math.max(done.getY(), lastBottom + 8));
        }
    }

    static String keyOf(Button button) {
        return button.getMessage().getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
    }

    /** Whether this button is one of the rows (it then draws itself with {@link #draw}). */
    static boolean isRow(Button original) {
        return KEYS.containsKey(original);
    }

    static void draw(GuiGraphics g, Font font, Button original, int x, int y, int width, int height, float hover, boolean active) {
        String key = KEYS.getOrDefault(original, "");
        Glass.button(g, x, y, width, height, Glass.Kind.NORMAL, hover, active);
        String title = original.getMessage().getString().replace("...", "").replace("…", "");
        int box = 18;
        int bx = x + 6;
        int by = y + (height - box) / 2;
        g.fill(bx, by, bx + box, by + box, HudTokens.withAlpha(FordixPalette.ACCENT, 0x24));
        int edge = HudTokens.withAlpha(FordixPalette.ACCENT, 0x55);
        g.fill(bx, by, bx + box, by + 1, edge);
        g.fill(bx, by + box - 1, bx + box, by + box, edge);
        g.fill(bx, by + 1, bx + 1, by + box - 1, edge);
        g.fill(bx + box - 1, by + 1, bx + box, by + box - 1, edge);
        String letter = title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        g.drawString(font, letter, bx + (box - font.width(letter)) / 2, by + 5, FordixPalette.ACCENT, false);
        int textX = bx + box + 7;
        String detail = detailOf(key);
        int titleY = detail.isEmpty() ? y + (height - 8) / 2 : y + 5;
        g.drawString(font, font.plainSubstrByWidth(title, width - (textX - x) - 16), textX, titleY, Glass.buttonText(Glass.Kind.NORMAL, active), false);
        if (!detail.isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth(detail, width - (textX - x) - 16), textX, y + 15, Glass.dim(), false);
        }
        g.drawString(font, "›", x + width - 10, y + (height - 8) / 2, Glass.dim(), false);
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
            case "options.language" -> options.languageCode;
            case "resourcePack.title" -> options.resourcePacks.size() + " " + net.minecraft.network.chat.Component.translatable("resourcePack.title").getString().toLowerCase(java.util.Locale.ROOT).replace("resource packs", "packs");
            case "options.chat.title" -> options.chatVisibility().get().getKey().isEmpty() ? "" : net.minecraft.network.chat.Component.translatable(options.chatVisibility().get().getKey()).getString();
            case "options.accessibility.title" -> options.narrator().get().getName().getString();
            default -> "";
        };
    }
}
