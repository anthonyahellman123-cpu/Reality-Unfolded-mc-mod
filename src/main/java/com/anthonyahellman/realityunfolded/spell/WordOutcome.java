package com.anthonyahellman.realityunfolded.spell;

public record WordOutcome(WordDisposition disposition, SpellExecutionContext context) {
    public static WordOutcome continueWith(SpellExecutionContext context) {
        return new WordOutcome(WordDisposition.CONTINUE, context);
    }

    public static WordOutcome suspend(SpellExecutionContext context) {
        return new WordOutcome(WordDisposition.SUSPEND, context);
    }

    public static WordOutcome terminate(SpellExecutionContext context) {
        return new WordOutcome(WordDisposition.TERMINATE, context);
    }
}
