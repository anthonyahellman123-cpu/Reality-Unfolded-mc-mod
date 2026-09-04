package com.anthonyahellman.realityunfolded.spell.runtime;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import com.anthonyahellman.realityunfolded.spell.SpellExecutor;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Bounded server-owned continuation scheduler for DELAY. */
@Mod.EventBusSubscriber(modid = RealityUnfolded.MOD_ID)
public final class DelayedSpellRuntime {
    public static final int MAX_DELAY_TICKS = 200;
    public static final int MAX_LEVEL_CONTINUATIONS = 256;
    public static final int MAX_CAST_CONTINUATIONS = 32;
    private static final Map<ServerLevel, List<Continuation>> CONTINUATIONS = new WeakHashMap<>();

    private DelayedSpellRuntime() {}

    public static synchronized boolean schedule(SpellProgram program, List<Integer> nodes,
                                                SpellExecutionContext context, int delayTicks) {
        List<Continuation> levelContinuations = CONTINUATIONS.computeIfAbsent(context.level(),
            ignored -> new ArrayList<>());
        long castCount = levelContinuations.stream()
            .filter(continuation -> continuation.context.castId().equals(context.castId())).count();
        if (levelContinuations.size() >= MAX_LEVEL_CONTINUATIONS || castCount >= MAX_CAST_CONTINUATIONS) {
            RealityUnfolded.LOGGER.warn("[RU SPELL] DELAY_REJECTED cast={} levelCount={} castCount={}",
                context.castId(), levelContinuations.size(), castCount);
            return false;
        }
        int safeDelay = Math.max(1, Math.min(MAX_DELAY_TICKS, delayTicks));
        levelContinuations.add(new Continuation(context.level().getGameTime() + safeDelay,
            program, List.copyOf(nodes), context.delayed()));
        RealityUnfolded.LOGGER.info("[RU SPELL] CONTINUATION_SUSPENDED cast={} branch={} ticks={} nodes={}",
            context.castId(), context.branchId(), safeDelay, nodes);
        return true;
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()
            || !(event.level instanceof ServerLevel level)) return;
        resumeDue(level);
    }

    static synchronized void resumeDue(ServerLevel level) {
        List<Continuation> continuations = CONTINUATIONS.get(level);
        if (continuations == null) return;
        long now = level.getGameTime();
        Iterator<Continuation> iterator = continuations.iterator();
        List<Continuation> ready = new ArrayList<>();
        while (iterator.hasNext()) {
            Continuation continuation = iterator.next();
            if (continuation.dueTick <= now) {
                iterator.remove();
                ready.add(continuation);
            }
        }
        if (continuations.isEmpty()) CONTINUATIONS.remove(level);
        for (Continuation continuation : ready) resume(continuation);
    }

    private static void resume(Continuation continuation) {
        SpellExecutionContext context = continuation.context;
        if (context.caster() == null
            || (context.manifestation() != null && context.manifestation().isRemoved())) {
            RealityUnfolded.LOGGER.info("[RU SPELL] CONTINUATION_DROPPED cast={} branch={} reason=owner_or_manifestation_gone",
                context.castId(), context.branchId());
            return;
        }
        RealityUnfolded.LOGGER.info("[RU SPELL] CONTINUATION_RESUMED cast={} branch={} nodes={}",
            context.castId(), context.branchId(), continuation.nodes);
        if (context.manifestation() != null) context.manifestation().resumeFromDelay();
        for (int node : continuation.nodes) SpellExecutor.execute(continuation.program, node, context);
    }

    static synchronized int continuationCount(ServerLevel level, UUID castId) {
        return (int) CONTINUATIONS.getOrDefault(level, List.of()).stream()
            .filter(value -> value.context.castId().equals(castId)).count();
    }

    private record Continuation(long dueTick, SpellProgram program, List<Integer> nodes,
                                SpellExecutionContext context) {}
}
