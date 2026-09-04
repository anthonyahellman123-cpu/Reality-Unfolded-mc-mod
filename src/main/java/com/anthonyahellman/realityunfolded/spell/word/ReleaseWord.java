package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;

public final class ReleaseWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        return context.releaseAvailable() ? WordOutcome.continueWith(context.consumeRelease())
            : WordOutcome.terminate(context);
    }
}
