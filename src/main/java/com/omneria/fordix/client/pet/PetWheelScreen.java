package com.omneria.fordix.client.pet;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.FordixClient;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.panel.network.PanelNetwork;
import com.omneria.fordix.pet.PetAction;
import com.omneria.fordix.pet.ServerboundPetActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.UUID;

/**
 * The interaction wheel of a tamed creature: hold the key while looking at it, slide to a sector and let go (or click).
 * Feeding and naming are the game's own click with the item in hand; the other actions go to the server ({@link PetAction}).
 * A sector that cannot be used now is greyed and says why. Setting a creature free asks for a second click.
 */
public final class PetWheelScreen extends Screen {
    private enum Slot {
        FOLLOW(Items.LEAD, "follow", PetAction.FOLLOW),
        STAY(Items.RED_BED, "stay", PetAction.STAY),
        FEED(Items.BONE, "feed", null),
        INVENTORY(Items.CHEST, "inventory", PetAction.INVENTORY),
        RECALL(Items.ENDER_PEARL, "recall", PetAction.RECALL),
        RENAME(Items.NAME_TAG, "rename", null),
        RELEASE(Items.BARRIER, "release", PetAction.RELEASE);

        final ItemStack icon;
        final String key;
        final PetAction action;

        Slot(net.minecraft.world.item.Item icon, String key, PetAction action) {
            this.icon = new ItemStack(icon);
            this.key = key;
            this.action = action;
        }
    }

    private static final Slot[] SLOTS = Slot.values();
    private static final int STEPS = 22;

    /** Dev test only: act as if the key were still held (no key is pressed while the screenshots are taken). */
    public static boolean testKeepOpen;

    /** The wheel is a HUD element like the others: the layout screen moves and sizes it (see {@link #ID}). */
    public static final String ID = "fordix:pet_wheel";
    /** Its size at normal scale, the box the layout screen shows. */
    public static final int BOX = 320;

    private final LivingEntity pet;
    private double centerX;
    private double centerY;
    private float scale = 1.0F;
    private int hovered = -1;
    private int openTicks;
    private boolean releaseArmed;

    public PetWheelScreen(LivingEntity pet) {
        super(Tr.c("pet.title"));
        this.pet = pet;
    }

