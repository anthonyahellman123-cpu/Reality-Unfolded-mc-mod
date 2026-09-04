package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;
import com.anthonyahellman.realityunfolded.spell.runtime.TargetResolver;
import net.minecraft.world.entity.Entity;

public final class HomeWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.manifestation() == null) return WordOutcome.terminate(context);
        Entity target = context.entityTarget();
        if (target == null && context.entityQuery() != null) {
            target = TargetResolver.nearest(context, context.entityQuery());
        }
        // Compatibility: only truly bare Stage-2 HOME retains nearest non-caster fallback.
        boolean legacyFallback = target == null && context.entityQuery() == null;
        context.manifestation().enableHoming(target, legacyFallback);
        SpellExecutionContext resolved = target == null ? context : context.withEntityTarget(target);
        SpellDebug.target(resolved, target);
        return WordOutcome.continueWith(resolved);
    }
}
