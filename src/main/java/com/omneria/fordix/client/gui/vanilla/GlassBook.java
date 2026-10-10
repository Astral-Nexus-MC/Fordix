package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.gui.widget.FieldBox;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.client.hud.HudTokens;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.BlastingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.SmeltingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SmokingRecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/**
 * The recipe book in Voidframe 2 glass: the game keeps the logic (recipes, tabs, search, paging, clicks, ghost recipe)
 * and this draws everything else, so the panel can be as tall as the container beside it. The game's widgets are
 * moved into this layout each frame and keep their own hit boxes; its private fields are reached by type and order
 * (names differ between mappings).
 */
final class GlassBook {
    static final int WIDTH = 147;
    private static final Map<String, Field> FIELDS = new HashMap<>();

    private GlassBook() {
    }

    /** Where the panel stands: the top and height of the container it follows, or -1 for the game's own 166 px. */
    static final class Frame {
        int top = -1;
        int height = 166;
    }

    interface Framed {
        Frame frame();
    }

    static void frame(RecipeBookComponent book, int top, int height) {
        if (book instanceof Framed framed) {
            framed.frame().top = top;
            framed.frame().height = height;
        }
    }

    /** Swaps the game's book in a screen for the glass one (the field is final: set by reflection). */
    static void install(Object screen, RecipeBookComponent glass) {
        for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && RecipeBookComponent.class.isAssignableFrom(field.getType())
                        && field.getType().isAssignableFrom(glass.getClass())) {
                    try {
                        field.setAccessible(true);
                        field.set(screen, glass);
                        return;
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        return;
                    }
                }
            }
        }
    }

    static final class Plain extends RecipeBookComponent implements Framed {
        private final Frame frame = new Frame();

        @Override
        public Frame frame() {
            return frame;
        }

        @Override
        public void initVisuals() {
            super.initVisuals();
            GlassBook.afterInit(this);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            GlassBook.draw(this, frame, g, mouseX, mouseY, partialTick);
        }
    }

    static final class Smelting extends SmeltingRecipeBookComponent implements Framed {
        private final Frame frame = new Frame();

        @Override
        public Frame frame() {
            return frame;
        }

        @Override
        public void initVisuals() {
            super.initVisuals();
            GlassBook.afterInit(this);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            GlassBook.draw(this, frame, g, mouseX, mouseY, partialTick);
        }
    }

    static final class Blasting extends BlastingRecipeBookComponent implements Framed {
        private final Frame frame = new Frame();

        @Override
        public Frame frame() {
            return frame;
        }

        @Override
        public void initVisuals() {
            super.initVisuals();
            GlassBook.afterInit(this);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            GlassBook.draw(this, frame, g, mouseX, mouseY, partialTick);
        }
    }

    static final class Smoking extends SmokingRecipeBookComponent implements Framed {
        private final Frame frame = new Frame();

        @Override
        public Frame frame() {
            return frame;
        }

        @Override
        public void initVisuals() {
            super.initVisuals();
            GlassBook.afterInit(this);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            GlassBook.draw(this, frame, g, mouseX, mouseY, partialTick);
        }
    }

    // ---- reflection

    private static Field find(Class<?> owner, Class<?> type, int nth) {
        String key = owner.getName() + "/" + type.getName() + "/" + nth;
        Field cached = FIELDS.get(key);
        if (cached != null) {
            return cached;
        }
        int seen = 0;
        for (Field field : owner.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.getType() == type) {
                if (seen++ == nth) {
                    field.setAccessible(true);
                    FIELDS.put(key, field);
                    return field;
                }
            }
        }
        throw new IllegalStateException("recipe book field " + key);
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(Object owner, Class<?> declaring, Class<?> type, int nth) {
        try {
            return (T) find(declaring, type, nth).get(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void set(Object owner, Class<?> declaring, Class<?> type, int nth, Object value) {
        try {
            find(declaring, type, nth).set(owner, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int num(Object owner, Class<?> declaring, int nth) {
        return GlassBook.<Integer>get(owner, declaring, int.class, nth);
    }

    // ---- setup

    /** After the game laid its widgets out: the search box becomes a glass field. */
    static void afterInit(RecipeBookComponent book) {
        EditBox old = get(book, RecipeBookComponent.class, EditBox.class, 0);
        FieldBox box = new FieldBox(Minecraft.getInstance().font, 0, 0, 127, 16, old.getMessage());
        box.setMaxLength(50);
        box.setValue(old.getValue());
        box.setHint(Component.translatable("gui.recipebook.search_hint").withStyle(style -> style.withColor(0x8AA0AD)));
        box.setCanLoseFocus(false);
        box.setFocused(true);
        set(book, RecipeBookComponent.class, EditBox.class, 0, box);
    }

    // ---- drawing

    static void draw(RecipeBookComponent book, Frame frame, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!book.isVisible()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int xOffset = num(book, RecipeBookComponent.class, 0);
        int width = num(book, RecipeBookComponent.class, 1);
        int height = num(book, RecipeBookComponent.class, 2);
        int px = (width - WIDTH) / 2 - xOffset;
        int py = frame.top >= 0 ? frame.top : (height - 166) / 2;
        int ph = frame.top >= 0 ? frame.height : 166;
        boolean airy = ph >= 230;

        EditBox search = get(book, RecipeBookComponent.class, EditBox.class, 0);
        List<RecipeBookTabButton> tabs = get(book, RecipeBookComponent.class, List.class, 0);
        StateSwitchingButton filter = get(book, RecipeBookComponent.class, StateSwitchingButton.class, 0);
        RecipeBookPage page = get(book, RecipeBookComponent.class, RecipeBookPage.class, 0);
        List<RecipeButton> cells = get(page, RecipeBookPage.class, List.class, 0);
        StateSwitchingButton forward = get(page, RecipeBookPage.class, StateSwitchingButton.class, 0);
        StateSwitchingButton back = get(page, RecipeBookPage.class, StateSwitchingButton.class, 1);
        OverlayRecipeComponent overlay = get(page, RecipeBookPage.class, OverlayRecipeComponent.class, 0);
        int totalPages = num(page, RecipeBookPage.class, 0);
        int currentPage = num(page, RecipeBookPage.class, 1);

        // Layout: the game's widgets move to these places and keep answering clicks there.
        int tabTop = py + (airy ? 52 : 26);
        int tabHeight = airy ? 22 : 20;
        int gridTop = py + (airy ? 82 : 50);
        int pitch = airy ? 25 : 24;
        int gridLeft = px + (WIDTH - 5 * pitch) / 2;
        int pagerY = py + ph - (airy ? 26 : 16);
        search.setX(px + 10);
        search.setY(py + (airy ? 30 : 7));
        search.setWidth(127);
        search.setHeight(airy ? 16 : 15);
        int shown = 0;
        for (RecipeBookTabButton tab : tabs) {
            if (tab.visible) {
                tab.setX(px + 10 + shown * 25);
                tab.setY(tabTop);
                tab.setWidth(23);
                tab.setHeight(tabHeight);
                shown++;
            }
        }
        filter.setX(px + 10);
        filter.setY(pagerY);
        filter.setWidth(56);
        filter.setHeight(14);
        forward.setX(px + WIDTH - 26);
        forward.setY(pagerY);
        forward.setWidth(16);
        forward.setHeight(14);
        back.setX(px + WIDTH - 26 - 16 - 36);
        back.setY(pagerY);
        back.setWidth(16);
        back.setHeight(14);
        RecipeButton hovered = null;
        for (int i = 0; i < cells.size(); i++) {
            RecipeButton cell = cells.get(i);
            cell.setX(gridLeft + pitch * (i % 5));
            cell.setY(gridTop + pitch * (i / 5));
            cell.setHeight(pitch);
            if (cell.visible && cell.isMouseOver(mouseX, mouseY)) {
                hovered = cell;
            }
        }
        set(page, RecipeBookPage.class, RecipeButton.class, 0, hovered);

        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 100.0F);
        Panel.draw(g, px, py, WIDTH, ph);
        if (airy) {
            Glass.title(g, font, Tr.s("inv.book.title"), px + 10, py + 9);
            Glass.rule(g, px + 10, py + 22, WIDTH - 20);
        }
        search.render(g, mouseX, mouseY, partialTick);
        if (search.getValue().isEmpty()) {
            String hint = Component.translatable("gui.recipebook.search_hint").getString();
            g.drawString(font, hint, search.getX() + 5, search.getY() + (search.getHeight() - 8) / 2, 0xFF8AA0AD, false);
        }

        int accent = 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF);
        for (RecipeBookTabButton tab : tabs) {
            if (!tab.visible) {
                continue;
            }
            int x = tab.getX();
            int y = tab.getY();
            boolean on = tab.isStateTriggered();
            boolean over = tab.isMouseOver(mouseX, mouseY);
            Glass.well(g, x, y, tab.getWidth(), tab.getHeight());
            if (on) {
                g.fill(x, y, x + tab.getWidth(), y + tab.getHeight(), HudTokens.withAlpha(FordixPalette.ACCENT, 0x2A));
                g.fill(x, y + tab.getHeight() - 2, x + tab.getWidth(), y + tab.getHeight(), accent);
            } else if (over) {
                g.fill(x, y, x + tab.getWidth(), y + tab.getHeight(), 0x14FFFFFF);
            }
            List<ItemStack> icons = tab.getCategory().getIconItems();
            if (!icons.isEmpty()) {
                g.renderFakeItem(icons.get(0), x + (tab.getWidth() - 16) / 2, y + (tab.getHeight() - 16) / 2);
            }
        }

        boolean any = false;
        for (RecipeButton cell : cells) {
            if (!cell.visible) {
                continue;
            }
            any = true;
            Recipe<?> recipe = cell.getRecipe();
            boolean craftable = cell.getCollection().isCraftable(recipe);
            int x = cell.getX();
            int y = cell.getY();
            Glass.well(g, x, y, pitch - 1, pitch - 1);
            if (craftable) {
                int edge = HudTokens.withAlpha(FordixPalette.ACCENT, 0x90);
                g.fill(x, y, x + pitch - 1, y + 1, edge);
                g.fill(x, y + pitch - 2, x + pitch - 1, y + pitch - 1, edge);
                g.fill(x, y + 1, x + 1, y + pitch - 2, edge);
                g.fill(x + pitch - 2, y + 1, x + pitch - 1, y + pitch - 2, edge);
            }
            if (cell == hovered) {
                g.fill(x, y, x + pitch - 1, y + pitch - 1, 0x1CFFFFFF);
            }
            ItemStack stack = recipe.getResultItem(mc.level.registryAccess());
            int ix = x + (pitch - 1 - 16) / 2;
            int iy = y + (pitch - 1 - 16) / 2;
            g.renderItem(stack, ix, iy);
            if (!craftable) {
                g.fill(ix, iy, ix + 16, iy + 16, 0x80070B10);
            }
            if (!cell.isOnlyOption()) {
                g.fill(x + pitch - 6, y + 2, x + pitch - 3, y + 3, accent);
                g.fill(x + pitch - 5, y + 1, x + pitch - 4, y + 4, accent);
            }
        }
        if (!any) {
            String empty = Tr.s("inv.book.empty");
            g.drawString(font, empty, px + (WIDTH - font.width(empty)) / 2, gridTop + 40, Glass.dim(), false);
        }

        boolean craftableOnly = filter.isStateTriggered();
        Glass.button(g, filter.getX(), filter.getY(), filter.getWidth(), filter.getHeight(),
                craftableOnly ? Glass.Kind.PRIMARY : Glass.Kind.NORMAL, filter.isMouseOver(mouseX, mouseY) ? 1.0F : 0.0F, true);
        String filterText = Tr.s(craftableOnly ? "inv.book.craftable" : "inv.book.all");
        g.drawString(font, filterText, filter.getX() + (filter.getWidth() - font.width(filterText)) / 2, filter.getY() + 3,
                Glass.buttonText(craftableOnly ? Glass.Kind.PRIMARY : Glass.Kind.NORMAL, true), false);

        if (totalPages > 1) {
            pagerButton(g, back, "<", mouseX, mouseY);
            pagerButton(g, forward, ">", mouseX, mouseY);
            String text = (currentPage + 1) + "/" + totalPages;
            g.drawString(font, text, back.getX() + 16 + (36 - font.width(text)) / 2, pagerY + 3, FordixPalette.TEXT_BRIGHT, false);
        }
        if (overlay.isVisible()) {
            overlay.render(g, mouseX, mouseY, partialTick);
        }
        g.pose().popPose();
    }

    private static void pagerButton(GuiGraphics g, AbstractWidget button, String glyph, int mouseX, int mouseY) {
        if (!button.visible) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        Glass.button(g, button.getX(), button.getY(), button.getWidth(), button.getHeight(), Glass.Kind.NORMAL,
                button.isMouseOver(mouseX, mouseY) ? 1.0F : 0.0F, true);
        g.drawString(font, glyph, button.getX() + (button.getWidth() - font.width(glyph)) / 2, button.getY() + 3, FordixPalette.TEXT_BRIGHT, false);
    }
}
