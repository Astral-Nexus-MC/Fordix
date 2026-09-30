package com.omneria.fordix.client.gui.vanilla;

import com.mojang.blaze3d.platform.NativeImage;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Vanilla screens that Fordix doesn't redraw (the recipe book, loom, cartography table, vanilla buttons) draw from
 * texture atlases. Their grey pixels are recolored with the current theme and registered
 * over the vanilla textures, so those screens follow the Voidframe look without being
 * reimplemented. Redone after every resource reload and every theme change; switching
 * "voidframeVanillaScreens" off puts the vanilla textures back.
 */
public final class TextureRecolor {
    /** A texture and the rows of it to recolor (the rest is left as vanilla drew it). */
    private record Target(ResourceLocation location, int fromRow, int toRow, boolean flat) {
        Target(String path) {
            this(new ResourceLocation(path), 0, Integer.MAX_VALUE, false);
        }

        Target(ResourceLocation location, int fromRow, int toRow) {
            this(location, fromRow, toRow, false);
        }
    }

    private static final List<Target> TARGETS = List.of(
            new Target("textures/gui/recipe_book.png"),
            // Track and handle of the option sliders (FOV, volume...).
            new Target("textures/gui/slider.png"),
            // The tiled backdrop of the options, world list and mod list screens.
            new Target(new ResourceLocation("textures/gui/options_background.png"), 0, Integer.MAX_VALUE, true),
            new Target("textures/gui/container/loom.png"),
            new Target("textures/gui/container/cartography_table.png"),
            new Target("textures/gui/container/creative_inventory/tab_items.png"),
            new Target("textures/gui/container/creative_inventory/tab_inventory.png"),
            new Target("textures/gui/container/creative_inventory/tab_item_search.png"),
            new Target("textures/gui/container/creative_inventory/tabs.png"),
            // Only the button rows of widgets.png: the hotbar and its selection frame share the file.
            new Target(new ResourceLocation("textures/gui/widgets.png"), 46, 106));

    private static boolean ready;

    /** Registered on the client reload event; the textures only exist once resources have loaded. */
    public static final ResourceManagerReloadListener RELOAD_LISTENER = manager -> {
        ready = true;
        apply(Minecraft.getInstance());
    };

    private TextureRecolor() {
    }

    /** Re-applies the theme; safe from any thread and before the first resource load. */
    public static void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(() -> apply(minecraft));
        }
    }

    /**
     * Vanilla preloads widgets.png and registers its own texture once the reload is over, which
     * can be after our listener ran and wipes the recolor: put it back as soon as that happens.
     */
    public static void onClientTick(Minecraft minecraft) {
        if (!ready || minecraft.getTextureManager() == null) {
            return;
        }
        try {
            if (FordixConfig.VOIDFRAME_VANILLA_SCREENS.get()
                    && !(minecraft.getTextureManager().getTexture(TARGETS.get(TARGETS.size() - 1).location()) instanceof DynamicTexture)) {
                apply(minecraft);
            }
        } catch (IllegalStateException notLoadedYet) {
            // Config not loaded yet: nothing to do.
        }
    }

    private static void apply(Minecraft minecraft) {
        if (!ready) {
            return;
        }
        boolean voidframe;
        try {
            voidframe = FordixConfig.VOIDFRAME_VANILLA_SCREENS.get();
        } catch (IllegalStateException notLoadedYet) {
            return;
        }
        ResourceManager resources = minecraft.getResourceManager();
        TextureManager textures = minecraft.getTextureManager();
        int[] ramp = {FordixPalette.BEVEL_SHADOW, FordixPalette.SLOT_FILL, FordixPalette.BUTTON_FILL,
                FordixPalette.PANEL_FILL, FordixPalette.BEVEL_HIGHLIGHT};
        for (Target target : TARGETS) {
            ResourceLocation location = target.location();
            try {
                if (voidframe) {
                    Optional<Resource> resource = resources.getResource(location);
                    if (resource.isEmpty()) {
                        continue;
                    }
                    try (InputStream stream = resource.get().open()) {
                        NativeImage image = NativeImage.read(stream);
                        recolor(image, ramp, target.fromRow(), target.toRow(), target.flat());
                        textures.register(location, new DynamicTexture(image));
                    }
                } else {
                    textures.register(location, new SimpleTexture(location));
                }
            } catch (Exception e) {
                Fordix.LOGGER.warn("Could not recolor {}", location, e);
            }
        }
    }

    private static void recolor(NativeImage image, int[] ramp, int fromRow, int toRow, boolean flat) {
        for (int y = fromRow; y < Math.min(image.getHeight(), toRow); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                // NativeImage stores ABGR; Duotone works on ARGB.
                int argb = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
                int out = Duotone.recolor(argb, ramp);
                if (flat) {
                    // A quiet texture: mostly the slot color, the original grain only faintly visible.
                    out = blend(out, FordixPalette.SLOT_FILL, 0.8F);
                }
                image.setPixelRGBA(x, y, (out & 0xFF00FF00) | ((out & 0xFF) << 16) | ((out >> 16) & 0xFF));
            }
        }
    }

    private static int blend(int from, int to, float amount) {
        int r = Math.round(((from >> 16) & 0xFF) * (1 - amount) + ((to >> 16) & 0xFF) * amount);
        int g = Math.round(((from >> 8) & 0xFF) * (1 - amount) + ((to >> 8) & 0xFF) * amount);
        int b = Math.round((from & 0xFF) * (1 - amount) + (to & 0xFF) * amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
