package com.anthonyahellman.realityunfolded.spell.programmer;

import com.anthonyahellman.realityunfolded.spell.SpellForce;
import com.anthonyahellman.realityunfolded.spell.WordRegistry;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class SpellGraphAnalysis {
    private SpellGraphAnalysis() {}

    public static Summary summarize(SpellGraphDraft graph) {
        Set<Integer> connected = SpellGraphCompiler.connectedNodeIds(graph);
        EnumSet<SpellForce> forces = EnumSet.noneOf(SpellForce.class);
        int mana = 0;
        for (SpellGraphDraft.Node node : graph.nodes()) {
            if (!connected.contains(node.id())) continue;
            var presentation = WordRegistry.presentation(node.word());
            forces.addAll(presentation.forces());
            mana += presentation.manaCost();
        }
        String force = forces.isEmpty() ? "Unresolved" : forces.stream()
            .map(value -> value.name().charAt(0) + value.name().substring(1).toLowerCase())
            .collect(Collectors.joining(" / "));
        return new Summary(connected.size(), graph.nodes().size() - connected.size(), force, mana);
    }

    public record Summary(int connectedNodes, int disconnectedNodes, String force, int informationalManaCost) {}
}
