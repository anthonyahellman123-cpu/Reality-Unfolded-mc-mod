package com.anthonyahellman.realityunfolded.testreality;

import net.minecraft.world.level.ChunkPos;

/** Pure deterministic cell allocation math, isolated from live server registries for testing. */
public final class TestRealityCells {
    public static final int STRIDE_CHUNKS = 128;
    public static final int MAX_ACTIVE = 16;

    private TestRealityCells() {}

    public static ChunkPos center(int index) {
        if (index == 0) return new ChunkPos(0, 0);
        int ring = (int) Math.ceil((Math.sqrt(index + 1.0D) - 1.0D) / 2.0D);
        int side = ring * 2;
        int maximum = (ring * 2 + 1) * (ring * 2 + 1) - 1;
        int offset = maximum - index;
        int x;
        int z;
        if (offset < side) { x = ring - offset; z = -ring; }
        else if (offset < side * 2) { x = -ring; z = -ring + (offset - side); }
        else if (offset < side * 3) { x = -ring + (offset - side * 2); z = ring; }
        else { x = ring; z = ring - (offset - side * 3); }
        return new ChunkPos(x * STRIDE_CHUNKS, z * STRIDE_CHUNKS);
    }
}
