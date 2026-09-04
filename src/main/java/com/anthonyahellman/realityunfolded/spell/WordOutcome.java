package com.anthonyahellman.realityunfolded.spell;

import java.util.List;

public record WordOutcome(WordDisposition disposition, SpellExecutionContext context,
                          boolean overridesContinuation, List<Integer> continuation) {
    public WordOutcome { continuation = List.copyOf(continuation); }

    public static WordOutcome continueWith(SpellExecutionContext context) {
        return new WordOutcome(WordDisposition.CONTINUE, context, false, List.of());
    }
    public static WordOutcome branch(SpellExecutionContext context, int nodeId) {
        return new WordOutcome(WordDisposition.CONTINUE, context, true, List.of(nodeId));
    }
    public static WordOutcome suspend(SpellExecutionContext context) {
        return new WordOutcome(WordDisposition.SUSPEND, context, false, List.of());
    }
    public static WordOutcome terminate(SpellExecutionContext context) {
        return new WordOutcome(WordDisposition.TERMINATE, context, false, List.of());
    }
}
