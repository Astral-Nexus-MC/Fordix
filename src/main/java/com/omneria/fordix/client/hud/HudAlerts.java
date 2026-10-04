package com.omneria.fordix.client.hud;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

import java.util.function.Predicate;

/**
 * Warnings shown above the hotbar (and with a sound): an item about to break, a bow without arrows, a server whose tick
 * rate collapsed. Each fires once when the situation starts and again only after it went away.
 */
public final class HudAlerts {
    private static final double LOW_TPS = 12.0D;
    private static final int LOW_TPS_TICKS = 100;
    private static final AlertLatch LATCH = new AlertLatch();
    private static int lowTpsTicks;

    private HudAlerts() {
    }

    public static void reset() {
        LATCH.clear();
        lowTpsTicks = 0;
    }

    /** Every client tick. */
    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !FordixConfig.VOIDFRAME_HUD.get()) {
            return;
        }
        float threshold = FordixConfig.ALERT_DURABILITY_PERCENT.get() / 100.0F;
        boolean durability = FordixConfig.hudAlert("alert_durability");
        for (int slot = 0; slot < 6; slot++) {
            ItemStack stack = slot == 0 ? player.getMainHandItem() : slot == 1 ? player.getOffhandItem() : player.getInventory().armor.get(slot - 2);
            boolean low = durability && !stack.isEmpty() && stack.isDamageableItem()
                    && HudFormat.durabilityLeft(stack.getDamageValue(), stack.getMaxDamage()) < threshold;
            if (LATCH.rise("durability" + slot, low)) {
                warn(minecraft, Tr.s("hud.alert.durability", stack.getHoverName().getString()));
            }
        }
        boolean noAmmo = false;
        if (FordixConfig.hudAlert("alert_ammo") && !player.getAbilities().instabuild) {
            for (ItemStack held : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
                if (held.getItem() instanceof ProjectileWeaponItem weapon && !hasAmmo(player, weapon)) {
                    noAmmo = true;
                }
            }
        }
        if (LATCH.rise("ammo", noAmmo)) {
            warn(minecraft, Tr.s("hud.alert.ammo"));
        }
        double tps = minecraft.hasSingleplayerServer() ? -1.0D : InfoHud.tps(minecraft);
        lowTpsTicks = FordixConfig.hudAlert("alert_tps") && tps >= 0 && tps < LOW_TPS ? lowTpsTicks + 1 : 0;
        if (LATCH.rise("tps", lowTpsTicks >= LOW_TPS_TICKS)) {
            warn(minecraft, Tr.s("hud.alert.tps", String.format(java.util.Locale.ROOT, "%.1f", tps)));
        }
    }

    private static boolean hasAmmo(LocalPlayer player, ProjectileWeaponItem weapon) {
        Predicate<ItemStack> supported = weapon.getAllSupportedProjectiles();
        for (ItemStack stack : player.getInventory().items) {
            if (supported.test(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (supported.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void warn(Minecraft minecraft, String message) {
        minecraft.gui.setOverlayMessage(net.minecraft.network.chat.Component.literal("⚠ " + message)
                .withStyle(net.minecraft.ChatFormatting.RED), false);
        if (FordixConfig.hudAlert("alert_sound")) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.get(), 0.7F, 1.0F));
        }
    }
}
