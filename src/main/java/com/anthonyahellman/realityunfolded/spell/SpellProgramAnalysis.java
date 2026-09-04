package com.anthonyahellman.realityunfolded.spell;

/** Deterministic information that both validation and player presentation can trust. */
public final class SpellProgramAnalysis {
    public static final int MAX_MANIFESTATIONS = 64;

    private SpellProgramAnalysis() {}

    public static int estimatedManifestations(SpellProgram program) {
        long count = program.nodes().stream().anyMatch(node -> node.word() == SpellWordId.BOLT) ? 1L : 0L;
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
            .map(node -> node.word() == SpellWordId.SPLIT && node.integerArgument() != 2
                ? "SPLIT(" + node.integerArgument() + ")" : node.word().name())
            .reduce((left, right) -> left + " " + right)
            .orElse("");
    }
}
