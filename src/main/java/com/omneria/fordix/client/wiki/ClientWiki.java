package com.omneria.fordix.client.wiki;

import com.mojang.blaze3d.platform.NativeImage;
import com.omneria.fordix.panel.network.PanelNetwork;
import com.omneria.fordix.wiki.ClientboundWikiPayload;
import com.omneria.fordix.wiki.ServerboundWikiPayload;
import com.omneria.fordix.wiki.WikiAnswers;
import com.omneria.fordix.wiki.WikiChunks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the client knows of the server's in-game wikis. Everything is fetched on demand (and cached for the session): the
 * list of wikis arrives at login, an index, a page, a search or a picture when a screen asks for it. A screen simply asks
 * every frame and gets the cached answer, or null while it is on its way ({@link #version} tells it when something new came).
 */
public final class ClientWiki {
    /** A picture ready to draw, with the size of its texture; {@code location} is null for a picture that could not be loaded. */
    public record Picture(ResourceLocation location, int width, int height) {
    }

    private static final long RETRY_MS = 8000L;
    private static volatile List<WikiAnswers.Info> wikis = List.of();
    private static final Map<String, List<WikiAnswers.Entry>> INDEXES = new HashMap<>();
    private static final Map<String, WikiAnswers.Page> PAGES = new HashMap<>();
    private static final Map<String, List<WikiAnswers.Hit>> SEARCHES = new HashMap<>();
    private static final Map<String, Picture> PICTURES = new HashMap<>();
    private static final Map<String, WikiChunks.Assembly> ASSEMBLIES = new HashMap<>();
    private static final Map<String, Long> ASKED = new HashMap<>();
    private static final WikiAnswers.Page MISSING_PAGE = new WikiAnswers.Page("", "", "", "");
    private static int version;
    private static int pictureCount;

    private ClientWiki() {
    }

    /** Goes up each time an answer arrives: screens that cache a layout can compare it. */
    public static int version() {
        return version;
    }

    public static List<WikiAnswers.Info> wikis() {
        return wikis;
    }

    public static WikiAnswers.Info wiki(String id) {
        for (WikiAnswers.Info info : wikis) {
            if (info.id().equals(id)) {
                return info;
            }
        }
        return null;
    }

    /** Whether the server has this wiki with at least one page: a guide tab of that mod then opens it in the game. */
    public static boolean hasWiki(String id) {
        WikiAnswers.Info info = wiki(id);
        return info != null && info.pages() > 0;
    }

    private static boolean ask(ServerboundWikiPayload.Kind kind, String wiki, String key) {
        String id = kind + "\n" + wiki + "\n" + key;
        long now = System.currentTimeMillis();
        Long before = ASKED.get(id);
        if (before != null && now - before < RETRY_MS) {
            return false;
        }
        if (Minecraft.getInstance().getConnection() == null) {
            return false;
        }
        ASKED.put(id, now);
        PanelNetwork.CHANNEL.sendToServer(new ServerboundWikiPayload(kind, wiki, key));
        return true;
    }

    public static List<WikiAnswers.Entry> index(String wiki) {
        List<WikiAnswers.Entry> index = INDEXES.get(wiki);
        if (index == null) {
            ask(ServerboundWikiPayload.Kind.INDEX, wiki, "");
        }
        return index;
    }

    /** The page, or null while it loads; a page the server doesn't have comes back with an empty body and title. */
    public static WikiAnswers.Page page(String wiki, String slug) {
        WikiAnswers.Page page = PAGES.get(wiki + "/" + slug);
        if (page == null) {
            ask(ServerboundWikiPayload.Kind.PAGE, wiki, slug);
        }
        return page;
    }

    public static boolean isMissing(WikiAnswers.Page page) {
        return page == MISSING_PAGE;
    }

    /** Pages matching the words (in one wiki, or in all when {@code wiki} is empty), or null while the answer is on its way. */
    public static List<WikiAnswers.Hit> search(String wiki, String query) {
        List<WikiAnswers.Hit> hits = SEARCHES.get(wiki + "\n" + query);
        if (hits == null) {
            ask(ServerboundWikiPayload.Kind.SEARCH, wiki, query);
        }
        return hits;
    }

    /** A picture of the server's images folder, or null while it loads. */
    public static Picture picture(String name) {
        Picture picture = PICTURES.get(name);
        if (picture == null) {
            ask(ServerboundWikiPayload.Kind.IMAGE, "", name);
        }
        return picture;
    }

    public static void requestList() {
        ask(ServerboundWikiPayload.Kind.LIST, "", "");
    }

    /** Logging out: nothing of this server is kept. */
    public static void clear() {
        wikis = List.of();
        INDEXES.clear();
        PAGES.clear();
        SEARCHES.clear();
        ASSEMBLIES.clear();
        ASKED.clear();
        for (Picture picture : PICTURES.values()) {
            if (picture.location() != null) {
                Minecraft.getInstance().getTextureManager().release(picture.location());
            }
        }
        PICTURES.clear();
        version++;
    }

    // ---- answers

    public static void receive(ClientboundWikiPayload payload) {
        String id = payload.kind() + "\n" + payload.wiki() + "\n" + payload.key();
        WikiChunks.Assembly assembly = ASSEMBLIES.computeIfAbsent(id, key -> new WikiChunks.Assembly(payload.parts()));
        if (!assembly.add(payload.part(), payload.data())) {
            return;
        }
        ASSEMBLIES.remove(id);
        byte[] data = assembly.join();
        switch (payload.kind()) {
            case LIST -> wikis = WikiAnswers.parseList(text(data));
            case INDEX -> INDEXES.put(payload.wiki(), WikiAnswers.parseIndex(text(data)));
            case PAGE -> PAGES.put(payload.wiki() + "/" + payload.key(), data.length == 0 ? MISSING_PAGE : WikiAnswers.parsePage(text(data)));
            case SEARCH -> SEARCHES.put(payload.wiki() + "\n" + payload.key(), WikiAnswers.parseHits(text(data)));
            case IMAGE -> PICTURES.put(payload.key(), load(data));
        }
        // An index asked before the list changed (a page imported meanwhile) is asked again next time.
        if (payload.kind() == ServerboundWikiPayload.Kind.LIST) {
            INDEXES.clear();
            SEARCHES.clear();
            PAGES.clear();
            ASKED.clear();
        }
        version++;
    }

    private static String text(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    private static Picture load(byte[] png) {
        if (png.length == 0) {
            return new Picture(null, 0, 0);
        }
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(png))) {
            ResourceLocation location = new ResourceLocation("fordix", "wiki/picture_" + pictureCount++);
            int width = image.getWidth();
            int height = image.getHeight();
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(copy(image)));
            return new Picture(location, width, height);
        } catch (IOException | RuntimeException e) {
            return new Picture(null, 0, 0);
        }
    }

    /** The texture keeps its own copy: the decoded image is closed right after. */
    private static NativeImage copy(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), false);
        copy.copyFrom(source);
        return copy;
    }
}
