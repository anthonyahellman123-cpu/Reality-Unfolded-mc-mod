package com.anthonyahellman.realityunfolded.grimoire;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class GrimoireData {
    public static final int SLOT_COUNT = 8;
    public static final int MAX_NAME_LENGTH = 48;
    public static final int MAX_SOURCE_LENGTH = 512;
    private static final String ROOT_KEY = "reality_unfolded_grimoire";
    private static final String INITIALIZED_KEY = "initialized";
    private static final String SELECTED_KEY = "selected_slot";

    private final CompoundTag tag;

    GrimoireData(CompoundTag tag) {
        this.tag = tag;
        initialize();
    }

    public static GrimoireData get(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag grimoire = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, grimoire);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return new GrimoireData(grimoire);
    }

    private void initialize() {
        if (!tag.getBoolean(INITIALIZED_KEY)) {
            tag.putInt(SELECTED_KEY, 0);
            for (int i = 0; i < SLOT_COUNT; i++) {
                tag.putString(nameKey(i), "Spell " + (i + 1));
                tag.putString(sourceKey(i), "");
            }
            tag.putBoolean(INITIALIZED_KEY, true);
        }
        tag.putInt(SELECTED_KEY, clampSlot(tag.getInt(SELECTED_KEY)));
    }

    public int selectedSlot() {
        return clampSlot(tag.getInt(SELECTED_KEY));
    }

    public void setSelectedSlot(int slot) {
        tag.putInt(SELECTED_KEY, clampSlot(slot));
    }

    public SpellSlot slot(int slot) {
        int safeSlot = clampSlot(slot);
        return new SpellSlot(tag.getString(nameKey(safeSlot)), tag.getString(sourceKey(safeSlot)));
    }

    public void saveSlot(int slot, String name, String source) {
        int safeSlot = clampSlot(slot);
        tag.putString(nameKey(safeSlot), sanitize(name, MAX_NAME_LENGTH, "Spell " + (safeSlot + 1)));
        tag.putString(sourceKey(safeSlot), sanitize(source, MAX_SOURCE_LENGTH, ""));
    }

    public Snapshot snapshot() {
        List<SpellSlot> slots = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) slots.add(slot(i));
        return new Snapshot(selectedSlot(), slots);
    }

    private static String sanitize(String value, int maximum, String fallback) {
        String safe = value == null ? fallback : value.strip();
        if (safe.isEmpty() && !fallback.isEmpty()) safe = fallback;
        return safe.length() <= maximum ? safe : safe.substring(0, maximum);
    }

    private static int clampSlot(int slot) {
        return Math.max(0, Math.min(SLOT_COUNT - 1, slot));
    }

    private static String nameKey(int slot) {
        return "slot_" + slot + "_name";
    }

    private static String sourceKey(int slot) {
        return "slot_" + slot + "_source";
    }

    public record SpellSlot(String name, String source) {}

    public record Snapshot(int selectedSlot, List<SpellSlot> slots) {
        public Snapshot {
            slots = List.copyOf(slots);
        }
    }
}
