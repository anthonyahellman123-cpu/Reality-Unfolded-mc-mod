package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import com.anthonyahellman.realityunfolded.spell.SpellNode;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellWord;
import com.anthonyahellman.realityunfolded.spell.WordOutcome;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class ExplosionWord implements SpellWord {
    private static final double FIXED_RADIUS = 4.0D;
    private static final float BASE_DAMAGE = 6.0F;

    @Override
    public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        context.level().sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            context.position().x, context.position().y, context.position().z, 1, 0, 0, 0, 0);
        context.level().playSound(null, context.position().x, context.position().y, context.position().z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, 0.8F + context.level().random.nextFloat() * 0.2F);

        AABB area = new AABB(context.position(), context.position()).inflate(FIXED_RADIUS);
        float peakDamage = (float) (BASE_DAMAGE * context.power(node));
        for (LivingEntity target : context.level().getEntitiesOfClass(LivingEntity.class, area)) {
            double distance = Math.sqrt(target.distanceToSqr(context.position()));
            if (distance > FIXED_RADIUS) continue;
            float falloffDamage = (float) (peakDamage * (1.0D - 0.65D * distance / FIXED_RADIUS));
            target.hurt(context.level().damageSources().explosion(context.caster(), context.caster()), falloffDamage);
        }
        return WordOutcome.continueWith(context);
    }
}
