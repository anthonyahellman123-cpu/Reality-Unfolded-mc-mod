package com.anthonyahellman.realityunfolded.testreality;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRealityDataTest {
    @Test
    void definitionRoundTripStoresInstructionsNotWorldState() {
        CompoundTag persisted = new CompoundTag();
        TestRealityData data = new TestRealityData(persisted, 1234L);
        TestRealityData.FillOperation operation = new TestRealityData.FillOperation(
            new BlockPos(-3, 20, -2), new BlockPos(3, 24, 2), new ResourceLocation("minecraft", "stone"));

        assertTrue(data.appendEdits(List.of(operation)));
        data.rememberScannedSubject(new ResourceLocation("minecraft", "zombie"));
        TestRealityData restored = new TestRealityData(persisted, 9999L);

        assertEquals(1234L, restored.seed());
        assertEquals(List.of(operation), restored.edits());
        assertEquals(List.of(new ResourceLocation("minecraft", "zombie")), restored.scannedSubjects());
        assertFalse(persisted.contains("chunks"));
        assertFalse(persisted.contains("entities"));
    }

    @Test
    void oversizedProceduralOperationIsDiscardedOnRead() {
        CompoundTag persisted = new CompoundTag();
        TestRealityData data = new TestRealityData(persisted, 1L);
        TestRealityData.FillOperation tooLarge = new TestRealityData.FillOperation(
            BlockPos.ZERO, new BlockPos(100, 100, 100), new ResourceLocation("minecraft", "stone"));

        assertFalse(data.appendEdits(List.of(tooLarge)));
        assertTrue(new TestRealityData(persisted, 1L).edits().isEmpty());
    }

    @Test
    void generatorIsDeterministicBySeed() {
        TestRealityGenerator first = new TestRealityGenerator(42L);
        TestRealityGenerator same = new TestRealityGenerator(42L);
        TestRealityGenerator different = new TestRealityGenerator(43L);

        for (int x = -128; x <= 128; x += 17) {
            assertEquals(first.surfaceHeight(x, x / 2), same.surfaceHeight(x, x / 2));
        }
        boolean differs = false;
        for (int x = -128; x <= 128; x += 17) {
            differs |= first.surfaceHeight(x, x / 2) != different.surfaceHeight(x, x / 2);
        }
        assertTrue(differs);
    }

    @Test
    void activeCellsAreWidelySeparatedAndReusableIdentifiers() {
        Set<Long> positions = new HashSet<>();
        for (int i = 0; i < TestRealityService.MAX_ACTIVE_INSTANCES; i++) {
            var center = TestRealityService.cellCenter(i);
            assertTrue(positions.add(center.toLong()));
            if (i > 0) assertTrue(Math.max(Math.abs(center.x), Math.abs(center.z))
                >= TestRealityService.CELL_STRIDE_CHUNKS);
        }
    }
}
