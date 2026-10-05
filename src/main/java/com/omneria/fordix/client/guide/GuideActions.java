package com.omneria.fordix.client.guide;

import com.omneria.fordix.Tr;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.client.gui.WikiScreen;
import com.omneria.fordix.client.wiki.ClientWiki;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
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
        run(entry, net.minecraft.world.item.ItemStack.EMPTY);
    }

    /** Runs the entry's action; {@code stack} is the item the player was on (a link can name its wiki page). */
    public static void run(GuideEntry entry, net.minecraft.world.item.ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation item = stack.isEmpty() ? null : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        // A guide that links to a web wiki opens the server's own copy in the game when it has one (a wiki named after the mod).
        if (entry.action() == GuideEntry.Action.URL && ClientWiki.hasWiki(entry.modId())) {
            openWiki(minecraft, entry.modId(), item == null ? null : GuideEntry.wikiPage(item.getPath()));
            return;
        }
        switch (entry.action()) {
            case WIKI -> openWiki(minecraft, entry.target(), item == null ? null : GuideEntry.wikiPage(item.getPath()));
            case PONDER -> openPonder(minecraft, entry);
            case PATCHOULI -> openPatchouli(minecraft, entry);
            case URL -> {
                ResourceLocation id = stack.isEmpty() ? null : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                openUrl(minecraft, GuideEntry.resolveUrl(entry.target(), id == null ? null : id.getPath()));
            }
            case COMMAND -> {
                if (minecraft.player != null) {
                    minecraft.player.connection.sendCommand(entry.target());
                }
            }
        }
    }

    private static void openWiki(Minecraft minecraft, String wiki, String slug) {
        minecraft.setScreen(new WikiScreen(minecraft.screen, wiki, slug));
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
            minecraft.player.displayClientMessage(Tr.c("guides.unavailable", entry.label()), true);
        }
    }
}
