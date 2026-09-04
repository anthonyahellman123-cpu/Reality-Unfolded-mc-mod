package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;

public final class AccelerateWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.manifestation() == null) return WordOutcome.terminate(context);
        context.manifestation().accelerate();
        return WordOutcome.continueWith(context);
    }
}
