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

    @Override
    protected void init() {
        super.init();
        int y = Math.max(Math.round(this.height * 0.66F), 168);
        int index = 0;
        for (GuiEventListener child : children()) {
            if (child instanceof AbstractWidget widget) {
                widget.setWidth(220);
                widget.setX((this.width - 220) / 2);
                widget.setY(y + index * 30);
                widget.setHeight(index == 0 ? 26 : 22);
                index++;
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, this.width, this.height, 0xA0320808, 0xF0100404);
        HudStyle.vignette(g, this.width, this.height, 0.55F + 0.2F * HudTokens.pulse(System.currentTimeMillis()));
        int centerX = this.width / 2;
        Glass.label(g, this.font, Tr.s(hardcoreMode ? "death.hardcore" : "death.heading"), centerX - this.font.width(Tr.s(hardcoreMode ? "death.hardcore" : "death.heading")) / 2, 38);
        g.pose().pushPose();
        g.pose().scale(3.0F, 3.0F, 1.0F);
        String title = Component.translatable("deathScreen.title").getString();
        g.drawString(this.font, title, Math.round(centerX / 3.0F - this.font.width(title) / 2.0F), 17, 0xFFFFFFFF, true);
        g.pose().popPose();
        if (cause != null) {
            String line = cause.getString();
            g.drawString(this.font, line, centerX - this.font.width(line) / 2, 76, HudTokens.mix(0xFFFFFFFF, 0xFFE5534B, 0.35F), true);
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
        int width = 260;
        int height = 58;
        int x = centerX - width / 2;
        int y = 98;
        Glass.panel(g, x, y, width, height, 0xE5534B);
        int column = x + 14;
        Glass.label(g, this.font, Tr.s("death.score"), column, y + 10);
        g.drawString(this.font, String.valueOf(player.getScore()), column, y + 22, 0xFFFFD36A, false);
        Glass.label(g, this.font, Tr.s("death.level"), column + 62, y + 10);
        g.drawString(this.font, String.valueOf(player.experienceLevel), column + 62, y + 22, 0xFF9BF0B9, false);
        Glass.label(g, this.font, Tr.s("death.place"), column + 124, y + 10);
        g.drawString(this.font, String.format("%d  %d  %d", player.getBlockX(), player.getBlockY(), player.getBlockZ()), column + 124, y + 22,
                FordixPalette.TEXT_BRIGHT, false);
        String dimension = minecraft.level == null ? "" : minecraft.level.dimension().location().getPath().replace('_', ' ');
        int cursor = column + Glass.pill(g, this.font, dimension, column, y + 38, FordixPalette.ACCENT) + 4;
        // Dropped items disappear five minutes after the death: a countdown from the moment the screen opened.
        long left = 300L - (System.currentTimeMillis() - openedAt) / 1000L;
        String items = left > 0L ? Tr.s("death.items", String.format("%d:%02d", left / 60L, left % 60L)) : Tr.s("death.items.gone");
        Glass.pill(g, this.font, items, cursor, y + 38, left > 60L ? 0xFFE8A64A : 0xFFE5534B);
    }
}
