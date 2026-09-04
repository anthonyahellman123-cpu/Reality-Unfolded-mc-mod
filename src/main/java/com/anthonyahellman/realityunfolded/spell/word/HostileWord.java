package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;
import com.anthonyahellman.realityunfolded.spell.runtime.TargetResolver;

public final class HostileWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.entityQuery() == null) return WordOutcome.terminate(context.withCondition(false));
        EntityQuery query = context.entityQuery().hostile();
        boolean found = TargetResolver.any(context, query);
        SpellExecutionContext refined = context.withQuery(query, context.sensing() ? found : context.condition());
        SpellDebug.condition(refined, "HOSTILE", found);
        return WordOutcome.continueWith(refined);
    }
}
