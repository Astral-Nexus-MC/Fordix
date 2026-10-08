package com.omneria.fordix.client.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HudProfileCodecTest {
    @Test
    void roundTrip() {
        Map<String, HudProfiles.Placement> layout = new LinkedHashMap<>();
        layout.put("health", new HudProfiles.Placement("top_left", 3, -4, true, 1.5F));
        layout.put("fordix:coordinates", new HudProfiles.Placement());
        String code = HudProfileCodec.encode(layout);
        assertTrue(code.startsWith("FXH1-"));
        assertTrue(code.length() < 400);
        Map<String, HudProfiles.Placement> back = HudProfileCodec.decode("  " + code + "\n");
        assertEquals(2, back.size());
        assertEquals("top_left", back.get("health").anchor);
        assertEquals(-4, back.get("health").offsetY);
        assertTrue(back.get("health").hidden);
        assertEquals(1.5F, back.get("health").scale, 0.001F);
    }

    @Test
    void refusesWhatIsNotACode() {
        assertNull(HudProfileCodec.decode(null));
        assertNull(HudProfileCodec.decode("hello"));
        assertNull(HudProfileCodec.decode("FXH1-%%%"));
        assertNull(HudProfileCodec.decode("FXH1-" + java.util.Base64.getUrlEncoder().encodeToString("not gzip".getBytes())));
    }

    @Test
    void refusesAbsurdPlacements() {
        Map<String, HudProfiles.Placement> layout = new LinkedHashMap<>();
        layout.put("health", new HudProfiles.Placement("default", 99999, 0, false, 1.0F));
        assertNull(HudProfileCodec.decode(HudProfileCodec.encode(layout)));
    }
}
