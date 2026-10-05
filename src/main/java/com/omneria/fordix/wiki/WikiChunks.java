package com.omneria.fordix.wiki;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Cuts a long answer into packets of at most {@link #SIZE} bytes and puts them back together. Pure Java, unit tested. */
public final class WikiChunks {
    public static final int SIZE = 24_000;
    public static final int MAX_PARTS = 64;

    private WikiChunks() {
    }

    /** At least one chunk, even for no data (an empty answer is still an answer). */
    public static List<byte[]> split(byte[] data) {
        List<byte[]> chunks = new ArrayList<>();
        if (data.length == 0) {
            chunks.add(new byte[0]);
            return chunks;
        }
        for (int at = 0; at < data.length && chunks.size() < MAX_PARTS; at += SIZE) {
            int end = Math.min(data.length, at + SIZE);
            byte[] chunk = new byte[end - at];
            System.arraycopy(data, at, chunk, 0, chunk.length);
            chunks.add(chunk);
        }
        return chunks;
    }

    /** The parts of one answer as they arrive, in any order. */
    public static final class Assembly {
        private final byte[][] parts;
        private int received;

        public Assembly(int count) {
            this.parts = new byte[Math.max(1, Math.min(MAX_PARTS, count))][];
        }

        /** Adds a part; true when the answer is complete. */
        public boolean add(int index, byte[] data) {
            if (index >= 0 && index < parts.length && parts[index] == null) {
                parts[index] = data;
                received++;
            }
            return received == parts.length;
        }

        public byte[] join() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] part : parts) {
                if (part != null) {
                    out.writeBytes(part);
                }
            }
            return out.toByteArray();
        }
    }
}
