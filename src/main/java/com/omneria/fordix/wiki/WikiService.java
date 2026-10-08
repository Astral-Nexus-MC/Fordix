package com.omneria.fordix.wiki;

import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Answers the clients' wiki requests from the server's {@link WikiStore} and {@link ImageStore}. Lists and pages are small
 * and answered at once; pictures are read, scaled and encoded on a worker thread. A player may ask only so often.
 */
public final class WikiService {
    private static final int SEARCH_LIMIT = 40;
    private static final int MAX_REQUESTS = 40;
    private static final long WINDOW_MS = 2000L;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "fordix-wiki");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<UUID, long[]> REQUESTS = new HashMap<>();
    private static WikiStore store;
    private static ImageStore images;

    private WikiService() {
    }

    private static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve("fordix");
    }

    public static synchronized WikiStore store() {
        if (store == null) {
            store = new WikiStore(root().resolve("wiki"));
        }
        return store;
    }

    public static synchronized ImageStore images() {
        if (images == null) {
            images = new ImageStore(root().resolve("images"));
        }
        return images;
    }

    private static boolean allowed(ServerPlayer player) {
        long now = System.currentTimeMillis();
        synchronized (REQUESTS) {
            long[] window = REQUESTS.computeIfAbsent(player.getUUID(), id -> new long[]{now, 0});
            if (now - window[0] > WINDOW_MS) {
                window[0] = now;
                window[1] = 0;
            }
            return ++window[1] <= MAX_REQUESTS;
        }
    }

    public static void handle(ServerboundWikiPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && allowed(player)) {
                answer(player, payload);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static String clean(String text) {
        return text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    /** The kind of page for the list: the one stored with the page, else what the mod of that name registers under the page's name. */
    static String categoryOf(String wiki, WikiStore.Entry entry) {
        if (!entry.category().isEmpty()) {
            return entry.category();
        }
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(wiki + ":" + entry.slug().toLowerCase(java.util.Locale.ROOT));
        if (id == null) {
            return "";
        }
        if (net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            return "mob";
        }
        if (net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(id)) {
            return "block";
        }
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(id) ? "item" : "";
    }

    private static void answer(ServerPlayer player, ServerboundWikiPayload request) {
        WikiStore wikis = store();
        switch (request.kind()) {
            case LIST -> sendList(player);
            case INDEX -> {
                StringBuilder out = new StringBuilder();
                for (WikiStore.Entry entry : wikis.index(request.wiki())) {
                    out.append(clean(entry.slug())).append('\t').append(clean(entry.title())).append('\t')
                            .append(clean(categoryOf(request.wiki(), entry))).append('\n');
                }
                reply(player, request, out.toString().getBytes(StandardCharsets.UTF_8));
            }
            case PAGE -> {
                WikiPage page = wikis.resolve(request.wiki(), request.key());
                WikiInfo info = wikis.info(request.wiki());
                byte[] data = page == null ? new byte[0]
                        : (clean(page.title) + "\n" + clean(page.source) + "\n" + clean(info == null ? "" : info.credit()) + "\n---\n"
                        + page.body + related(wikis, request.wiki(), page)).getBytes(StandardCharsets.UTF_8);
                reply(player, request, data);
            }
            case SEARCH -> {
                StringBuilder out = new StringBuilder();
                for (WikiSearch.Hit hit : wikis.search(request.wiki(), request.key(), SEARCH_LIMIT)) {
                    out.append(clean(hit.wiki())).append('\t').append(clean(hit.slug())).append('\t').append(clean(hit.title()))
                            .append('\t').append(clean(hit.snippet())).append('\n');
                }
                reply(player, request, out.toString().getBytes(StandardCharsets.UTF_8));
            }
            case LOOT -> reply(player, request, loot(player, request.key()).getBytes(StandardCharsets.UTF_8));
            case IMAGE -> WORKER.execute(() -> {
                byte[] png = images().png(request.key());
                reply(player, request, png == null ? new byte[0] : png);
            });
        }
    }

    /** The drops of a creature type (its id, "minecraft:zombie"), read from its loot table as the server has it; empty when it has none. */
    static String loot(ServerPlayer player, String entityId) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(entityId);
            if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                return "";
            }
            ResourceLocation tableId = ForgeRegistries.ENTITY_TYPES.getValue(id).getDefaultLootTable();
            LootTable table = player.server.getLootData().getLootTable(tableId);
            return LootSummary.toLines(LootSummary.parse(LootDataType.TABLE.parser().toJsonTree(table)));
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** "Linked pages": a button to each page this one links to that the wiki has (at most 20). */
    static String related(WikiStore wikis, String wiki, WikiPage page) {
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (String link : page.links == null ? List.<String>of() : page.links) {
            WikiPage target = link.equals(page.slug) ? null : wikis.page(wiki, link);
            if (target != null && count++ < 20) {
                out.append(count == 1 ? "\n\n## \u00A7related\n" : "\n").append("@link ").append(target.slug).append(" | ").append(clean(target.title));
            }
        }
        return out.toString();
    }

    /** The wikis this server has: one line each, "id, title, pages, source, licence". */
    public static void sendList(ServerPlayer player) {
        StringBuilder out = new StringBuilder();
        for (WikiInfo info : store().wikis()) {
            out.append(clean(info.id)).append('\t').append(clean(info.title)).append('\t').append(store().count(info.id)).append('\t')
                    .append(clean(info.source)).append('\t').append(clean(info.credit())).append('\n');
        }
        reply(player, new ServerboundWikiPayload(ServerboundWikiPayload.Kind.LIST, "", ""), out.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void broadcastList(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(WikiService::sendList);
    }

    private static void reply(ServerPlayer player, ServerboundWikiPayload request, byte[] data) {
        List<byte[]> chunks = WikiChunks.split(data);
        for (int i = 0; i < chunks.size(); i++) {
            PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new ClientboundWikiPayload(request.kind(), request.wiki(), request.key(), i, chunks.size(), chunks.get(i)));
        }
    }

    public static void forget(UUID player) {
        synchronized (REQUESTS) {
            REQUESTS.remove(player);
        }
    }
}
