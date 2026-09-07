package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;

public final class SelfWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        return context.caster() == null ? WordOutcome.terminate(context.withCondition(false))
            : WordOutcome.continueWith(context.withEntityTarget(context.caster()));
    }
}
