package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.entity.SpellBoltEntity;
import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import com.anthonyahellman.realityunfolded.spell.SpellNode;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellWord;
import com.anthonyahellman.realityunfolded.spell.WordOutcome;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class BoltWord implements SpellWord {
    @Override
    public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        Vec3 direction = direction(context);
        SpellBoltEntity bolt = SpellBoltEntity.create(context.level(), program, context.casterId(),
            context.castId(), context.branchId(), context.position(), direction, context.power(node));
        context.level().addFreshEntity(bolt);
        return WordOutcome.continueWith(context.withManifestation(bolt));
    }

    private Vec3 direction(SpellExecutionContext context) {
        ServerPlayer caster = context.caster();
        if (caster != null) return caster.getLookAngle().normalize();
        if (context.entityTarget() != null) {
            Vec3 delta = context.entityTarget().getEyePosition().subtract(context.position());
            if (delta.lengthSqr() > 0.0001D) return delta.normalize();
        }
        return new Vec3(0.0D, 0.0D, 1.0D);
    }
}
