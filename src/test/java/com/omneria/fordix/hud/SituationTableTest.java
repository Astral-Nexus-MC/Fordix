package com.omneria.fordix.hud;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class SituationTableTest {
    @Test
    void parsesARow() {
        SituationTable.Row row = SituationTable.parseRow("coordinates | 100 | 30 | 100 | 30 | 100 %");
        assertEquals("coordinates", row.panel());
        assertArrayEquals(new float[]{1.0F, 0.3F, 1.0F, 0.3F, 1.0F}, row.factors(), 0.001F);
    }

    @Test
    void refusesBadRows() {
        assertNull(SituationTable.parseRow("coordinates | 100 | 30"));
        assertNull(SituationTable.parseRow("coordinates | 100 | 30 | 100 | 30 | 101"));
        assertNull(SituationTable.parseRow("coordinates | a | 30 | 100 | 30 | 100"));
        assertNull(SituationTable.parseRow(" | 1 | 1 | 1 | 1 | 1"));
        assertNull(SituationTable.parseRow(42));
    }

    @Test
    void parsesProfilesPerSituation() {
        String[] codes = SituationTable.profiles(List.of("Combat | FXH1-abc", "nonsense | FXH1-x", "boss | "));
        assertEquals("", codes[0]);
        assertEquals("FXH1-abc", codes[1]);
        assertEquals("", codes[3]);
        assertNull(SituationTable.parseProfile("sleep | FXH1-x"));
    }
}
