package com.anthonyahellman.realityunfolded.grimoire;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrimoireSpellServiceTest {
    @Test
    void validatesAcceptanceSpellWithRealParser() {
        GrimoireSpellService.Result result = GrimoireSpellService.validate(
            "BOLT -> HOME -> IMPACT -> IGNITE");

        assertTrue(result.success());
        assertTrue(result.message().contains("4 glyphs"));
        assertEquals(1, result.manifestations());
    }

    @Test
    void repeatedSplitEstimateComesFromCanonicalValidation() {
        GrimoireSpellService.Result result = GrimoireSpellService.validate(
            "BOLT SPLIT SPLIT HOME IMPACT IGNITE");

        assertTrue(result.success());
        assertEquals(4, result.manifestations());
    }

    @Test
    void invalidSourceCannotOverwriteSavedSpellOrSelection() {
        GrimoireData data = new GrimoireData(new CompoundTag());
        assertTrue(GrimoireSpellService.save(data, 2, "Working", "BOLT IMPACT IGNITE", true).success());

        GrimoireSpellService.Result rejected = GrimoireSpellService.save(
            data, 2, "Broken", "BOLT NOT_A_WORD", false);

        assertFalse(rejected.success());
        assertEquals(2, data.selectedSlot());
        assertEquals("Working", data.slot(2).name());
        assertEquals("BOLT IMPACT IGNITE", data.slot(2).source());
    }

    @Test
    void outOfRangeClientSlotCannotOverwriteAnEdgeSlot() {
        GrimoireData data = new GrimoireData(new CompoundTag());

        GrimoireSpellService.Result rejected = GrimoireSpellService.save(
            data, 99, "Injected", "BOLT", true);

        assertFalse(rejected.success());
        assertEquals("Spell 8", data.slot(7).name());
        assertEquals("", data.slot(7).source());
        assertEquals(0, data.selectedSlot());
    }

    @Test
    void eightSlotsRemainIndependentWhenEditingAndRenaming() {
        GrimoireData data = new GrimoireData(new CompoundTag());
        assertTrue(GrimoireSpellService.save(data, 0, "First", "BOLT", false).success());
        assertTrue(GrimoireSpellService.save(data, 7, "Eighth", "BREAK", false).success());
        assertTrue(GrimoireSpellService.save(data, 0, "Renamed", "BOLT HOME", false).success());

        assertEquals("Renamed", data.slot(0).name());
        assertEquals("BOLT HOME", data.slot(0).source());
        assertEquals("Eighth", data.slot(7).name());
        assertEquals("BREAK", data.slot(7).source());
        assertEquals(GrimoireData.SLOT_COUNT, data.snapshot().slots().size());
    }

    @Test
    void selectingSavedSpellDoesNotRewriteOtherSlots() {
        GrimoireData data = new GrimoireData(new CompoundTag());
        GrimoireSpellService.save(data, 1, "One", "BOLT", false);
        GrimoireSpellService.save(data, 5, "Five", "BREAK", false);

        GrimoireSpellService.Result result = GrimoireSpellService.select(data, 5);

        assertTrue(result.success());
        assertEquals(5, data.selectedSlot());
        assertEquals("One", data.slot(1).name());
        assertEquals("BOLT", data.slot(1).source());
    }

    @Test
    void emptySlotCannotBecomeTheActiveCastingSelection() {
        GrimoireData data = new GrimoireData(new CompoundTag());

        GrimoireSpellService.Result result = GrimoireSpellService.select(data, 4);

        assertFalse(result.success());
        assertEquals(0, data.selectedSlot());
    }

    @Test
    void corruptedStoredSourceCannotBecomeTheActiveCastingSelection() {
        GrimoireData data = new GrimoireData(new CompoundTag());
        data.saveSlot(6, "Corrupt", "BOLT UNKNOWN");

        GrimoireSpellService.Result result = GrimoireSpellService.select(data, 6);

        assertFalse(result.success());
        assertEquals(0, data.selectedSlot());
    }

    @Test
    void separatePersistentTagsProduceSeparatePlayerLibraries() {
        GrimoireData firstPlayer = new GrimoireData(new CompoundTag());
        GrimoireData secondPlayer = new GrimoireData(new CompoundTag());
        GrimoireSpellService.save(firstPlayer, 3, "Private", "BOLT HOME", true);

        assertEquals("Private", firstPlayer.slot(3).name());
        assertEquals("Spell 4", secondPlayer.slot(3).name());
        assertEquals("", secondPlayer.slot(3).source());
        assertEquals(0, secondPlayer.selectedSlot());
    }

    @Test
    void smartSpellRoundTripUsesExistingPlayerOwnedSlots() {
        CompoundTag persisted = new CompoundTag();
        GrimoireData data = new GrimoireData(persisted);
        String source = "ENTITY HOSTILE NEAREST BOLT SPLIT HOME ACCELERATE IMPACT IGNITE";

        GrimoireSpellService.Result saved = GrimoireSpellService.save(
            data, 3, "Hunter", source, true);
        GrimoireData restored = new GrimoireData(persisted);

        assertTrue(saved.success());
        assertEquals(3, restored.selectedSlot());
        assertEquals("Hunter", restored.slot(3).name());
        assertEquals(source, restored.slot(3).source());
    }

    @Test
    void invalidContextProgramCannotReplaceCanonicalStorage() {
        GrimoireData data = new GrimoireData(new CompoundTag());
        assertTrue(GrimoireSpellService.save(data, 1, "Safe", "BOLT", true).success());

        GrimoireSpellService.Result rejected = GrimoireSpellService.save(
            data, 1, "Invalid", "HOSTILE NEAREST BOLT", true);

        assertFalse(rejected.success());
        assertEquals("Safe", data.slot(1).name());
        assertEquals("BOLT", data.slot(1).source());
        assertEquals(1, data.selectedSlot());
    }
}
