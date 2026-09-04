package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;

public final class GravityWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.manifestation() == null) return WordOutcome.terminate(context);
        context.manifestation().setGravityDirection(1);
        return WordOutcome.continueWith(context);
    }
}
