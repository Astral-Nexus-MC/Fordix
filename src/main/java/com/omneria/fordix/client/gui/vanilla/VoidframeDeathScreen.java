package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.hud.HudStyle;
import com.omneria.fordix.client.hud.HudTokens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * The death screen in Voidframe 2: red vignette, large heading, what killed the player, and a card with the score, the level
 * and the place of death (so the items can be found again). It subclasses the game's screen, so respawn, the title-screen
 * confirmation, the hardcore spectate case and the button delay are all the game's own; only the drawing and the
 * button positions change.
 */
public final class VoidframeDeathScreen extends DeathScreen implements VoidframeScreen {
    /** The game's screen has two components (the cause, then the score line) : the cause is the first declared. */
    private static final Field CAUSE = firstField(Component.class);
    private static final Field HARDCORE = Reflect.uniqueField(DeathScreen.class, boolean.class);

    private static Field firstField(Class<?> type) {
        for (Field field : DeathScreen.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.getType() == type) {
                try {
                    field.setAccessible(true);
                    return field;
                } catch (RuntimeException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private final Component cause;
    private final boolean hardcoreMode;
    private final long openedAt = System.currentTimeMillis();

    private VoidframeDeathScreen(@Nullable Component cause, boolean hardcore) {
        super(cause, hardcore);
        this.cause = cause;
        this.hardcoreMode = hardcore;
    }

    /** A Voidframe screen for the game's death screen, or null when its private state cannot be read (it then stays vanilla). */
    @Nullable
    public static DeathScreen replacing(DeathScreen vanilla) {
        if (CAUSE == null || HARDCORE == null) {
            return null;
        }
        Object cause = Reflect.get(CAUSE, vanilla);
        Object hardcore = Reflect.get(HARDCORE, vanilla);
        if (!(hardcore instanceof Boolean flag)) {
            return null;
        }
        return new VoidframeDeathScreen(cause instanceof Component component ? component : null, flag);
    }

    /** Tall enough for the mockup's positions (a 480 px screen); on a smaller one everything moves up and the card shrinks. */
    private boolean roomy() {
        return this.height >= 420;
    }

    @Override
    protected void init() {
        super.init();
        int y = roomy() ? Math.round(this.height * 324.0F / 480.0F) : Math.max(Math.round(this.height * 0.66F), 168);
        int index = 0;
        for (GuiEventListener child : children()) {
            if (child instanceof AbstractWidget widget) {
                widget.setWidth(roomy() ? 260 : 220);
                widget.setX((this.width - widget.getWidth()) / 2);
                widget.setY(y + index * (roomy() ? 40 : 30));
                widget.setHeight(index == 0 ? (roomy() ? 32 : 26) : (roomy() ? 28 : 22));
                index++;
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, this.width, this.height, 0xA0320808, 0xF0100404);
        HudStyle.vignette(g, this.width, this.height, 0.55F + 0.2F * HudTokens.pulse(System.currentTimeMillis()));
        int centerX = this.width / 2;
        boolean roomy = roomy();
        int labelY = roomy ? Math.round(this.height * 86.0F / 480.0F) : 38;
        String heading = Tr.s(hardcoreMode ? "death.hardcore" : "death.heading");
        g.drawString(this.font, heading.toUpperCase(java.util.Locale.ROOT), centerX - this.font.width(heading) / 2, labelY, 0xFFFF8A7A, false);
        float scale = roomy ? 5.0F : 3.0F;
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0F);
        String title = Tr.s("death.title");
        g.drawString(this.font, title, Math.round(centerX / scale - this.font.width(title) / 2.0F), Math.round((labelY + 14) / scale), 0xFFFFFFFF, true);
        g.pose().popPose();
        if (cause != null) {
            String line = cause.getString();
            g.pose().pushPose();
            float causeScale = roomy ? 1.6F : 1.0F;
            g.pose().scale(causeScale, causeScale, 1.0F);
            g.drawString(this.font, line, Math.round(centerX / causeScale - this.font.width(line) / 2.0F),
                    Math.round((roomy ? this.height * 160.0F / 480.0F : 76.0F) / causeScale), 0xFFE3B6B0, true);
            g.pose().popPose();
        }
        card(g, centerX);
        for (GuiEventListener child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(g, mouseX, mouseY, partialTick);
            }
        }
    }

    private void card(GuiGraphics g, int centerX) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean roomy = roomy();
        int width = roomy ? 340 : 260;
        int height = roomy ? 100 : 58;
        int x = centerX - width / 2;
        int y = roomy ? Math.round(this.height * 206.0F / 480.0F) : 98;
        Glass.panel(g, x, y, width, height, 0xE5534B);
        int column = x + 14;
        int gap = roomy ? 80 : 62;
        Glass.label(g, this.font, Tr.s("death.score"), column, y + 10);
        g.drawString(this.font, String.valueOf(player.getScore()), column, y + 22, 0xFFFFD36A, false);
        Glass.label(g, this.font, Tr.s("death.level"), column + gap, y + 10);
        g.drawString(this.font, String.valueOf(player.experienceLevel), column + gap, y + 22, 0xFF9BF0B9, false);
        Glass.label(g, this.font, Tr.s("death.place"), column + gap * 2, y + 10);
        g.drawString(this.font, String.format("%d \u00B7 %d \u00B7 %d", player.getBlockX(), player.getBlockY(), player.getBlockZ()),
                column + gap * 2, y + 22, FordixPalette.TEXT_BRIGHT, false);
        String dimension = minecraft.level == null ? "" : minecraft.level.dimension().location().getPath().replace('_', ' ');
        int pillY = roomy ? y + 72 : y + 38;
        if (roomy) {
            g.drawString(this.font, dimension, column + gap * 2, y + 34, Glass.dim(), false);
        }
        long left = 300L - (System.currentTimeMillis() - openedAt) / 1000L;
        String items = left > 0L ? Tr.s("death.items", String.format("%d min %02d s", left / 60L, left % 60L)) : Tr.s("death.items.gone");
        int cursor = column;
        if (!roomy) {
            cursor += Glass.pill(g, this.font, dimension, column, pillY, FordixPalette.ACCENT) + 4;
        }
        Glass.pill(g, this.font, items, cursor, pillY, left > 60L ? 0xFFE8A64A : 0xFFE5534B);
    }
}
