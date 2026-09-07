package com.anthonyahellman.realityunfolded.grimoire;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrimoireDataTest {
    @Test
    void initializesEightPlayerOwnedSlots() {
        GrimoireData data = new GrimoireData(new CompoundTag());
        assertEquals(GrimoireData.SLOT_COUNT, data.snapshot().slots().size());
        assertEquals("Spell 1", data.slot(0).name());
        assertEquals("", data.slot(0).source());
    }

    @Test
    void slotAndSelectionSurviveReconstructionFromPersistentTag() {
        CompoundTag persisted = new CompoundTag();
        GrimoireData initial = new GrimoireData(persisted);
        initial.saveSlot(4, "Homing Ember", "BOLT HOME IMPACT IGNITE");
        initial.setSelectedSlot(4);

        GrimoireData restored = new GrimoireData(persisted);
        assertEquals(4, restored.selectedSlot());
        assertEquals("Homing Ember", restored.slot(4).name());
        assertEquals("BOLT HOME IMPACT IGNITE", restored.slot(4).source());
    }

    @Test
    void playerDataEnforcesStorageLimits() {
        GrimoireData data = new GrimoireData(new CompoundTag());
        data.saveSlot(0, "N".repeat(100), "BOLT ".repeat(200));
        assertEquals(GrimoireData.MAX_NAME_LENGTH, data.slot(0).name().length());
        assertEquals(GrimoireData.MAX_SOURCE_LENGTH, data.slot(0).source().length());
    }
}
