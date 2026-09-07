package com.anthonyahellman.realityunfolded.spell;

import com.anthonyahellman.realityunfolded.entity.SpellManifestationEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/** Immutable, branch-local runtime state. Persistent player knowledge never lives here. */
public record SpellExecutionContext(
    ServerLevel level,
    UUID casterId,
    UUID castId,
    UUID branchId,
    @Nullable UUID parentBranchId,
    SpellPhase phase,
    Vec3 position,
    @Nullable SpellManifestationEntity manifestation,
    @Nullable Entity entityTarget,
    @Nullable BlockPos blockTarget,
    @Nullable Direction blockFace,
    @Nullable EntityQuery entityQuery,
    @Nullable Boolean condition,
    boolean sensing,
    boolean releaseAvailable,
    double basePower
) {
    @Nullable
    public ServerPlayer caster() {
        return level.getServer().getPlayerList().getPlayer(casterId);
    }

    public double power(SpellNode node) {
        return basePower * node.powerMultiplier();
    }

    public SpellExecutionContext withManifestation(SpellManifestationEntity value) {
        return copy(value.manifestationId(), value.parentManifestationId(), SpellPhase.MANIFESTATION,
            value.position(), value, entityTarget, blockTarget, blockFace, entityQuery, condition,
            sensing, releaseAvailable);
    }

    public SpellExecutionContext withEntityTarget(@Nullable Entity target) {
        return copy(branchId, parentBranchId, phase, position, manifestation, target,
            null, null, entityQuery, target != null, sensing, releaseAvailable);
    }

    public SpellExecutionContext withBlockTarget(@Nullable BlockPos block, @Nullable Direction face) {
        return copy(branchId, parentBranchId, phase, position, manifestation, null,
            block, face, entityQuery, block != null, sensing, releaseAvailable);
    }

    public SpellExecutionContext withQuery(EntityQuery query, @Nullable Boolean result) {
        return copy(branchId, parentBranchId, phase, position, manifestation, entityTarget,
            blockTarget, blockFace, query, result, sensing, releaseAvailable);
    }

    public SpellExecutionContext withCondition(@Nullable Boolean result) {
        return copy(branchId, parentBranchId, phase, position, manifestation, entityTarget,
            blockTarget, blockFace, entityQuery, result, sensing, releaseAvailable);
    }

    public SpellExecutionContext beginSensing() {
        return copy(branchId, parentBranchId, phase, position, manifestation, entityTarget,
            blockTarget, blockFace, entityQuery, null, true, releaseAvailable);
    }

    public SpellExecutionContext delayed() {
        return copy(branchId, parentBranchId, SpellPhase.DELAYED, position, manifestation, entityTarget,
            blockTarget, blockFace, entityQuery, condition, sensing, true);
    }

    public SpellExecutionContext consumeRelease() {
        return copy(branchId, parentBranchId, phase, position, manifestation, entityTarget,
            blockTarget, blockFace, entityQuery, condition, sensing, false);
    }

    public SpellExecutionContext atImpact(Vec3 impactPosition, @Nullable Entity target,
                                          @Nullable BlockPos block, @Nullable Direction face) {
        return copy(branchId, parentBranchId, SpellPhase.IMPACT, impactPosition, manifestation,
            target, block, face, entityQuery, condition, sensing, releaseAvailable);
    }

    private SpellExecutionContext copy(UUID nextBranchId, @Nullable UUID nextParentBranchId,
                                       SpellPhase nextPhase, Vec3 nextPosition,
                                       @Nullable SpellManifestationEntity nextManifestation,
                                       @Nullable Entity nextEntityTarget, @Nullable BlockPos nextBlockTarget,
                                       @Nullable Direction nextBlockFace, @Nullable EntityQuery nextQuery,
                                       @Nullable Boolean nextCondition, boolean nextSensing,
                                       boolean nextReleaseAvailable) {
        return new SpellExecutionContext(level, casterId, castId, nextBranchId, nextParentBranchId,
            nextPhase, nextPosition, nextManifestation, nextEntityTarget, nextBlockTarget, nextBlockFace,
            nextQuery, nextCondition, nextSensing, nextReleaseAvailable, basePower);
    }
}
