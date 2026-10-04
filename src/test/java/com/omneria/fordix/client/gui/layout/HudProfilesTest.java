package com.omneria.fordix.client.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HudProfilesTest {
    @TempDir
    Path dir;

    @Test
    void savedSlotsSurviveAReloadAndTheKeyCyclesThroughFilledOnes() {
        Path file = dir.resolve("profiles.json");
        HudProfiles profiles = new HudProfiles(file);
        assertEquals(-1, profiles.next());
        profiles.put(0, Map.of("health", new HudProfiles.Placement("top_left", 3, 4, true, 1.5F)));
        profiles.put(2, Map.of());
        HudProfiles reloaded = new HudProfiles(file);
        assertTrue(reloaded.has(0));
        assertFalse(reloaded.has(1));
        HudProfiles.Placement health = reloaded.get(0).get("health");
        assertEquals("top_left", health.anchor);
        assertEquals(1.5F, health.scale, 0.001F);
        assertTrue(health.hidden);
        assertEquals(2, reloaded.active());
        assertEquals(0, reloaded.next());
        reloaded.setActive(0);
        assertEquals(2, reloaded.next());
    }
}
