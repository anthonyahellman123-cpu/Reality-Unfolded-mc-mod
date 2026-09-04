package com.anthonyahellman.realityunfolded.entity;

import com.anthonyahellman.realityunfolded.spell.SpellDebug;
import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import com.anthonyahellman.realityunfolded.spell.SpellExecutor;
import com.anthonyahellman.realityunfolded.spell.SpellPhase;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.runtime.LinkRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Shared server-owned behavior for distinct manifestation forms. */
public abstract class SpellManifestationEntity extends ThrowableProjectile {
    private static final double LEGACY_HOME_RANGE = 16.0D;
    private static final double HOME_STEERING = 0.18D;
    private static final double MAX_SPEED = 4.0D;
    private static final double GRAVITY_ACCELERATION = 0.035D;

    protected SpellProgram program;
    protected UUID casterId;
    protected UUID castId;
    protected UUID manifestationId = UUID.randomUUID();
    @Nullable protected UUID parentManifestationId;
    protected double basePower = 1.0D;
    private boolean homing;
    private boolean legacyHomeFallback;
    @Nullable private UUID homingTargetId;
    private int gravityDirection;
    private int[] impactContinuations = new int[0];
    private boolean delayed;
    private Vec3 storedDelayMotion = Vec3.ZERO;

    protected SpellManifestationEntity(EntityType<? extends SpellManifestationEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    protected final void initialize(SpellProgram spellProgram, UUID caster, UUID cast,
                                    @Nullable UUID parent, Vec3 position, Vec3 direction,
                                    double power, double speed) {
        program = spellProgram;
        casterId = caster;
        castId = cast;
        parentManifestationId = parent;
        basePower = Math.max(0.01D, power);
        setPos(position);
        Vec3 safeDirection = direction.lengthSqr() < 0.0001D ? new Vec3(0, 0, 1) : direction.normalize();
        setDeltaMovement(safeDirection.scale(speed));
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && !delayed) {
            if (homing) steerTowardTarget();
            applyContinuousGravity();
        }
        super.tick();
        if (level().isClientSide()) {
            clientParticles();
        } else if (level() instanceof ServerLevel serverLevel) {
            serverParticles(serverLevel);
            LinkRuntime.tickVisuals(serverLevel, this);
            if (tickCount > maximumLifetimeTicks()) {
                SpellDebug.termination(context(SpellPhase.MANIFESTATION), "manifestation lifetime expired");
                discard();
            }
        }
    }

    protected abstract int maximumLifetimeTicks();
    protected abstract void clientParticles();
    protected abstract void serverParticles(ServerLevel level);
    protected abstract SpellManifestationEntity createChild(ServerLevel level, @Nullable UUID parent,
                                                             Vec3 position, Vec3 direction, double power);

    protected boolean supportsImpact() {
        return true;
    }

    public final void enableHoming(@Nullable Entity target, boolean allowLegacyFallback) {
        homing = true;
        homingTargetId = target == null ? null : target.getUUID();
        legacyHomeFallback = allowLegacyFallback;
    }

