package com.omneria.fordix.console;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * What a levelled-settings console shows. A mod builds it as a plain {@link CompoundTag} (so it needs no Fordix class to do it) and
 * hands it to {@link FordixConsoles#open}; the client reads it back here. Texts are components written as JSON strings, translated
 * on the player's side. Tag layout: {@code kind}, {@code pos} (int array), {@code title}, {@code subtitle}, {@code pill}, {@code footer},
 * {@code max}, {@code chips} (list of strings) and {@code rows} (list of compounds: {@code id}, {@code name}, {@code effect}, {@code level},
 * {@code value}, {@code malus} and {@code bonus}, each {@code {item, name}} where item is an item id or "#tag").
 */
public record ConsoleData(String kind, BlockPos pos, Component title, Component subtitle, Component pill, Component footer, int maxLevel,
                          List<Row> rows, List<Component> chips) {
    public static final int MAX_ROWS = 16;
    public static final int MAX_CHIPS = 24;

    public record Slot(String spec, Component name) {
    }

    public record Row(String id, Component name, Component effect, int level, Component value, Slot malus, Slot bonus) {
    }

    private static Component text(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            return Component.empty();
        }
        Component parsed = Component.Serializer.fromJson(tag.getString(key));
        return parsed == null ? Component.empty() : parsed;
    }

    private static Slot slot(CompoundTag tag) {
        return new Slot(tag.getString("item"), text(tag, "name"));
    }

    /** The data of a tag, or null when it is not a console (no kind, no position). */
    public static ConsoleData fromTag(CompoundTag tag) {
        if (tag == null || !tag.contains("kind", Tag.TAG_STRING) || tag.getIntArray("pos").length != 3) {
            return null;
        }
        int[] pos = tag.getIntArray("pos");
        List<Row> rows = new ArrayList<>();
        ListTag rowTags = tag.getList("rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < rowTags.size() && i < MAX_ROWS; i++) {
            CompoundTag row = rowTags.getCompound(i);
            rows.add(new Row(row.getString("id"), text(row, "name"), text(row, "effect"), row.getInt("level"), text(row, "value"),
                    slot(row.getCompound("malus")), slot(row.getCompound("bonus"))));
        }
        List<Component> chips = new ArrayList<>();
        ListTag chipTags = tag.getList("chips", Tag.TAG_STRING);
        for (int i = 0; i < chipTags.size() && i < MAX_CHIPS; i++) {
            Component chip = Component.Serializer.fromJson(chipTags.getString(i));
            if (chip != null) {
                chips.add(chip);
            }
        }
        int max = Math.max(1, Math.min(9, tag.contains("max", Tag.TAG_ANY_NUMERIC) ? tag.getInt("max") : 5));
        return new ConsoleData(tag.getString("kind"), new BlockPos(pos[0], pos[1], pos[2]), text(tag, "title"), text(tag, "subtitle"),
                text(tag, "pill"), text(tag, "footer"), max, rows, chips);
    }
}
