package com.omneria.fordix.wiki;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WikiChunksTest {
    @Test
    void cutsAndJoinsInAnyOrder() {
        byte[] data = new byte[WikiChunks.SIZE * 2 + 10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 7);
        }
        List<byte[]> chunks = WikiChunks.split(data);
        assertEquals(3, chunks.size());
        WikiChunks.Assembly assembly = new WikiChunks.Assembly(3);
        assertFalse(assembly.add(2, chunks.get(2)));
        assertFalse(assembly.add(0, chunks.get(0)));
        assertFalse(assembly.add(0, chunks.get(0)));
        assertTrue(assembly.add(1, chunks.get(1)));
        assertArrayEquals(data, assembly.join());
    }

    @Test
    void anEmptyAnswerIsOneEmptyPart() {
        List<byte[]> chunks = WikiChunks.split(new byte[0]);
        assertEquals(1, chunks.size());
        assertTrue(new WikiChunks.Assembly(1).add(0, chunks.get(0)));
    }
}
