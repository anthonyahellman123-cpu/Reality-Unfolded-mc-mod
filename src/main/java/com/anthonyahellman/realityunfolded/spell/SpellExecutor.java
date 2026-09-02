package com.anthonyahellman.realityunfolded.spell;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SpellExecutor {
    private static final int MAX_NODES_PER_RESUME = 256;

    private SpellExecutor() {}

    public static void begin(SpellProgram program, SpellExecutionContext context) {
        SpellDebug.cast(program, context);
        execute(program, program.rootNode(), context);
    }

    public static void execute(SpellProgram program, int nodeId, SpellExecutionContext context) {
        execute(program, nodeId, context, new HashSet<>(), 0);
    }

    private static void execute(SpellProgram program, int nodeId, SpellExecutionContext context,
                                Set<Integer> activePath, int executed) {
        if (nodeId == SpellProgram.TERMINAL) {
            SpellDebug.termination(context, "terminal continuation");
            return;
        }
        if (executed >= MAX_NODES_PER_RESUME) {
            SpellDebug.termination(context, "execution budget exceeded");
            return;
        }
        if (!activePath.add(nodeId)) {
            SpellDebug.termination(context, "cycle detected at node " + nodeId);
            return;
        }

        SpellNode node = program.node(nodeId);
        if (node == null) {
            SpellDebug.termination(context, "missing node " + nodeId);
            return;
        }
        SpellDebug.node(node, context);
        SpellWord word = WordRegistry.get(node.word());
        WordOutcome outcome = word.execute(program, node, context);
        if (outcome.disposition() == WordDisposition.SUSPEND) return;
        if (outcome.disposition() == WordDisposition.TERMINATE) {
            SpellDebug.termination(outcome.context(), "word requested termination: " + node.word());
            return;
        }

        List<Integer> next = node.next();
        if (next.isEmpty()) {
            SpellDebug.termination(outcome.context(), "branch complete");
            return;
        }
        for (int child : next) {
            execute(program, child, outcome.context(), new HashSet<>(activePath), executed + 1);
        }
    }
}
