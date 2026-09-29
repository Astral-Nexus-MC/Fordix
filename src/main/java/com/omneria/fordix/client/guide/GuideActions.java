package com.omneria.fordix.client.guide;

import com.omneria.fordix.Fordix;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Runs a guide tab's action. Create's Ponder and Patchouli are reached by class name
 * rather than compiled against, so Fordix has no dependency on either mod and a
 * missing or moved class just shows a message instead of crashing.
 */
public final class GuideActions {
    /** Ponder's index screen: its own library since Create 6, inside Create before that. */
    private static final List<String> PONDER_INDEX_CLASSES = List.of(
            "net.createmod.ponder.foundation.ui.PonderIndexScreen",
            "com.simibubi.create.foundation.ponder.ui.PonderIndexScreen"
    );

    private GuideActions() {
    }

    public static void run(GuideEntry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (entry.action()) {
            case PONDER -> openPonder(minecraft, entry);
            case PATCHOULI -> openPatchouli(minecraft, entry);
            case URL -> openUrl(minecraft, entry.target());
            case COMMAND -> {
                if (minecraft.player != null) {
                    minecraft.player.connection.sendCommand(entry.target());
                }
            }
        }
    }

    private static void openPonder(Minecraft minecraft, GuideEntry entry) {
        for (String className : PONDER_INDEX_CLASSES) {
            try {
                Class<?> type = Class.forName(className);
                minecraft.setScreen((Screen) type.getConstructor().newInstance());
                return;
            } catch (ClassNotFoundException e) {
                // Try the next known location.
            } catch (ReflectiveOperationException | ClassCastException e) {
                Fordix.LOGGER.warn("Fordix: could not open Ponder index {}", className, e);
            }
        }
        unavailable(minecraft, entry);
    }

    private static void openPatchouli(Minecraft minecraft, GuideEntry entry) {
        ResourceLocation book = ResourceLocation.tryParse(entry.target());
        if (book == null) {
            unavailable(minecraft, entry);
            return;
        }
        try {
            Object api = Class.forName("vazkii.patchouli.api.PatchouliAPI").getMethod("get").invoke(null);
            api.getClass().getMethod("openBookGUI", ResourceLocation.class).invoke(api, book);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Fordix.LOGGER.warn("Fordix: could not open Patchouli book {}", book, e);
            unavailable(minecraft, entry);
        }
    }

    /** Vanilla's "open this link?" prompt, then back to the screen the player was on. */
    private static void openUrl(Minecraft minecraft, String url) {
        Screen previous = minecraft.screen;
        minecraft.setScreen(new ConfirmLinkScreen(accepted -> {
            if (accepted) {
                Util.getPlatform().openUri(url);
            }
            minecraft.setScreen(previous);
        }, url, true));
    }

    private static void unavailable(Minecraft minecraft, GuideEntry entry) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("Guide '" + entry.label() + "' indisponible."), true);
        }
    }
}
