package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;

public final class NotWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.condition() == null) return WordOutcome.terminate(context);
        SpellExecutionContext negated = context.withCondition(!context.condition());
        SpellDebug.condition(negated, "NOT", negated.condition());
        return WordOutcome.continueWith(negated);
    }
}
