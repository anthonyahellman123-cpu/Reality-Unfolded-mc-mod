package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.entity.SpellOrbEntity;
import com.anthonyahellman.realityunfolded.spell.*;
import net.minecraft.world.phys.Vec3;

public final class OrbWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        Vec3 direction = context.caster() != null ? context.caster().getLookAngle().normalize()
            : new Vec3(0, 0, 1);
        SpellOrbEntity orb = SpellOrbEntity.create(context.level(), program, context.casterId(),
            context.castId(), context.branchId(), context.position(), direction, context.power(node));
        context.level().addFreshEntity(orb);
        return WordOutcome.continueWith(context.withManifestation(orb));
    }
}
