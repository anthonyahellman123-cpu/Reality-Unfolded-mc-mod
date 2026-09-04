package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;

/** TRUE follows the node edge; FALSE selects the implicit terminal branch. */
public final class IfWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.condition() == null) return WordOutcome.terminate(context);
        boolean selected = context.condition();
        SpellDebug.branch(context, node.id(), selected ? "TRUE" : "FALSE->TERMINAL");
        return WordOutcome.branch(context, selectBranch(node, selected));
    }

    public static int selectBranch(SpellNode node, boolean condition) {
        int branchIndex = condition ? 0 : 1;
        return node.next().size() > branchIndex ? node.next().get(branchIndex) : SpellProgram.TERMINAL;
    }
}