    /** Whether the creature is tamed and belongs to the player: the only ones the wheel opens on. */
    public static boolean opensOn(net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.player.Player player) {
        return entity instanceof LivingEntity && entity instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID());
    }

    @Override
    protected void init() {
        var placed = com.omneria.fordix.client.gui.vanilla.HudSlots.placed(ID, this.width, this.height);
        // Never larger than the screen: on a small interface the wheel shrinks to fit.
        scale = Math.min(com.omneria.fordix.client.gui.vanilla.HudSlots.scale(ID), (Math.min(this.width, this.height) - 30) / (float) BOX);
        centerX = placed.x() + placed.w() / 2.0;
        centerY = placed.y() + placed.h() / 2.0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- what each sector can do now

    private ItemStack held() {
        return this.minecraft.player == null ? ItemStack.EMPTY : this.minecraft.player.getMainHandItem();
    }

    /**
     * Whether the creature sits. The client never gets the "ordered to sit" order (it is not synchronized, so it always reads
     * false here): only the sitting pose is, and that is what is drawn on the creature.
     */
    private boolean sitting() {
        return pet instanceof TamableAnimal animal && (animal.isInSittingPose() || animal.isOrderedToSit());
    }

    /** Null when the sector can be used, else the reason it cannot (shown when hovered). */
    private String unavailable(Slot slot) {
        switch (slot) {
            case FOLLOW:
                // Always useful: it stands up if it sits, and walks to you; only a creature already beside you has nothing to do.
                return sitting() || !(pet instanceof TamableAnimal) || this.minecraft.player == null
                        || pet.distanceToSqr(this.minecraft.player) > 9 ? null : Tr.s("pet.no.follow");
            case STAY:
                return pet instanceof TamableAnimal && !sitting() ? null : Tr.s("pet.no.stay");
            case FEED:
                return pet instanceof Animal animal && !held().isEmpty() && animal.isFood(held()) && pet.getHealth() < pet.getMaxHealth()
                        ? null : Tr.s("pet.no.feed");
            case INVENTORY:
                return pet instanceof AbstractHorse ? null : Tr.s("pet.no.inventory");
            case RENAME:
                return held().is(Items.NAME_TAG) && held().hasCustomHoverName() ? null : Tr.s("pet.no.rename");
            default:
                return null;
        }
    }

    private void activate(int index) {
        Slot slot = SLOTS[index];
        if (unavailable(slot) != null) {
            return;
        }
        if (slot == Slot.RELEASE && !releaseArmed) {
            releaseArmed = true;
            return;
        }
        if (slot.action != null) {
            PanelNetwork.CHANNEL.sendToServer(new ServerboundPetActionPayload(pet.getId(), slot.action.ordinal()));
        } else if (this.minecraft.gameMode != null && this.minecraft.player != null) {
            this.minecraft.gameMode.interact(this.minecraft.player, pet, InteractionHand.MAIN_HAND);
            this.minecraft.player.swing(InteractionHand.MAIN_HAND);
        }
        onClose();
    }

    // ---- input

    private boolean keyHeld() {
        if (testKeepOpen) {
            return true;
        }
        InputConstants.Key key = FordixClient.PET_WHEEL.getKey();
        long window = this.minecraft.getWindow().getWindow();
        return key.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(window, key.getValue());
    }

    @Override
    public void tick() {
        openTicks++;
        if (this.minecraft.player == null || !pet.isAlive() || pet.distanceToSqr(this.minecraft.player) > 100) {
            onClose();
            return;
        }
        // Letting go of the key picks the sector under the mouse (nothing there closes the wheel; "release" waits for a click).
        if (openTicks > 3 && !keyHeld()) {
            if (hovered < 0) {
                onClose();
            } else if (SLOTS[hovered] != Slot.RELEASE) {
                activate(hovered);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered >= 0) {
            if (SLOTS[hovered] != Slot.RELEASE) {
                releaseArmed = false;
            }
            activate(hovered);
            return true;
        }
        if (hovered < 0 && button == 0 && distanceFromCenter(mouseX, mouseY) > outerRadius() * scale) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int digit = keyCode - GLFW.GLFW_KEY_1;
        if (digit >= 0 && digit < SLOTS.length) {
            activate(digit);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private double distanceFromCenter(double x, double y) {
        return Math.hypot(x - centerX, y - centerY);
    }

    /** The wheel of the mockup: sectors from 70 to 150, a card of 62 in the middle. */
    private double outerRadius() {
        return 150;
    }

    private double hubRadius() {
        return 70;
    }

    // ---- drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, this.width, this.height, 0x66050A10, 0x88020408);
        double outer = outerRadius();
        double hub = hubRadius();
        // Everything is drawn around (0, 0) in the wheel's own units; the mouse is brought into them.
        double mx = (mouseX - centerX) / scale;
        double my = (mouseY - centerY) / scale;
        hovered = WheelGeometry.sectorAt(mx, my, SLOTS.length, hub);
        if (hovered >= 0 && Math.hypot(mx, my) > outer + 24) {
            hovered = -1;
        }
        if (hovered != SLOTS.length - 1) {
            releaseArmed = false;
        }
        g.pose().pushPose();
        g.pose().translate(centerX, centerY, 0);
        g.pose().scale(scale, scale, 1);
        double width = 2 * Math.PI / SLOTS.length;
        double gap = 0.035;
        for (int i = 0; i < SLOTS.length; i++) {
            double middle = WheelGeometry.middleOf(i, SLOTS.length);
            boolean usable = unavailable(SLOTS[i]) == null;
            boolean on = i == hovered && usable;
            int tone = ACTION_COLORS[Math.min(i, ACTION_COLORS.length - 1)];
            double from = middle - width / 2 + gap;
            double to = middle + width / 2 - gap;
            boolean release = SLOTS[i] == Slot.RELEASE;
            // Glass: dark and lit from above; the sector under the pointer takes the color of its action, with a glow and a bright edge.
            int top = on ? withAlpha(tone, 0x58) : release ? withAlpha(tone, 0x20) : 0xDC1A2834;
            int bottom = on ? withAlpha(tone, 0x24) : release ? withAlpha(tone, 0x0C) : 0xDC0C141C;
            if (on) {
                sector(g, 0, 0, outer, outer + 12, from, to, withAlpha(tone, 0x30), withAlpha(tone, 0x00));
            }
            sector(g, 0, 0, hub, outer, from, to, top, bottom);
            sector(g, 0, 0, hub, outer, from, to, 0x16FFFFFF, 0x00FFFFFF);
            int edge = on ? 0xFF000000 | tone : 0x1AFFFFFF;
            double thick = on ? 2 : 1;
            sector(g, 0, 0, outer - thick, outer, from, to, edge);
            sector(g, 0, 0, hub, hub + thick, from, to, edge);
            radial(g, hub, outer, from, thick, edge);
            radial(g, hub, outer, to, thick, edge);
            double labelRadius = (hub + outer) / 2;
            int lx = (int) Math.round(labelRadius * Math.sin(middle));
            int ly = (int) Math.round(-labelRadius * Math.cos(middle));
            int ink = usable ? 0xFF000000 | tone : 0xFF5B6B75;
            // The number in a round badge, then the name under it.
            sector(g, lx, ly - 8, 0, 13, 0, 2 * Math.PI, 0x66000000);
            sector(g, lx, ly - 8, 12, 13, 0, 2 * Math.PI, withAlpha(ink, usable ? 0xFF : 0x99));
            g.drawCenteredString(this.font, String.valueOf(i + 1), lx, ly - 12, ink);
            String label = release && releaseArmed ? Tr.s("pet.confirm") : Tr.s("pet.slot." + SLOTS[i].key);
            int nameColor = !usable ? 0xFF5B6B75 : release && releaseArmed ? 0xFFFF7A7A : on ? 0xFFFFFFFF : 0xFFCFE3E8;
            g.drawCenteredString(this.font, label, lx, ly + 10, nameColor);
        }
        drawHub(g);
        g.pose().popPose();
        drawCaption(g, centerY + (outer + 18) * scale);
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** The glass card in the middle: name, owner, life as a number and a bar, and the state as a pill. */
    private void drawHub(GuiGraphics g) {
        double radius = 62;
        sector(g, 0, 0, radius, radius + 8, 0, 2 * Math.PI, withAlpha(FordixPalette.ACCENT, 0x34), withAlpha(FordixPalette.ACCENT, 0x00));
        sector(g, 0, 0, 0, radius, 0, 2 * Math.PI, 0xF2222F3C, 0xF808121A);
        sector(g, 0, 0, 0, radius, 0, 2 * Math.PI, 0x1CFFFFFF, 0x00FFFFFF);
        sector(g, 0, 0, radius - 1, radius, 0, 2 * Math.PI, 0x30FFFFFF);
        int y = -36;
        g.pose().pushPose();
        g.pose().translate(0, y, 0);
        g.pose().scale(1.25F, 1.25F, 1);
        g.drawCenteredString(this.font, this.font.plainSubstrByWidth(pet.getDisplayName().getString(), 88), 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
        UUID ownerId = pet instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
        String owner = ownerId != null && this.minecraft.player != null && ownerId.equals(this.minecraft.player.getUUID())
                ? Tr.s("pet.owner_you") : Tr.s("pet.owner_other");
        g.drawCenteredString(this.font, this.font.plainSubstrByWidth(owner, 110), 0, y + 14, 0xFF9FB4BD);
        String life = "\u2665 " + format(pet.getHealth()) + " / " + format(pet.getMaxHealth());
        g.drawCenteredString(this.font, life, 0, y + 27, 0xFFFF8A7A);
        float left = Math.max(0, Math.min(1, pet.getHealth() / Math.max(1, pet.getMaxHealth())));
        com.omneria.fordix.client.hud.HudStyle.bar(g, -37, y + 40, 74, 5, left, 0xFFE5534B);
        if (pet instanceof TamableAnimal) {
            String state = Tr.s(sitting() ? "pet.state.sitting" : "pet.state.standing");
            int pillWidth = this.font.width(state) + 14;
            com.omneria.fordix.client.gui.Glass.pill(g, this.font, state, -pillWidth / 2, y + 50, FordixPalette.ACCENT);
        }
    }

    /** A straight edge between two radii at the given angle (clockwise from the top), {@code thick} wide. */
    private static void radial(GuiGraphics g, double inner, double outer, double angle, double thick, int argb) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double nx = cos * thick / 2;
        double ny = sin * thick / 2;
        Matrix4f matrix = g.pose().last().pose();
        int a = argb >>> 24 & 255;
        int r = argb >> 16 & 255;
        int gr = argb >> 8 & 255;
        int b = argb & 255;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, (float) (outer * sin - nx), (float) (-outer * cos - ny), 0).color(r, gr, b, a).endVertex();
        buffer.vertex(matrix, (float) (outer * sin + nx), (float) (-outer * cos + ny), 0).color(r, gr, b, a).endVertex();
        buffer.vertex(matrix, (float) (inner * sin - nx), (float) (-inner * cos - ny), 0).color(r, gr, b, a).endVertex();
        buffer.vertex(matrix, (float) (inner * sin + nx), (float) (-inner * cos + ny), 0).color(r, gr, b, a).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }

    private void drawCaption(GuiGraphics g, double y) {
        String text = Tr.s("pet.hint");
        int color = FordixPalette.TEXT;
        if (hovered >= 0) {
            String reason = unavailable(SLOTS[hovered]);
            text = reason != null ? reason : Tr.s("pet.desc." + SLOTS[hovered].key);
            color = reason != null ? 0xFFE8B455 : FordixPalette.TEXT_BRIGHT;
        }
        g.drawCenteredString(this.font, text, this.width / 2, (int) Math.min(this.height - 14, y), color);
    }

    /** The color of each sector's action: follow, stay, feed, inventory, call back, rename, release. */
    private static final int[] ACTION_COLORS = {0xFF3FD1C7, 0xFF9FB3C0, 0xFF5ED68A, 0xFFE8A64A, 0xFF4DA3FF, 0xFFB45FE0, 0xFFE5534B};

    /** Half the wheel's height, in its own units: the span over which a sector's top-to-bottom shading runs. */
    private static final double WHEEL_HALF = 150;

    private static int shade(int top, int bottom, double t) {
        double f = Math.max(0.0D, Math.min(1.0D, t));
        int out = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int a = top >>> shift & 255;
            int b = bottom >>> shift & 255;
            out |= (int) Math.round(a + (b - a) * f) << shift;
        }
        return out;
    }

    private static String format(float value) {
        return value == Math.floor(value) ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** A ring sector (angles in radians, clockwise from the top; inner radius 0 gives a disc) filled with one color. */
    private static void sector(GuiGraphics g, double cx, double cy, double inner, double outer, double from, double to, int argb) {
        sector(g, cx, cy, inner, outer, from, to, argb, argb);
    }

    /** The same, shaded from {@code top} (lit) to {@code bottom} over the wheel's height: the glass of Voidframe 2. */
    private static void sector(GuiGraphics g, double cx, double cy, double inner, double outer, double from, double to, int top, int bottom) {
        Matrix4f matrix = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= STEPS; i++) {
            double angle = from + (to - from) * i / STEPS;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            double yOuter = cy - outer * cos;
            double yInner = cy - inner * cos;
            int cOuter = shade(top, bottom, (yOuter - (cy - WHEEL_HALF)) / (2 * WHEEL_HALF));
            int cInner = shade(top, bottom, (yInner - (cy - WHEEL_HALF)) / (2 * WHEEL_HALF));
            buffer.vertex(matrix, (float) (cx + outer * sin), (float) yOuter, 0)
                    .color(cOuter >> 16 & 255, cOuter >> 8 & 255, cOuter & 255, cOuter >>> 24 & 255).endVertex();
            buffer.vertex(matrix, (float) (cx + inner * sin), (float) yInner, 0)
                    .color(cInner >> 16 & 255, cInner >> 8 & 255, cInner & 255, cInner >>> 24 & 255).endVertex();
        }
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }
}
