package com.anthonyahellman.realityunfolded.entity;

import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public final class SpellBoltEntity extends SpellManifestationEntity {
    public SpellBoltEntity(EntityType<? extends SpellBoltEntity> type, Level level) { super(type, level); }

    public static SpellBoltEntity create(ServerLevel level, SpellProgram program, UUID casterId,
                                         UUID castId, @Nullable UUID parent, Vec3 position,
                                         Vec3 direction, double power) {
        SpellBoltEntity bolt = new SpellBoltEntity(ModEntities.SPELL_BOLT.get(), level);
        bolt.initialize(program, casterId, castId, parent, position, direction, power, 1.35D);
        return bolt;
    }

    @Override protected int maximumLifetimeTicks() { return 120; }
    @Override protected void clientParticles() {
        level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(),
            -getDeltaMovement().x * 0.03D, -getDeltaMovement().y * 0.03D, -getDeltaMovement().z * 0.03D);
        level().addParticle(ParticleTypes.WITCH, getX(), getY(), getZ(), 0, 0, 0);
    }
    @Override protected void serverParticles(ServerLevel level) {
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 2, 0.06D, 0.06D, 0.06D, 0.01D);
    }
    @Override protected SpellManifestationEntity createChild(ServerLevel level, @Nullable UUID parent,
                                                              Vec3 position, Vec3 direction, double power) {
        return create(level, program, casterId, castId, parent, position, direction, power);
    }
}
