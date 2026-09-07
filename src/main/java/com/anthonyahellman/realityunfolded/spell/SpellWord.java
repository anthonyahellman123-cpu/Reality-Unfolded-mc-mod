package com.anthonyahellman.realityunfolded.spell;

public interface SpellWord {
    WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context);
}
