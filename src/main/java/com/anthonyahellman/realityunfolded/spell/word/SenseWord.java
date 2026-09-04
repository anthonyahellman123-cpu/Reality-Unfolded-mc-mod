package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;

public final class SenseWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        return WordOutcome.continueWith(context.beginSensing());
    }
}
