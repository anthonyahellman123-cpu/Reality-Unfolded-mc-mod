package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;
import com.anthonyahellman.realityunfolded.spell.runtime.DelayedSpellRuntime;

public final class DelayWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.manifestation() != null) context.manifestation().suspendForDelay();
        if (DelayedSpellRuntime.schedule(program, node.next(), context, node.integerArgument())) {
            return WordOutcome.suspend(context);
        }
        if (context.manifestation() != null) context.manifestation().resumeFromDelay();
        return WordOutcome.terminate(context);
    }
}
