package com.omneria.fordix.panel;

import com.omneria.fordix.panel.network.ServerboundPanelActionPayload;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PanelActionPayloadTest {
    @Test
    void aClickSurvivesTheRoundTrip() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ServerboundPanelActionPayload.encode(new ServerboundPanelActionPayload("shop", 3), buf);
        ServerboundPanelActionPayload back = ServerboundPanelActionPayload.decode(buf);
        assertEquals("shop", back.panelId());
        assertEquals(3, back.buttonIndex());
    }

    @Test
    void anIdLongerThanTheLimitIsRefused() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("x".repeat(ServerboundPanelActionPayload.MAX_ID + 1));
        buf.writeVarInt(0);
        assertThrows(DecoderException.class, () -> ServerboundPanelActionPayload.decode(buf));
    }
}
