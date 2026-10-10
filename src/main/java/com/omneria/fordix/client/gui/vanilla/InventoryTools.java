package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.chat.ChatSearch;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.ModNames;
import com.omneria.fordix.client.gui.widget.FordixButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import com.omneria.fordix.client.gui.widget.FieldBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * A search field and a sort button on the survival inventory, chests, barrels and shulker boxes. The search dims every
 * stack that doesn't match (name, id or mod); the button tidies the container (or, with Shift or on the inventory, the
 * player's bag): identical stacks are merged, the rest ordered by kind and packed at the start, with ordinary clicks a
 * couple per tick so a server never sees a burst ({@link SortPlan} works out the moves).
 */
public final class InventoryTools {
    private static final int SEARCH_WIDTH = 100;
    private static final int SORT_WIDTH = 34;
    private static final int HEIGHT = 14;
    private static final int CLICKS_PER_TICK = 2;
    private static final int TAB_FILL = 0xE0141F2B;

    /** One click of a sort: a slot id, skipped when the cursor has nothing left to put down. */
    private record Click(int slotId, boolean onlyIfCarried) {
    }

    private static Screen current;
    private static EditBox search;
    private static FordixButton sortButton;
    private static String query = "";
    private static int searchWidth = SEARCH_WIDTH;
    private static final Deque<Click> QUEUE = new ArrayDeque<>();
    private static int queueMenu = -1;

    private InventoryTools() {
    }

    private static boolean enabled() {
        try {
            return FordixConfig.INVENTORY_TOOLS.get();
        } catch (IllegalStateException notLoadedYet) {
            return false;
        }
    }

