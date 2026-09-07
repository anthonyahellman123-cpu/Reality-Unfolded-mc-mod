package com.anthonyahellman.realityunfolded.spell.programmer;

import com.anthonyahellman.realityunfolded.spell.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Structural/safety compiler. Semantic dead ends are intentionally left to branch-local runtime resolution. */
public final class SpellGraphCompiler {
    private SpellGraphCompiler() {}

    public static SpellProgram compile(SpellGraphDraft graph) throws SpellValidationException {
        validateStructure(graph);
        Map<Integer, Double> powers = compiledPowers(graph);
        List<SpellNode> nodes = new ArrayList<>();
        for (SpellGraphDraft.Node graphNode : graph.nodes()) {
            List<Integer> next = new ArrayList<>();
            for (SpellPort port : WordRegistry.presentation(graphNode.word()).outputs()) {
                graph.edges().stream()
                    .filter(edge -> edge.from() == graphNode.id() && edge.port() == port)
                    .findFirst().ifPresent(edge -> next.add(edge.to()));
                if ((port == SpellPort.TRUE || port == SpellPort.FALSE)
                    && next.size() < WordRegistry.presentation(graphNode.word()).outputs().indexOf(port) + 1) {
                    next.add(SpellProgram.TERMINAL);
                }
            }
            nodes.add(new SpellNode(graphNode.id(), graphNode.word(), graphNode.integerArgument(),
                powers.getOrDefault(graphNode.id(), 1.0D), next));
        }
        SpellProgram program = new SpellProgram(graph.rootId(), nodes,
            "graph:" + Integer.toUnsignedString(SpellGraphCodec.encode(graph).hashCode(), 16));
        if (SpellProgramAnalysis.estimatedManifestations(program) > SpellProgramAnalysis.MAX_MANIFESTATIONS) {
            throw new SpellValidationException("Graph may exceed manifestation safety limit");
        }
        return program;
    }

    public static Set<Integer> connectedNodeIds(SpellGraphDraft graph) {
        Set<Integer> reached = new HashSet<>();
        visit(graph, graph.rootId(), reached);
        return reached;
    }

    private static void visit(SpellGraphDraft graph, int id, Set<Integer> reached) {
        if (id < 0 || !reached.add(id)) return;
        for (SpellGraphDraft.Edge edge : graph.edges()) {
            if (edge.from() == id) visit(graph, edge.to(), reached);
        }
    }

    private static void validateStructure(SpellGraphDraft graph) throws SpellValidationException {
        if (graph.nodes().isEmpty()) throw new SpellValidationException("Spell graph is empty");
        if (graph.nodes().size() > SpellGraphDraft.MAX_NODES || graph.edges().size() > SpellGraphDraft.MAX_EDGES) {
            throw new SpellValidationException("Spell graph exceeds safety limits");
        }
        Set<Integer> ids = new HashSet<>();
        for (SpellGraphDraft.Node node : graph.nodes()) {
            if (node.id() < 0 || !ids.add(node.id())) throw new SpellValidationException("Duplicate graph node id");
            validateArgument(node);
        }
        if (!ids.contains(graph.rootId())) throw new SpellValidationException("Spell graph has no valid root");
        Set<String> outputs = new HashSet<>();
        for (SpellGraphDraft.Edge edge : graph.edges()) {
            if (!ids.contains(edge.from()) || !ids.contains(edge.to())) {
                throw new SpellValidationException("Spell graph edge references a missing node");
            }
            SpellGraphDraft.Node source = graph.node(edge.from());
            if (!WordRegistry.presentation(source.word()).outputs().contains(edge.port())) {
                throw new SpellValidationException("Spell graph uses an unsupported output port");
            }
            if (!outputs.add(edge.from() + ":" + edge.port())) {
                throw new SpellValidationException("Spell graph output has multiple wires");
            }
        }
    }

    private static void validateArgument(SpellGraphDraft.Node node) throws SpellValidationException {
        var parameter = WordRegistry.presentation(node.word()).parameter();
        if (node.word() == SpellWordId.SPLIT && node.integerArgument() != 2) {
            throw new SpellValidationException("Visual SPLIT must use safe ×2 semantics");
        }
        if (parameter == null && node.word() != SpellWordId.SPLIT && node.integerArgument() != 0) {
            throw new SpellValidationException(node.word() + " does not accept a parameter");
        }
        if (parameter != null && (node.integerArgument() < parameter.minimum()
            || node.integerArgument() > parameter.maximum())) {
            throw new SpellValidationException(node.word() + " parameter exceeds safety bounds");
        }
    }

    private static Map<Integer, Double> compiledPowers(SpellGraphDraft graph) {
        Map<Integer, Double> powers = new HashMap<>();
        for (SpellGraphDraft.Node node : graph.nodes()) powers.put(node.id(), 1.0D);
        for (SpellGraphDraft.Node amplifier : graph.nodes()) {
            if (amplifier.word() != SpellWordId.AMPLIFY) continue;
            graph.edges().stream().filter(edge -> edge.to() == amplifier.id())
                .map(edge -> graph.node(edge.from())).filter(node -> node != null && node.word().acceptsPower())
                .min(Comparator.comparingInt(SpellGraphDraft.Node::id))
                .ifPresent(target -> powers.put(target.id(), powers.get(target.id()) * 2.0D));
        }
        return powers;
    }
}
