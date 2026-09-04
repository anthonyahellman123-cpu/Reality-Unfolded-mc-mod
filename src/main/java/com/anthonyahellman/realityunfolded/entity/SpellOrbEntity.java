package com.anthonyahellman.realityunfolded.entity;

import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/** Persistent, non-impacting manifestation form; it is not a reskinned bolt. */
public final class SpellOrbEntity extends SpellManifestationEntity {
    public SpellOrbEntity(EntityType<? extends SpellOrbEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static SpellOrbEntity create(ServerLevel level, SpellProgram program, UUID casterId,
                                        UUID castId, @Nullable UUID parent, Vec3 position,
                                        Vec3 direction, double power) {
        SpellOrbEntity orb = new SpellOrbEntity(ModEntities.SPELL_ORB.get(), level);
        orb.initialize(program, casterId, castId, parent, position, direction, power, 0.08D);
        return orb;
    }

    @Override protected int maximumLifetimeTicks() { return 600; }
    @Override protected void clientParticles() {
        level().addParticle(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 0, 0.01D, 0);
        level().addParticle(ParticleTypes.ENCHANT, getX(), getY(), getZ(), 0, 0, 0);
    }
    @Override protected void serverParticles(ServerLevel level) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(),
            3, 0.16D, 0.16D, 0.16D, 0.01D);
    }
    @Override protected SpellManifestationEntity createChild(ServerLevel level, @Nullable UUID parent,
                                                              Vec3 position, Vec3 direction, double power) {
        return create(level, program, casterId, castId, parent, position, direction, power);
    }
    @Override protected boolean supportsImpact() { return false; }
}