    public final void accelerate() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 0.0001D) motion = new Vec3(0, 0, 0.08D);
        double speed = Math.min(MAX_SPEED, motion.length() * 1.5D);
        setDeltaMovement(motion.normalize().scale(speed));
        hasImpulse = true;
    }

    public final void setGravityDirection(int direction) {
        gravityDirection = Integer.compare(direction, 0);
    }

    public final void suspendForDelay() {
        if (delayed) return;
        storedDelayMotion = getDeltaMovement();
        setDeltaMovement(Vec3.ZERO);
        delayed = true;
        hasImpulse = true;
    }

    public final void resumeFromDelay() {
        if (!delayed) return;
        setDeltaMovement(storedDelayMotion);
        delayed = false;
        hasImpulse = true;
    }

    public final boolean armImpact(List<Integer> continuations) {
        if (!supportsImpact()) return false;
        impactContinuations = continuations.stream().mapToInt(Integer::intValue).toArray();
        return true;
    }

    public final void split(int childCount, List<Integer> continuations,
                            SpellExecutionContext parentContext) {
        if (!(level() instanceof ServerLevel serverLevel) || program == null) return;
        int safeChildren = Math.max(2, Math.min(16, childCount));
        Vec3 currentDirection = getDeltaMovement().lengthSqr() < 0.0001D
            ? new Vec3(0, 0, 1) : getDeltaMovement().normalize();
        double spreadDegrees = Math.min(42.0D, 12.0D * (safeChildren - 1));
        for (int index = 0; index < safeChildren; index++) {
            double fraction = safeChildren == 1 ? 0.5D : (double) index / (safeChildren - 1);
            double yaw = Math.toRadians(-spreadDegrees / 2.0D + spreadDegrees * fraction);
            Vec3 direction = currentDirection.yRot((float) yaw);
            SpellManifestationEntity child = createChild(serverLevel, manifestationId,
                position().add(direction.scale(0.2D)), direction, basePower);
            child.homing = homing;
            child.homingTargetId = homingTargetId;
            child.legacyHomeFallback = legacyHomeFallback;
            child.gravityDirection = gravityDirection;
            serverLevel.addFreshEntity(child);
            SpellDebug.child(context(SpellPhase.MANIFESTATION), child.manifestationId());
            SpellExecutionContext childContext = parentContext.withManifestation(child);
            for (int continuation : continuations) SpellExecutor.execute(program, continuation, childContext);
        }
        discard();
    }

    private void steerTowardTarget() {
        LivingEntity target = resolvedHomeTarget();
        if (target == null) return;
        Vec3 desired = target.getEyePosition().subtract(position());
        Vec3 current = getDeltaMovement();
        if (desired.lengthSqr() < 0.0001D || current.lengthSqr() < 0.0001D) return;
        double speed = current.length();
        setDeltaMovement(current.normalize().scale(1.0D - HOME_STEERING)
            .add(desired.normalize().scale(HOME_STEERING)).normalize().scale(speed));
        hasImpulse = true;
    }

    @Nullable
    private LivingEntity resolvedHomeTarget() {
        if (homingTargetId != null && level() instanceof ServerLevel serverLevel) {
            Entity resolved = serverLevel.getEntity(homingTargetId);
            if (resolved instanceof LivingEntity living && living.isAlive()) return living;
        }
        if (!legacyHomeFallback) return null;
        List<LivingEntity> candidates = new ArrayList<>(level().getEntitiesOfClass(LivingEntity.class,
            getBoundingBox().inflate(LEGACY_HOME_RANGE), this::isValidLegacyHomeTarget));
        candidates.sort(Comparator.comparingDouble(this::distanceToSqr));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean isValidLegacyHomeTarget(LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && !entity.getUUID().equals(casterId);
    }

    private void applyContinuousGravity() {
        if (gravityDirection == 0) return;
        Vec3 motion = getDeltaMovement().add(0.0D, -gravityDirection * GRAVITY_ACCELERATION, 0.0D);
        if (motion.length() > MAX_SPEED) motion = motion.normalize().scale(MAX_SPEED);
        setDeltaMovement(motion);
        hasImpulse = true;
    }

    @Override
    protected void onHit(HitResult result) {
        if (!supportsImpact() || level().isClientSide() || !(level() instanceof ServerLevel) || program == null) return;
        Entity target = result instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        BlockPos block = result instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null;
        Direction face = result instanceof BlockHitResult blockHit ? blockHit.getDirection() : null;
        SpellExecutionContext impact = context(SpellPhase.IMPACT).atImpact(result.getLocation(), target, block, face);
        SpellDebug.impact(impact);
        for (int continuation : impactContinuations) SpellExecutor.execute(program, continuation, impact);
        discard();
    }

    public final SpellExecutionContext context(SpellPhase phase) {
        return new SpellExecutionContext((ServerLevel) level(), casterId, castId, manifestationId,
            parentManifestationId, phase, position(), this, null, null, null,
            null, null, false, false, basePower);
    }

    public final UUID manifestationId() { return manifestationId; }
    @Nullable public final UUID parentManifestationId() { return parentManifestationId; }
    public final UUID castId() { return castId; }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
            LinkRuntime.unregister(serverLevel, this);
        }
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (program != null) tag.put("program", program.save());
        if (casterId != null) tag.putUUID("caster", casterId);
        if (castId != null) tag.putUUID("cast", castId);
        tag.putUUID("manifestation", manifestationId);
        if (parentManifestationId != null) tag.putUUID("parent_manifestation", parentManifestationId);
        if (homingTargetId != null) tag.putUUID("homing_target", homingTargetId);
        tag.putDouble("base_power", basePower);
        tag.putBoolean("homing", homing);
        tag.putBoolean("legacy_home_fallback", legacyHomeFallback);
        tag.putInt("gravity_direction", gravityDirection);
        tag.putIntArray("impact_continuations", impactContinuations);
        tag.putBoolean("delayed", delayed);
        tag.putDouble("delay_motion_x", storedDelayMotion.x);
        tag.putDouble("delay_motion_y", storedDelayMotion.y);
        tag.putDouble("delay_motion_z", storedDelayMotion.z);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("program")) program = SpellProgram.load(tag.getCompound("program"));
        if (tag.hasUUID("caster")) casterId = tag.getUUID("caster");
        if (tag.hasUUID("cast")) castId = tag.getUUID("cast");
        if (tag.hasUUID("manifestation")) manifestationId = tag.getUUID("manifestation");
        parentManifestationId = tag.hasUUID("parent_manifestation") ? tag.getUUID("parent_manifestation") : null;
        homingTargetId = tag.hasUUID("homing_target") ? tag.getUUID("homing_target") : null;
        basePower = tag.getDouble("base_power");
        homing = tag.getBoolean("homing");
        legacyHomeFallback = tag.getBoolean("legacy_home_fallback");
        gravityDirection = tag.getInt("gravity_direction");
        impactContinuations = tag.getIntArray("impact_continuations");
        delayed = tag.getBoolean("delayed");
        storedDelayMotion = new Vec3(tag.getDouble("delay_motion_x"), tag.getDouble("delay_motion_y"),
            tag.getDouble("delay_motion_z"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
