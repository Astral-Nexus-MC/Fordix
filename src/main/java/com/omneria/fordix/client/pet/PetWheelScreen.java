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
    public static final int BOX = 224;

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
        scale = com.omneria.fordix.client.gui.vanilla.HudSlots.scale(ID);
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

    private double outerRadius() {
        return BOX / 2.0 - 8;
    }

    private double hubRadius() {
        return outerRadius() * 0.44;
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
        for (int i = 0; i < SLOTS.length; i++) {
            double middle = WheelGeometry.middleOf(i, SLOTS.length);
            boolean usable = unavailable(SLOTS[i]) == null;
            boolean on = i == hovered;
            int tone = ACTION_COLORS[Math.min(i, ACTION_COLORS.length - 1)];
            int fill = on ? (usable ? withAlpha(tone, 0x55) : 0xCC2A3138) : (usable ? 0xDD101C24 : 0xCC0C1318);
            double grow = on && usable ? 6 : 0;
            sector(g, 0, 0, hub + 3, outer + grow, middle - width / 2 + 0.02, middle + width / 2 - 0.02, fill);
            if (on && usable) {
                // Halo and bright rim in the color of the action, like a lit glass edge.
                sector(g, 0, 0, outer + grow, outer + grow + 5, middle - width / 2 + 0.02, middle + width / 2 - 0.02, withAlpha(tone, 0x28));
                sector(g, 0, 0, outer + grow - 2, outer + grow, middle - width / 2 + 0.02, middle + width / 2 - 0.02, 0xFF000000 | tone);
            } else if (usable) {
                sector(g, 0, 0, outer - 1, outer, middle - width / 2 + 0.02, middle + width / 2 - 0.02, withAlpha(tone, 0x50));
            }
            double labelRadius = (hub + outer) / 2 + grow / 2;
            int lx = (int) Math.round(labelRadius * Math.sin(middle));
            int ly = (int) Math.round(-labelRadius * Math.cos(middle));
            int color = !usable ? 0xFF5B6B75 : on ? 0xFFFFFFFF : FordixPalette.TEXT_BRIGHT;
            g.pose().pushPose();
            g.pose().translate(lx - 12, ly - 22, 0);
            g.pose().scale(1.5F, 1.5F, 1);
            if (usable) {
                g.renderItem(SLOTS[i].icon, 0, 0);
            } else {
                RenderSystem.setShaderColor(0.45F, 0.45F, 0.45F, 1);
                g.renderItem(SLOTS[i].icon, 0, 0);
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
            g.pose().popPose();
            String label = i == SLOTS.length - 1 && releaseArmed ? Tr.s("pet.confirm") : Tr.s("pet.slot." + SLOTS[i].key);
            g.drawCenteredString(this.font, label, lx, ly + 6, i == SLOTS.length - 1 && releaseArmed ? 0xFFFF7A7A : color);
            g.drawCenteredString(this.font, String.valueOf(i + 1), lx, ly + 17, usable ? withAlpha(tone, 0xFF) : 0xFF5B6B75);
        }
        drawHub(g, hub);
        g.pose().popPose();
        drawCaption(g, centerY + (outer + 18) * scale);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawHub(GuiGraphics g, double hub) {
        sector(g, 0, 0, 0, hub, 0, 2 * Math.PI, 0xEE0C161D);
        sector(g, 0, 0, 0, hub * 0.55, -0.9, 0.9, 0x14FFFFFF);
        sector(g, 0, 0, hub, hub + 5, 0, 2 * Math.PI, withAlpha(FordixPalette.ACCENT, 0x2C));
        sector(g, 0, 0, hub - 2, hub, 0, 2 * Math.PI, 0xFF000000 | FordixPalette.ACCENT);
        int x = 0;
        int y = -34;
        g.drawCenteredString(this.font, this.font.plainSubstrByWidth(pet.getDisplayName().getString(), (int) hub * 2 - 10), x, y, 0xFFFFFFFF);
        UUID ownerId = pet instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
        String owner = ownerId != null && this.minecraft.player != null && ownerId.equals(this.minecraft.player.getUUID())
                ? Tr.s("pet.owner_you") : Tr.s("pet.owner_other");
        g.drawCenteredString(this.font, this.font.plainSubstrByWidth(owner, (int) hub * 2 - 10), x, y + 12, FordixPalette.TEXT);
        String life = "♥ " + format(pet.getHealth()) + " / " + format(pet.getMaxHealth());
        g.drawCenteredString(this.font, life, x, y + 26, 0xFFFF7A7A);
        float left = Math.max(0, Math.min(1, pet.getHealth() / Math.max(1, pet.getMaxHealth())));
        int barLeft = x - 36;
        g.fill(barLeft, y + 38, barLeft + 72, y + 41, 0x99000000);
        g.fillGradient(barLeft, y + 38, barLeft + Math.round(72 * left), y + 41, 0xFFFFA89E, 0xFFE5534B);
        if (pet instanceof TamableAnimal animal) {
            g.drawCenteredString(this.font, Tr.s(sitting() ? "pet.state.sitting" : "pet.state.standing"), x, y + 46, FordixPalette.ACCENT);
        }
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

    private static String format(float value) {
        return value == Math.floor(value) ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** A ring sector (angles in radians, clockwise from the top; inner radius 0 gives a disc) filled with one color. */
    private static void sector(GuiGraphics g, double cx, double cy, double inner, double outer, double from, double to, int argb) {
        Matrix4f matrix = g.pose().last().pose();
        float a = (argb >>> 24 & 255) / 255.0F;
        float r = (argb >> 16 & 255) / 255.0F;
        float gr = (argb >> 8 & 255) / 255.0F;
        float b = (argb & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= STEPS; i++) {
            double angle = from + (to - from) * i / STEPS;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            buffer.vertex(matrix, (float) (cx + outer * sin), (float) (cy - outer * cos), 0).color(r, gr, b, a).endVertex();
            buffer.vertex(matrix, (float) (cx + inner * sin), (float) (cy - inner * cos), 0).color(r, gr, b, a).endVertex();
        }
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }
}
