package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import com.anthonyahellman.realityunfolded.spell.SpellNode;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellWord;
import com.anthonyahellman.realityunfolded.spell.WordOutcome;
import com.anthonyahellman.realityunfolded.spell.runtime.LinkRuntime;

public final class LinkWord implements SpellWord {
    @Override
    public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.manifestation() == null) return WordOutcome.terminate(context);
        LinkRuntime.register(context.level(), context.castId(), node.id(), context.manifestation());
        return WordOutcome.continueWith(context);
    }
}
