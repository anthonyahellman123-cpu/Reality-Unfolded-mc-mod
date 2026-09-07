package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;
import com.anthonyahellman.realityunfolded.spell.runtime.TargetResolver;
import net.minecraft.world.entity.LivingEntity;

public final class NearestWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.entityQuery() == null) return WordOutcome.terminate(context.withCondition(false));
        LivingEntity target = TargetResolver.nearest(context, context.entityQuery());
        SpellExecutionContext resolved = context.withEntityTarget(target).withQuery(context.entityQuery(), target != null);
        SpellDebug.target(resolved, target);
        return WordOutcome.continueWith(resolved);
    }
}
