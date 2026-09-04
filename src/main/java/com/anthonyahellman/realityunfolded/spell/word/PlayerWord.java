package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;
import com.anthonyahellman.realityunfolded.spell.runtime.TargetResolver;

public final class PlayerWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        EntityQuery query = EntityQuery.players();
        Boolean result = context.sensing() ? TargetResolver.any(context, query) : null;
        SpellExecutionContext refined = context.withEntityTarget(null).withQuery(query, result);
        SpellDebug.context(refined, "PLAYER query excludes caster; candidates="
            + TargetResolver.candidates(refined, query).size());
        return WordOutcome.continueWith(refined);
    }
}
