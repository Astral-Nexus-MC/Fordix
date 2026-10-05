package com.omneria.fordix.wiki;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootSummaryTest {
    private static List<LootSummary.Drop> parse(String json) {
        return LootSummary.parse(JsonParser.parseString(json));
    }

    @Test
    void zombieLikeTable() {
        List<LootSummary.Drop> drops = parse("""
                {"pools":[
                 {"rolls":1.0,"entries":[{"type":"minecraft:item","name":"minecraft:rotten_flesh","functions":[
                   {"function":"minecraft:set_count","count":{"min":0.0,"max":2.0,"type":"minecraft:uniform"}},
                   {"function":"minecraft:looting_enchant","count":{"min":0.0,"max":1.0}}]}]},
                 {"rolls":1.0,"conditions":[{"condition":"minecraft:killed_by_player"},
                   {"condition":"minecraft:random_chance_with_looting","chance":0.025,"looting_multiplier":0.01}],
                  "entries":[{"type":"minecraft:item","name":"minecraft:iron_ingot"}]}]}
                """);
        assertEquals(2, drops.size());
        LootSummary.Drop flesh = drops.get(0);
        assertEquals("minecraft:rotten_flesh", flesh.item());
        assertEquals(0, flesh.min());
        assertEquals(2, flesh.max());
        assertEquals(100.0, flesh.chance());
        assertTrue(flesh.flags().contains(LootSummary.LOOTING));
        LootSummary.Drop iron = drops.get(1);
        assertEquals(1, iron.min());
        assertEquals(2.5, iron.chance());
        assertTrue(iron.flags().contains(LootSummary.PLAYER_KILL));
    }

    @Test
    void weightsShareAPool() {
        List<LootSummary.Drop> drops = parse("""
                {"pools":[{"rolls":1,"entries":[
                 {"type":"minecraft:item","name":"a:x","weight":3},
                 {"type":"minecraft:item","name":"a:y","weight":1}]}]}
                """);
        assertEquals(75.0, drops.get(0).chance());
        assertEquals(25.0, drops.get(1).chance());
    }

    @Test
    void rollsMultiplyTheCountAndChildrenAreListed() {
        List<LootSummary.Drop> drops = parse("""
                {"pools":[{"rolls":{"min":2,"max":3},"entries":[
                 {"type":"minecraft:alternatives","children":[
                   {"type":"minecraft:tag","name":"forge:ores","conditions":[{"condition":"minecraft:weather_check"}]},
                   {"type":"minecraft:loot_table","name":"a:other"}]}]}]}
                """);
        assertEquals(2, drops.size());
        assertEquals(2, drops.get(0).min());
        assertEquals(3, drops.get(0).max());
        assertTrue(drops.get(0).flags().contains(LootSummary.TAG));
        assertTrue(drops.get(0).flags().contains(LootSummary.OTHER_CONDITION));
        assertTrue(drops.get(1).flags().contains(LootSummary.TABLE));
    }

    @Test
    void emptyOrOddTablesGiveNothing() {
        assertTrue(parse("{}").isEmpty());
        assertTrue(parse("{\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:empty\"}]}]}").isEmpty());
        assertTrue(LootSummary.parse(null).isEmpty());
    }

    @Test
    void linesRoundTrip() {
        List<LootSummary.Drop> drops = parse("{\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"a:x\"}]}]}");
        List<WikiAnswers.Drop> back = WikiAnswers.parseLoot(LootSummary.toLines(drops));
        assertEquals(1, back.size());
        assertEquals("a:x", back.get(0).item());
        assertEquals(100.0, back.get(0).chance());
    }
}
