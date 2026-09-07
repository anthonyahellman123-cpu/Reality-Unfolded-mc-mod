package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import com.anthonyahellman.realityunfolded.spell.SpellNode;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellWord;
import com.anthonyahellman.realityunfolded.spell.WordOutcome;

public final class BreakWord implements SpellWord {
    @Override
    public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.blockTarget() == null) return WordOutcome.terminate(context);
        context.level().destroyBlock(context.blockTarget(), true, context.caster());
        return WordOutcome.continueWith(context);
    }
}
