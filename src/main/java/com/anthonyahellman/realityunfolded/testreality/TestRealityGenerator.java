package com.anthonyahellman.realityunfolded.testreality;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;

/** Deterministic, chunk-local realization of a player's compact reality definition. */
public final class TestRealityGenerator {
    public static final int CLEAR_MIN_Y = 0;
    public static final int CLEAR_MAX_Y = 127;
    private final SimplexNoise broadNoise;
    private final SimplexNoise detailNoise;

    public TestRealityGenerator(long seed) {
        broadNoise = new SimplexNoise(RandomSource.create(seed));
        detailNoise = new SimplexNoise(RandomSource.create(seed ^ 0x9E3779B97F4A7C15L));
    }

    public int surfaceHeight(int worldX, int worldZ) {
        double broad = broadNoise.getValue(worldX / 72.0D, worldZ / 72.0D);
        double detail = detailNoise.getValue(worldX / 23.0D, worldZ / 23.0D);
        return Math.max(12, Math.min(48, 27 + (int) Math.round(broad * 10.0D + detail * 3.0D)));
    }

    public void rebuild(LevelChunk chunk, List<TestRealityData.FillOperation> edits) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        for (int x = minX; x <= minX + 15; x++) {
            for (int z = minZ; z <= minZ + 15; z++) {
                int surface = surfaceHeight(x, z);
                for (int y = CLEAR_MIN_Y; y <= CLEAR_MAX_Y; y++) {
                    cursor.set(x, y, z);
                    Block block = y == 0 ? Blocks.BEDROCK
                        : y < surface - 3 ? Blocks.STONE
                        : y < surface ? Blocks.DIRT
                        : y == surface ? Blocks.GRASS_BLOCK : Blocks.AIR;
                    chunk.setBlockState(cursor, block.defaultBlockState(), false);
                }
            }
        }
        for (TestRealityData.FillOperation edit : edits) apply(chunk, edit);
        chunk.setUnsaved(true);
    }

    public static void apply(LevelChunk chunk, TestRealityData.FillOperation operation) {
        Block block = BuiltInRegistries.BLOCK.get(operation.block());
        if (block == Blocks.AIR && !operation.block().equals(BuiltInRegistries.BLOCK.getKey(Blocks.AIR))) return;
        int minX = Math.max(chunk.getPos().getMinBlockX(), operation.min().getX());
        int maxX = Math.min(chunk.getPos().getMaxBlockX(), operation.max().getX());
        int minZ = Math.max(chunk.getPos().getMinBlockZ(), operation.min().getZ());
        int maxZ = Math.min(chunk.getPos().getMaxBlockZ(), operation.max().getZ());
        if (minX > maxX || minZ > maxZ) return;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            for (int y = Math.max(CLEAR_MIN_Y, operation.min().getY());
                 y <= Math.min(CLEAR_MAX_Y, operation.max().getY()); y++) {
                chunk.setBlockState(cursor.set(x, y, z), block.defaultBlockState(), false);
            }
        }
        chunk.setUnsaved(true);
    }
}
