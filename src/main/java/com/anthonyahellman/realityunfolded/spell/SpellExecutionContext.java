package com.anthonyahellman.realityunfolded.spell;

import com.anthonyahellman.realityunfolded.entity.SpellBoltEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public record SpellExecutionContext(
    ServerLevel level,
    UUID casterId,
    UUID castId,
    UUID branchId,
    @Nullable UUID parentBranchId,
    SpellPhase phase,
    Vec3 position,
    @Nullable SpellBoltEntity manifestation,
    @Nullable Entity entityTarget,
    @Nullable BlockPos blockTarget,
    @Nullable Direction blockFace,
    double basePower
) {
    @Nullable
    public ServerPlayer caster() {
        return level.getServer().getPlayerList().getPlayer(casterId);
    }

    public double power(SpellNode node) {
        return basePower * node.powerMultiplier();
    }

    public SpellExecutionContext withManifestation(SpellBoltEntity bolt) {
        return new SpellExecutionContext(level, casterId, castId, bolt.manifestationId(),
            bolt.parentManifestationId(), SpellPhase.MANIFESTATION, bolt.position(), bolt,
            null, null, null, basePower);
    }

    public SpellExecutionContext atImpact(Vec3 impactPosition, @Nullable Entity target,
                                          @Nullable BlockPos block, @Nullable Direction face) {
        return new SpellExecutionContext(level, casterId, castId, branchId, parentBranchId,
            SpellPhase.IMPACT, impactPosition, manifestation, target, block, face, basePower);
    }
}
