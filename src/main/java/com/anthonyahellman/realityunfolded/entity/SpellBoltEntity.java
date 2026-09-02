package com.anthonyahellman.realityunfolded.entity;

import com.anthonyahellman.realityunfolded.spell.SpellDebug;
import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import com.anthonyahellman.realityunfolded.spell.SpellExecutor;
import com.anthonyahellman.realityunfolded.spell.SpellPhase;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.runtime.LinkRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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

public final class SpellBoltEntity extends ThrowableProjectile {
    private static final int MAX_LIFETIME_TICKS = 120;
    private static final double HOME_RANGE = 16.0D;
    private static final double HOME_STEERING = 0.18D;

    private SpellProgram program;
    private UUID casterId;
    private UUID castId;
    private UUID manifestationId = UUID.randomUUID();
    @Nullable private UUID parentManifestationId;
    private double basePower = 1.0D;
    private boolean homing;
    private int[] impactContinuations = new int[0];

    public SpellBoltEntity(EntityType<? extends SpellBoltEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static SpellBoltEntity create(ServerLevel level, SpellProgram program, UUID casterId,
                                         UUID castId, @Nullable UUID parentManifestationId,
                                         Vec3 position, Vec3 direction, double basePower) {
        SpellBoltEntity bolt = new SpellBoltEntity(ModEntities.SPELL_BOLT.get(), level);
        bolt.program = program;
        bolt.casterId = casterId;
        bolt.castId = castId;
        bolt.parentManifestationId = parentManifestationId;
        bolt.basePower = Math.max(0.01D, basePower);
        bolt.setPos(position);
        Vec3 safeDirection = direction.lengthSqr() < 0.0001D ? new Vec3(0, 0, 1) : direction.normalize();
        bolt.setDeltaMovement(safeDirection.scale(1.35D));
        return bolt;
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && homing) steerTowardTarget();
        super.tick();

        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(),
                -getDeltaMovement().x * 0.03D, -getDeltaMovement().y * 0.03D, -getDeltaMovement().z * 0.03D);
            level().addParticle(ParticleTypes.WITCH, getX(), getY(), getZ(), 0, 0, 0);
        } else if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(),
                2, 0.06D, 0.06D, 0.06D, 0.01D);
            LinkRuntime.tickVisuals(serverLevel, this);
            if (tickCount > MAX_LIFETIME_TICKS) {
                SpellDebug.termination(context(SpellPhase.MANIFESTATION), "manifestation lifetime expired");
                discard();
            }
        }
    }

    private void steerTowardTarget() {
        List<LivingEntity> candidates = new ArrayList<>(level().getEntitiesOfClass(LivingEntity.class,
            getBoundingBox().inflate(HOME_RANGE), this::isValidHomeTarget));
        candidates.sort(Comparator.comparingDouble(this::distanceToSqr));
        if (candidates.isEmpty()) return;

        LivingEntity target = candidates.get(0);
        Vec3 desired = target.getEyePosition().subtract(position());
        Vec3 current = getDeltaMovement();
        if (desired.lengthSqr() < 0.0001D || current.lengthSqr() < 0.0001D) return;
        double speed = current.length();
        Vec3 steered = current.normalize().scale(1.0D - HOME_STEERING)
            .add(desired.normalize().scale(HOME_STEERING)).normalize().scale(speed);
        setDeltaMovement(steered);
    }

    private boolean isValidHomeTarget(LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && (casterId == null || !casterId.equals(entity.getUUID()));
    }

    public void enableHoming() {
        homing = true;
    }

    public void armImpact(List<Integer> continuations) {
        impactContinuations = continuations.stream().mapToInt(Integer::intValue).toArray();
    }

    public void split(int childCount, List<Integer> continuations) {
        if (!(level() instanceof ServerLevel serverLevel) || program == null) return;
        Vec3 currentDirection = getDeltaMovement().lengthSqr() < 0.0001D
            ? new Vec3(0, 0, 1) : getDeltaMovement().normalize();
        double spreadDegrees = Math.min(42.0D, 12.0D * (childCount - 1));

        for (int index = 0; index < childCount; index++) {
            double fraction = childCount == 1 ? 0.5D : (double) index / (childCount - 1);
            double yaw = Math.toRadians(-spreadDegrees / 2.0D + spreadDegrees * fraction);
            Vec3 direction = currentDirection.yRot((float) yaw);
            SpellBoltEntity child = create(serverLevel, program, casterId, castId, manifestationId,
                position().add(direction.scale(0.2D)), direction, basePower);
            child.homing = homing;
            serverLevel.addFreshEntity(child);
            SpellDebug.child(context(SpellPhase.MANIFESTATION), child.manifestationId());
            SpellExecutionContext childContext = child.context(SpellPhase.MANIFESTATION);
            for (int continuation : continuations) SpellExecutor.execute(program, continuation, childContext);
        }
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (level().isClientSide() || !(level() instanceof ServerLevel) || program == null) return;

        Entity target = null;
        BlockPos block = null;
        Direction face = null;
        if (result instanceof EntityHitResult entityHit) target = entityHit.getEntity();
        if (result instanceof BlockHitResult blockHit) {
            block = blockHit.getBlockPos();
            face = blockHit.getDirection();
        }
        SpellExecutionContext impact = context(SpellPhase.IMPACT).atImpact(result.getLocation(), target, block, face);
        SpellDebug.impact(impact);
        for (int continuation : impactContinuations) SpellExecutor.execute(program, continuation, impact);
        discard();
    }

    private SpellExecutionContext context(SpellPhase phase) {
        return new SpellExecutionContext((ServerLevel) level(), casterId, castId, manifestationId,
            parentManifestationId, phase, position(), this, null, null, null, basePower);
    }

    public UUID manifestationId() {
        return manifestationId;
    }

    @Nullable
    public UUID parentManifestationId() {
        return parentManifestationId;
    }

    public UUID castId() {
        return castId;
    }

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
        tag.putDouble("base_power", basePower);
        tag.putBoolean("homing", homing);
        tag.putIntArray("impact_continuations", impactContinuations);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("program")) program = SpellProgram.load(tag.getCompound("program"));
        if (tag.hasUUID("caster")) casterId = tag.getUUID("caster");
        if (tag.hasUUID("cast")) castId = tag.getUUID("cast");
        if (tag.hasUUID("manifestation")) manifestationId = tag.getUUID("manifestation");
        parentManifestationId = tag.hasUUID("parent_manifestation") ? tag.getUUID("parent_manifestation") : null;
        basePower = tag.getDouble("base_power");
        homing = tag.getBoolean("homing");
        impactContinuations = tag.getIntArray("impact_continuations");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
