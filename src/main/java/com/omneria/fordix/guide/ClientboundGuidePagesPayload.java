package com.omneria.fordix.guide;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/** Server to client: the server's guide pages, all of them, replacing what the client had. */
public record ClientboundGuidePagesPayload(List<GuidePage> pages) {
    public static final int MAX_PAGES = 64;

    public static void encode(ClientboundGuidePagesPayload payload, FriendlyByteBuf buf) {
        buf.writeCollection(payload.pages(), (b, page) -> {
            b.writeUtf(page.id, 64);
            b.writeUtf(page.title, 128);
            b.writeUtf(page.author, 64);
            b.writeLong(page.updatedAt);
            b.writeUtf(page.body, GuidePage.MAX_BODY + 64);
        });
    }

    public static ClientboundGuidePagesPayload decode(FriendlyByteBuf buf) {
        List<GuidePage> pages = buf.readList(b -> {
            String id = b.readUtf(64);
            String title = b.readUtf(128);
            String author = b.readUtf(64);
            long updatedAt = b.readLong();
            String body = b.readUtf(GuidePage.MAX_BODY + 64);
            return new GuidePage(id, title, body, author, updatedAt);
        });
        return new ClientboundGuidePagesPayload(pages.size() > MAX_PAGES ? pages.subList(0, MAX_PAGES) : pages);
    }
}
