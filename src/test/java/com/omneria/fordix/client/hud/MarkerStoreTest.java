package com.omneria.fordix.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkerStoreTest {
    @TempDir
    Path dir;

    @Test
    void markersSurviveAReload() {
        Path file = dir.resolve("markers.json");
        MarkerStore store = new MarkerStore(file);
        store.set("world|overworld", new MarkerStore.Marker(10, 64, -20));
        assertEquals(new MarkerStore.Marker(10, 64, -20), new MarkerStore(file).get("world|overworld"));
        store.remove("world|overworld");
        assertNull(new MarkerStore(file).get("world|overworld"));
    }
}
