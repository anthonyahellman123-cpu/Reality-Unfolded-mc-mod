package com.anthonyahellman.realityunfolded.spell.runtime;

import com.anthonyahellman.realityunfolded.spell.EntityQuery;
import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public final class TargetResolver {
    public static final double QUERY_RADIUS = 32.0D;
    public static final int MAX_CANDIDATES = 64;

    private TargetResolver() {}

    public static List<LivingEntity> candidates(SpellExecutionContext context, EntityQuery query) {
        AABB area = new AABB(context.position(), context.position()).inflate(QUERY_RADIUS);
        return context.level().getEntitiesOfClass(LivingEntity.class, area, entity -> matches(context, query, entity))
            .stream()
            .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(context.position())))
            .limit(MAX_CANDIDATES)
            .toList();
    }

    @Nullable
    public static LivingEntity nearest(SpellExecutionContext context, EntityQuery query) {
        List<LivingEntity> candidates = candidates(context, query);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    public static boolean any(SpellExecutionContext context, EntityQuery query) {
        return !candidates(context, query).isEmpty();
    }

    private static boolean matches(SpellExecutionContext context, EntityQuery query, LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()) return false;
        if (!query.includeCaster() && entity.getUUID().equals(context.casterId())) return false;
        if (query.kind() == EntityQuery.Kind.PLAYER && !(entity instanceof Player)) return false;
        return !query.hostileOnly() || entity instanceof Enemy;
    }

    public static double blockReach(ServerPlayer player) {
        return Math.max(1.0D, player.getAttributeValue(ForgeMod.BLOCK_REACH.get()));
    }

    public static double entityReach(ServerPlayer player) {
        return Math.max(1.0D, player.getAttributeValue(ForgeMod.ENTITY_REACH.get()));
    }
}
