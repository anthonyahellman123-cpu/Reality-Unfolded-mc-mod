package com.anthonyahellman.realityunfolded.spell;

/** Deterministic information that both validation and player presentation can trust. */
public final class SpellProgramAnalysis {
    public static final int MAX_MANIFESTATIONS = 64;

    private SpellProgramAnalysis() {}

    public static int estimatedManifestations(SpellProgram program) {
        long count = program.nodes().stream().anyMatch(node -> node.word() == SpellWordId.BOLT
            || node.word() == SpellWordId.ORB) ? 1L : 0L;
        for (SpellNode node : program.nodes()) {
            if (node.word() == SpellWordId.SPLIT && count > 0L) {
                count *= node.integerArgument();
                if (count > MAX_MANIFESTATIONS) return MAX_MANIFESTATIONS + 1;
            }
        }
        return (int) count;
    }

    public static String canonicalSource(SpellProgram program) {
        return program.nodes().stream()
            .map(SpellProgramAnalysis::sourceToken)
            .reduce((left, right) -> left + " " + right)
            .orElse("");
    }

    private static String sourceToken(SpellNode node) {
        if (node.word() == SpellWordId.SPLIT && node.integerArgument() == 2) return "SPLIT";
        if (node.integerArgument() > 0) return node.word().name() + "(" + node.integerArgument() + ")";
        return node.word().name();
    }
}