    private static boolean eligible(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return false;
        }
        AbstractContainerMenu menu = container.getMenu();
        return screen instanceof InventoryScreen || menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu;
    }

    /** Whether the inventory search field is being typed in (the guide key must not fire on its letters). */
    public static boolean typing() {
        return search != null && search.isFocused();
    }

    private static boolean active(Screen screen) {
        return enabled() && screen == current && search != null;
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        current = null;
        search = null;
        sortButton = null;
        query = "";
        QUEUE.clear();
        if (!enabled() || !eligible(screen)) {
            return;
        }
        AbstractContainerScreen<?> container = (AbstractContainerScreen<?>) screen;
        current = screen;
        int right = container.getGuiLeft() + container.getXSize();
        int y = Math.max(2, container.getGuiTop() - HEIGHT - 2);
        Minecraft minecraft = Minecraft.getInstance();
        int[] inside = screen instanceof WideLayout layout && layout.wide() ? layout.searchRect() : null;
        int[] insideSort = screen instanceof WideLayout layout && layout.wide() ? layout.sortRect() : null;
        int searchX = inside != null ? container.getGuiLeft() + inside[0] : right - SORT_WIDTH - 4 - SEARCH_WIDTH;
        int searchY = inside != null ? container.getGuiTop() + inside[1] : y;
        int searchW = inside != null ? inside[2] : SEARCH_WIDTH;
        int searchH = inside != null ? inside[3] : HEIGHT;
        searchWidth = searchW;
        search = new FieldBox(minecraft.font, searchX, searchY, searchW, searchH, Tr.c("inventory.search"));
        search.setMaxLength(40);
        search.setHint(Tr.c("inventory.search").withStyle(style -> style.withColor(0x8AA0AD)));
        search.setResponder(text -> query = text);
        sortButton = insideSort != null
                ? new FordixButton(container.getGuiLeft() + insideSort[0], container.getGuiTop() + insideSort[1], insideSort[2], insideSort[3],
                        Tr.c("inventory.sort"), InventoryTools::sortClicked)
                : new FordixButton(right - SORT_WIDTH, y, SORT_WIDTH, HEIGHT, Tr.c("inventory.sort"), InventoryTools::sortClicked);
    }

    // ---- drawing: the widgets, and the dimming of stacks that don't match

    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!active(event.getScreen())) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        search.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        sortButton.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    private static String textOf(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return stack.getHoverName().getString() + " " + key.getPath().replace('_', ' ') + " " + ModNames.of(key.getNamespace());
    }

    /** Over the slots, before tooltips: stacks that don't match are dimmed, the ones that do get an accent frame. */
    public static void onForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!active(event.getContainerScreen()) || query.isBlank()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int accent = 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        for (Slot slot : event.getContainerScreen().getMenu().slots) {
            if (!slot.hasItem()) {
                continue;
            }
            if (ChatSearch.matches(textOf(slot.getItem()), query)) {
                graphics.fill(slot.x - 1, slot.y - 1, slot.x + 17, slot.y, accent);
                graphics.fill(slot.x - 1, slot.y + 16, slot.x + 17, slot.y + 17, accent);
                graphics.fill(slot.x - 1, slot.y, slot.x, slot.y + 16, accent);
                graphics.fill(slot.x + 16, slot.y, slot.x + 17, slot.y + 16, accent);
            } else {
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xB0000000);
            }
        }
        graphics.pose().popPose();
    }

    // ---- input

    private static boolean over(net.minecraft.client.gui.components.AbstractWidget widget, double x, double y) {
        return x >= widget.getX() && x < widget.getX() + widget.getWidth() && y >= widget.getY() && y < widget.getY() + widget.getHeight();
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!active(event.getScreen())) {
            return;
        }
        double x = event.getMouseX();
        double y = event.getMouseY();
        if (event.getButton() == 0 && over(search, x, y)) {
            search.setFocused(true);
            search.mouseClicked(x, y, 0);
            event.setCanceled(true);
        } else if (event.getButton() == 0 && over(sortButton, x, y)) {
            search.setFocused(false);
            sortButton.onClick(x, y);
            event.setCanceled(true);
        } else {
            search.setFocused(false);
        }
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!active(event.getScreen())) {
            return;
        }
        if (!search.isFocused() && com.omneria.fordix.client.FordixClient.pressed(com.omneria.fordix.client.FordixClient.SEARCH_FIELD, event.getKeyCode(), event.getScanCode())) {
            search.setFocused(true);
            event.setCanceled(true);
        } else if (search.isFocused() && event.getKeyCode() != GLFW.GLFW_KEY_ESCAPE) {
            if (event.getKeyCode() == GLFW.GLFW_KEY_ENTER || event.getKeyCode() == GLFW.GLFW_KEY_KP_ENTER) {
                search.setFocused(false);
            } else {
                search.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            }
            event.setCanceled(true);
        }
    }

    public static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (active(event.getScreen()) && search.isFocused()) {
            search.charTyped(event.getCodePoint(), event.getModifiers());
            event.setCanceled(true);
        }
    }

    /** Types into the search field (used by the in-game test). */
    public static void setSearch(String text) {
        if (search != null) {
            search.setValue(text);
        }
    }

    // ---- sorting

    private static List<Slot> region(AbstractContainerMenu menu, boolean bag) {
        Minecraft minecraft = Minecraft.getInstance();
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            boolean mine = slot.container == minecraft.player.getInventory();
            if (bag ? mine && slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 36 : !mine) {
                slots.add(slot);
            }
        }
        if (bag) {
            slots.sort(Comparator.comparingInt(Slot::getContainerSlot));
        }
        return slots;
    }

    private static SortPlan.Entry entryOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String same = key + "#" + (stack.getTag() == null ? "" : stack.getTag().toString()) + "@" + stack.getDamageValue();
        int group = stack.isDamageableItem() ? 0 : stack.getItem() instanceof BlockItem ? 1 : 2;
        String order = String.format(Locale.ROOT, "%d|%s|%05d", group, key, stack.getDamageValue());
        return new SortPlan.Entry(same, stack.getCount(), stack.getMaxStackSize(), order);
    }

    private static void sortClicked() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen) || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        if (!menu.getCarried().isEmpty() || !QUEUE.isEmpty()) {
            return;
        }
        boolean bag = screen instanceof InventoryScreen || Screen.hasShiftDown();
        List<Slot> slots = region(menu, bag);
        List<SortPlan.Entry> entries = new ArrayList<>();
        for (Slot slot : slots) {
            entries.add(entryOf(slot.getItem()));
        }
        queueMenu = menu.containerId;
        for (SortPlan.Move move : SortPlan.plan(entries)) {
            int from = slots.get(move.from()).index;
            int to = slots.get(move.to()).index;
            if (move.merge()) {
                QUEUE.add(new Click(from, false));
                QUEUE.add(new Click(to, false));
                QUEUE.add(new Click(from, true));
            } else {
                QUEUE.add(new Click(to, false));
                QUEUE.add(new Click(from, false));
                QUEUE.add(new Click(to, true));
            }
        }
    }

    /** A couple of the queued clicks per tick; the queue is dropped as soon as the screen is not the sorted one any more. */
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.containerMenu.containerId != queueMenu
                || !(minecraft.screen instanceof AbstractContainerScreen<?>)) {
            QUEUE.clear();
            return;
        }
        for (int i = 0; i < CLICKS_PER_TICK && !QUEUE.isEmpty(); i++) {
            Click click = QUEUE.poll();
            if (click.onlyIfCarried() && minecraft.player.containerMenu.getCarried().isEmpty()) {
                continue;
            }
            minecraft.gameMode.handleInventoryMouseClick(queueMenu, click.slotId(), 0, ClickType.PICKUP, minecraft.player);
        }
    }

    public static void reset() {
        QUEUE.clear();
        current = null;
        search = null;
        sortButton = null;
    }
}
