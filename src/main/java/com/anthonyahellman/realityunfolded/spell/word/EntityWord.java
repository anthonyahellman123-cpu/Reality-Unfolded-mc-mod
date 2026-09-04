package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;
import com.anthonyahellman.realityunfolded.spell.runtime.TargetResolver;

public final class EntityWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        EntityQuery query = EntityQuery.entities();
        Boolean result = context.sensing() ? TargetResolver.any(context, query) : null;
        SpellExecutionContext refined = context.withEntityTarget(null).withQuery(query, result);
        SpellDebug.context(refined, "ENTITY query candidates=" + TargetResolver.candidates(refined, query).size());
        return WordOutcome.continueWith(refined);
    }
}
