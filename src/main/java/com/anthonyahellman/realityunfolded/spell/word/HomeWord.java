package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import com.anthonyahellman.realityunfolded.spell.SpellNode;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellWord;
import com.anthonyahellman.realityunfolded.spell.WordOutcome;

public final class HomeWord implements SpellWord {
    @Override
    public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.manifestation() == null) return WordOutcome.terminate(context);
        context.manifestation().enableHoming();
        return WordOutcome.continueWith(context);
    }
}
