package com.anthonyahellman.realityunfolded.testreality;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tiny player-owned definition. It never stores disposable Test Reality world state. */
public final class TestRealityData {
    public static final int GENERATION_VERSION = 1;
    public static final int DEFAULT_RADIUS_CHUNKS = 8;
    public static final int MAX_EDIT_OPERATIONS = 256;
    public static final int MAX_SCANNED_SUBJECTS = 64;
    public static final int MAX_FILL_VOLUME = 32_768;
    private static final String ROOT_KEY = "reality_unfolded_test_reality";
    private static final String INITIALIZED = "initialized";
    private static final String EDITS = "edits";
    private static final String SUBJECTS = "scanned_subjects";
    private static final String RETURN = "return_ticket";

    private final CompoundTag tag;

    public TestRealityData(CompoundTag tag, long initialSeed) {
        this.tag = tag;
        if (!tag.getBoolean(INITIALIZED)) {
            tag.putBoolean(INITIALIZED, true);
            tag.putInt("generation_version", GENERATION_VERSION);
            tag.putLong("seed", initialSeed);
            tag.putInt("radius_chunks", DEFAULT_RADIUS_CHUNKS);
            tag.putBoolean("diagnostics", false);
            tag.put(EDITS, new ListTag());
            tag.put(SUBJECTS, new ListTag());
        }
    }

    public static TestRealityData get(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag reality = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, reality);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        UUID id = player.getUUID();
        return new TestRealityData(reality, id.getMostSignificantBits() ^ id.getLeastSignificantBits()
            ^ 0x52555F5445535452L);
    }

    public long seed() { return tag.getLong("seed"); }
    public void setSeed(long seed) { tag.putLong("seed", seed); }
    public int generationVersion() { return tag.getInt("generation_version"); }
    public int radiusChunks() {
        int value = tag.getInt("radius_chunks");
        return Math.max(2, Math.min(32, value == 0 ? DEFAULT_RADIUS_CHUNKS : value));
    }
    public boolean diagnostics() { return tag.getBoolean("diagnostics"); }
    public void setDiagnostics(boolean enabled) { tag.putBoolean("diagnostics", enabled); }

    public List<FillOperation> edits() {
        List<FillOperation> operations = new ArrayList<>();
        ListTag list = tag.getList(EDITS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && operations.size() < MAX_EDIT_OPERATIONS; i++) {
            FillOperation.load(list.getCompound(i)).ifPresent(operations::add);
        }
        return List.copyOf(operations);
    }

    public boolean appendEdits(List<FillOperation> operations) {
        if (operations.isEmpty()) return true;
        if (operations.stream().anyMatch(operation -> operation == null
            || operation.volume() > MAX_FILL_VOLUME)) return false;
        List<FillOperation> existing = new ArrayList<>(edits());
        if (existing.size() + operations.size() > MAX_EDIT_OPERATIONS) return false;
        existing.addAll(operations);
        ListTag list = new ListTag();
        existing.forEach(operation -> list.add(operation.save()));
        tag.put(EDITS, list);
        return true;
    }

    public List<ResourceLocation> scannedSubjects() {
        List<ResourceLocation> result = new ArrayList<>();
        ListTag list = tag.getList(SUBJECTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && result.size() < MAX_SCANNED_SUBJECTS; i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getCompound(i).getString("type"));
            if (id != null) result.add(id);
        }
        return List.copyOf(result);
    }

    /** Stores only a recreate-able subject descriptor, never a live entity snapshot. */
    public boolean rememberScannedSubject(ResourceLocation entityType) {
        List<ResourceLocation> subjects = new ArrayList<>(scannedSubjects());
        if (subjects.contains(entityType)) return true;
        if (subjects.size() >= MAX_SCANNED_SUBJECTS) return false;
        subjects.add(entityType);
        ListTag list = new ListTag();
        for (ResourceLocation subject : subjects) {
            CompoundTag value = new CompoundTag();
            value.putString("type", subject.toString());
            list.add(value);
        }
        tag.put(SUBJECTS, list);
        return true;
    }

    public ReturnTicket returnTicket() {
        return tag.contains(RETURN, Tag.TAG_COMPOUND) ? ReturnTicket.load(tag.getCompound(RETURN)) : null;
    }
    public void setReturnTicket(ReturnTicket ticket) { tag.put(RETURN, ticket.save()); }
    public void clearReturnTicket() { tag.remove(RETURN); }

    public record FillOperation(BlockPos min, BlockPos max, ResourceLocation block) {
        public FillOperation {
            BlockPos first = min;
            BlockPos second = max;
            min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()));
            max = new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ()));
        }

        public long volume() {
            return (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1)
                * (max.getZ() - min.getZ() + 1);
        }

        CompoundTag save() {
            CompoundTag value = new CompoundTag();
            value.putLong("min", min.asLong());
            value.putLong("max", max.asLong());
            value.putString("block", block.toString());
            return value;
        }

        static java.util.Optional<FillOperation> load(CompoundTag value) {
            ResourceLocation block = ResourceLocation.tryParse(value.getString("block"));
            if (block == null) return java.util.Optional.empty();
            FillOperation operation = new FillOperation(BlockPos.of(value.getLong("min")),
                BlockPos.of(value.getLong("max")), block);
            return operation.volume() <= MAX_FILL_VOLUME ? java.util.Optional.of(operation)
                : java.util.Optional.empty();
        }
    }

    public record ReturnTicket(ResourceKey<Level> dimension, double x, double y, double z,
                               float yaw, float pitch) {
        public CompoundTag save() {
            CompoundTag value = new CompoundTag();
            value.putString("dimension", dimension.location().toString());
            value.putDouble("x", x);
            value.putDouble("y", y);
            value.putDouble("z", z);
            value.putFloat("yaw", yaw);
            value.putFloat("pitch", pitch);
            return value;
        }

        static ReturnTicket load(CompoundTag value) {
            ResourceLocation id = ResourceLocation.tryParse(value.getString("dimension"));
            if (id == null) return null;
            return new ReturnTicket(ResourceKey.create(Registries.DIMENSION, id), value.getDouble("x"),
                value.getDouble("y"), value.getDouble("z"), value.getFloat("yaw"), value.getFloat("pitch"));
        }
    }
}
