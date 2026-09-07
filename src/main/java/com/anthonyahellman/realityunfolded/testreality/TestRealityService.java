package com.anthonyahellman.realityunfolded.testreality;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.spell.runtime.DelayedSpellRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-thread-owned lifecycle for disposable, spatially isolated Test Reality allocations. */
public final class TestRealityService {
    public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
        new ResourceLocation(RealityUnfolded.MOD_ID, "test_reality"));
    public static final int CELL_STRIDE_CHUNKS = TestRealityCells.STRIDE_CHUNKS;
    public static final int MAX_ACTIVE_INSTANCES = TestRealityCells.MAX_ACTIVE;
    private static final int MAX_EDIT_AXIS = 64;
    private static final Set<ResourceLocation> EDIT_PALETTE = Set.of(
        new ResourceLocation("minecraft", "air"), new ResourceLocation("minecraft", "dirt"),
        new ResourceLocation("minecraft", "stone"), new ResourceLocation("minecraft", "obsidian"),
        new ResourceLocation("minecraft", "bedrock"));
    private static final Map<MinecraftServer, RuntimeState> SERVERS = new WeakHashMap<>();

    private TestRealityService() {}

    public static Result enter(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ServerLevel level = server.getLevel(LEVEL_KEY);
        if (level == null) return Result.failure("Test Reality dimension is unavailable.");
        RuntimeState state = state(server);
        if (state.byOwner.containsKey(player.getUUID())) return Result.failure("A Test Reality is already active.");
        if (state.byOwner.size() >= MAX_ACTIVE_INSTANCES) return Result.failure("Test Reality runtime capacity reached.");

        TestRealityData data = TestRealityData.get(player);
        data.setReturnTicket(new TestRealityData.ReturnTicket(player.serverLevel().dimension(), player.getX(),
            player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        int cellIndex = firstFreeCell(state);
        ChunkPos center = TestRealityCells.center(cellIndex);
        ActiveInstance instance = new ActiveInstance(player.getUUID(), UUID.randomUUID(), cellIndex, center,
            data.radiusChunks(), data.seed(), data.edits(), data.diagnostics());
        state.byOwner.put(player.getUUID(), instance);
        state.byCell.put(cellIndex, instance);

        LevelChunk arrival = level.getChunk(center.x, center.z);
        prepareChunk(instance, arrival);
        int x = center.getMiddleBlockX();
        int z = center.getMiddleBlockZ();
        int y = instance.generator.surfaceHeight(x, z) + 2;
        player.teleportTo(level, x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot());
        RealityUnfolded.LOGGER.info("[RU REALITY] INSTANCE_CREATED session={} owner={} cell={} seed={}",
            instance.sessionId, instance.owner, instance.cellIndex, instance.seed);
        return Result.success("Entered Test Reality " + shortId(instance.sessionId) + ".");
    }

    public static Result exit(ServerPlayer player) {
        RuntimeState state = state(player.getServer());
        ActiveInstance instance = state.byOwner.get(player.getUUID());
        if (instance == null) {
            if (player.serverLevel().dimension().equals(LEVEL_KEY)) {
                recover(player);
                return Result.success("Recovered from an orphaned Test Reality.");
            }
            return Result.failure("No active Test Reality.");
        }
        destroy(player.getServer(), instance, true, null);
        return Result.success("Test Reality discarded. Its definition remains.");
    }

    public static Result setEditMode(ServerPlayer player, boolean enabled) {
        ActiveInstance instance = owned(player);
        if (instance == null) return Result.failure("Enter your Test Reality first.");
        instance.editMode = enabled;
        return Result.success("Edit mode " + (enabled ? "enabled" : "disabled") + ".");
    }

    public static Result stageFill(ServerPlayer player, BlockPos first, BlockPos second, Block block) {
        ActiveInstance instance = owned(player);
        if (instance == null || !instance.editMode) return Result.failure("Enable edit mode inside your Test Reality first.");
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (!EDIT_PALETTE.contains(blockId)) return Result.failure("POC palette: air, dirt, stone, obsidian, bedrock.");
        TestRealityData.FillOperation absolute = new TestRealityData.FillOperation(first, second,
            blockId);
        if (!instance.contains(absolute.min()) || !instance.contains(absolute.max())) {
            return Result.failure("Edit must remain inside this Test Reality's provisional bounds.");
        }
        int dx = absolute.max().getX() - absolute.min().getX() + 1;
        int dy = absolute.max().getY() - absolute.min().getY() + 1;
        int dz = absolute.max().getZ() - absolute.min().getZ() + 1;
        if (absolute.volume() > TestRealityData.MAX_FILL_VOLUME || dx > MAX_EDIT_AXIS || dy > MAX_EDIT_AXIS
            || dz > MAX_EDIT_AXIS) return Result.failure("Edit exceeds the POC operation safety bound.");
        if (instance.staged.size() >= 64) return Result.failure("Save or exit before staging more edits.");

        TestRealityData.FillOperation relative = instance.toRelative(absolute);
        instance.staged.add(relative);
        applyToLoadedArea(player.serverLevel(), absolute);
        return Result.success("Staged FILL of " + absolute.volume() + " blocks. Use /ru reality save to persist it.");
    }

    public static Result saveEdits(ServerPlayer player) {
        ActiveInstance instance = owned(player);
        if (instance == null || !instance.editMode) return Result.failure("No active edit session.");
        if (instance.staged.isEmpty()) return Result.success("No deliberate edits are staged.");
        TestRealityData data = TestRealityData.get(player);
        if (!data.appendEdits(instance.staged)) return Result.failure("Persistent edit-operation safety limit reached.");
        int count = instance.staged.size();
        instance.committed.addAll(instance.staged);
        instance.staged.clear();
        return Result.success("Saved " + count + " procedural edit operation(s); disposable world state was not saved.");
    }

    public static Result setSeed(ServerPlayer player, long seed) {
        if (state(player.getServer()).byOwner.containsKey(player.getUUID())) {
            return Result.failure("Exit the active Test Reality before changing its seed.");
        }
        TestRealityData.get(player).setSeed(seed);
        return Result.success("Test Reality seed set to " + seed + ".");
    }

    public static String status(ServerPlayer player) {
        TestRealityData data = TestRealityData.get(player);
        ActiveInstance active = state(player.getServer()).byOwner.get(player.getUUID());
        return "seed=" + data.seed() + " edits=" + data.edits().size() + " subjects="
            + data.scannedSubjects().size() + " active=" + (active == null ? "no" : shortId(active.sessionId))
            + (active == null ? "" : " staged=" + active.staged.size() + " chunks=" + active.generated.size());
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (!level.dimension().equals(LEVEL_KEY)) return;
        ActiveInstance instance = findByChunk(state(level.getServer()), chunk.getPos());
        if (instance != null) prepareChunk(instance, chunk);
    }

    public static void keepInside(ServerPlayer player) {
        if (!player.serverLevel().dimension().equals(LEVEL_KEY)) return;
        ActiveInstance instance = state(player.getServer()).byOwner.get(player.getUUID());
        if (instance == null) {
            recover(player);
            return;
        }
        if (!instance.contains(player.blockPosition())) {
            int x = instance.center.getMiddleBlockX();
            int z = instance.center.getMiddleBlockZ();
            int y = instance.generator.surfaceHeight(x, z) + 2;
            player.teleportTo(player.serverLevel(), x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot());
        }
    }

    public static void recoverOrphan(ServerPlayer player) {
        if (player.serverLevel().dimension().equals(LEVEL_KEY)
            && !state(player.getServer()).byOwner.containsKey(player.getUUID())) recover(player);
    }

    public static void disconnected(ServerPlayer player) {
        ActiveInstance instance = state(player.getServer()).byOwner.get(player.getUUID());
        if (instance != null) destroy(player.getServer(), instance, false, player.getUUID());
    }

    public static void changedDimension(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) {
        if (!from.equals(LEVEL_KEY) || to.equals(LEVEL_KEY)) return;
        ActiveInstance instance = state(player.getServer()).byOwner.get(player.getUUID());
        if (instance != null) destroy(player.getServer(), instance, false, null);
        TestRealityData.get(player).clearReturnTicket();
    }

    public static void serverStopping(MinecraftServer server) {
        RuntimeState state = SERVERS.remove(server);
        if (state != null) {
            RealityUnfolded.LOGGER.info("[RU REALITY] SERVER_STOP activeInstances={}", state.byOwner.size());
            state.byOwner.clear();
            state.byCell.clear();
        }
    }

    private static void prepareChunk(ActiveInstance instance, LevelChunk chunk) {
        if (!instance.contains(chunk.getPos()) || !instance.generated.add(chunk.getPos().toLong())) return;
        instance.generator.rebuild(chunk, instance.realizedEdits());
        if (instance.diagnostics) RealityUnfolded.LOGGER.info(
            "[RU REALITY] CHUNK_REBUILT session={} chunk={} edits={}", instance.sessionId, chunk.getPos(),
            instance.committed.size() + instance.staged.size());
    }

    private static void applyToLoadedArea(ServerLevel level, TestRealityData.FillOperation operation) {
        int minChunkX = operation.min().getX() >> 4;
        int maxChunkX = operation.max().getX() >> 4;
        int minChunkZ = operation.min().getZ() >> 4;
        int maxChunkZ = operation.max().getZ() >> 4;
        for (int x = minChunkX; x <= maxChunkX; x++) for (int z = minChunkZ; z <= maxChunkZ; z++) {
            TestRealityGenerator.apply(level.getChunk(x, z), operation);
        }
    }

    private static void destroy(MinecraftServer server, ActiveInstance instance, boolean evacuateOwner,
                                UUID disconnectedOwner) {
        RuntimeState state = state(server);
        state.byOwner.remove(instance.owner);
        state.byCell.remove(instance.cellIndex);
        ServerLevel level = server.getLevel(LEVEL_KEY);
        if (level != null) {
            List<ServerPlayer> occupants = new ArrayList<>(level.players().stream()
                .filter(player -> instance.contains(player.blockPosition())).toList());
            for (ServerPlayer occupant : occupants) {
                if (occupant.getUUID().equals(disconnectedOwner)) continue;
                if (evacuateOwner || !occupant.getUUID().equals(instance.owner)) recover(occupant);
            }
            for (Entity entity : level.getEntitiesOfClass(Entity.class, instance.bounds,
                entity -> !(entity instanceof ServerPlayer))) entity.discard();
            DelayedSpellRuntime.cancelCaster(level, instance.owner);
        }
        RealityUnfolded.LOGGER.info("[RU REALITY] INSTANCE_DESTROYED session={} owner={} touchedChunks={} stagedDiscarded={}",
            instance.sessionId, instance.owner, instance.generated.size(), instance.staged.size());
    }

    private static void recover(ServerPlayer player) {
        TestRealityData data = TestRealityData.get(player);
        TestRealityData.ReturnTicket ticket = data.returnTicket();
        ServerLevel target = ticket == null ? null : player.getServer().getLevel(ticket.dimension());
        if (target == null || target.dimension().equals(LEVEL_KEY)) target = player.getServer().overworld();
        if (ticket != null && target.dimension().equals(ticket.dimension())) {
            player.teleportTo(target, ticket.x(), ticket.y(), ticket.z(), ticket.yaw(), ticket.pitch());
        } else {
            BlockPos spawn = target.getSharedSpawnPos();
            player.teleportTo(target, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        }
        data.clearReturnTicket();
    }

    private static ActiveInstance owned(ServerPlayer player) {
        ActiveInstance instance = state(player.getServer()).byOwner.get(player.getUUID());
        return instance != null && player.serverLevel().dimension().equals(LEVEL_KEY)
            && instance.contains(player.blockPosition()) ? instance : null;
    }

    private static RuntimeState state(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, ignored -> new RuntimeState());
    }

    private static int firstFreeCell(RuntimeState state) {
        for (int i = 0; i < MAX_ACTIVE_INSTANCES; i++) if (!state.byCell.containsKey(i)) return i;
        throw new IllegalStateException("No Test Reality cell available");
    }

    private static ActiveInstance findByChunk(RuntimeState state, ChunkPos chunk) {
        for (ActiveInstance instance : state.byOwner.values()) if (instance.contains(chunk)) return instance;
        return null;
    }

    private static ActiveInstance findByPosition(RuntimeState state, BlockPos position) {
        for (ActiveInstance instance : state.byOwner.values()) if (instance.contains(position)) return instance;
        return null;
    }

    private static String shortId(UUID id) { return id.toString().substring(0, 8); }

    private static final class RuntimeState {
        private final Map<UUID, ActiveInstance> byOwner = new HashMap<>();
        private final Map<Integer, ActiveInstance> byCell = new HashMap<>();
    }

    private static final class ActiveInstance {
        private final UUID owner;
        private final UUID sessionId;
        private final int cellIndex;
        private final ChunkPos center;
        private final int radius;
        private final long seed;
        private final List<TestRealityData.FillOperation> committed;
        private final List<TestRealityData.FillOperation> staged = new ArrayList<>();
        private final Set<Long> generated = new HashSet<>();
        private final TestRealityGenerator generator;
        private final boolean diagnostics;
        private final AABB bounds;
        private boolean editMode;

        private ActiveInstance(UUID owner, UUID sessionId, int cellIndex, ChunkPos center, int radius,
                               long seed, List<TestRealityData.FillOperation> committed, boolean diagnostics) {
            this.owner = owner;
            this.sessionId = sessionId;
            this.cellIndex = cellIndex;
            this.center = center;
            this.radius = radius;
            this.seed = seed;
            this.committed = new ArrayList<>(committed);
            this.generator = new TestRealityGenerator(seed);
            this.diagnostics = diagnostics;
            int minX = (center.x - radius) << 4;
            int minZ = (center.z - radius) << 4;
            int maxX = ((center.x + radius + 1) << 4) - 1;
            int maxZ = ((center.z + radius + 1) << 4) - 1;
            this.bounds = new AABB(minX, TestRealityGenerator.CLEAR_MIN_Y, minZ,
                maxX + 1.0D, TestRealityGenerator.CLEAR_MAX_Y + 1.0D, maxZ + 1.0D);
        }

        private boolean contains(ChunkPos chunk) {
            return Math.abs(chunk.x - center.x) <= radius && Math.abs(chunk.z - center.z) <= radius;
        }
        private boolean contains(BlockPos position) { return bounds.contains(position.getX() + 0.5D,
            position.getY() + 0.5D, position.getZ() + 0.5D); }
        private TestRealityData.FillOperation toRelative(TestRealityData.FillOperation absolute) {
            int x = center.getMiddleBlockX();
            int z = center.getMiddleBlockZ();
            return new TestRealityData.FillOperation(absolute.min().offset(-x, 0, -z),
                absolute.max().offset(-x, 0, -z), absolute.block());
        }
        private TestRealityData.FillOperation realize(TestRealityData.FillOperation relative) {
            int x = center.getMiddleBlockX();
            int z = center.getMiddleBlockZ();
            return new TestRealityData.FillOperation(relative.min().offset(x, 0, z),
                relative.max().offset(x, 0, z), relative.block());
        }
        private List<TestRealityData.FillOperation> realizedEdits() {
            List<TestRealityData.FillOperation> all = new ArrayList<>(committed.size() + staged.size());
            committed.forEach(operation -> all.add(realize(operation)));
            staged.forEach(operation -> all.add(realize(operation)));
            return all;
        }
    }

    public record Result(boolean success, String message) {
        public static Result success(String message) { return new Result(true, message); }
        public static Result failure(String message) { return new Result(false, message); }
    }
}
