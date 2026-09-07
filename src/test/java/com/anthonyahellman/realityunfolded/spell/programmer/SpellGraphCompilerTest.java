package com.anthonyahellman.realityunfolded.spell.programmer;

import com.anthonyahellman.realityunfolded.spell.SpellPort;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellValidationException;
import com.anthonyahellman.realityunfolded.spell.SpellWordId;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellGraphCompilerTest {
    @Test
    void codecPreservesPositionsRootArgumentsAndBranchPorts() throws Exception {
        SpellGraphDraft graph = branchGraph();

        SpellGraphDraft restored = SpellGraphCodec.decode(SpellGraphCodec.encode(graph));

        assertEquals(graph.nodes(), restored.nodes());
        assertEquals(graph.edges(), restored.edges());
        assertEquals(graph.rootId(), restored.rootId());
    }

    @Test
    void trueAndFalseBranchesCompileAsDistinctRuntimeEdges() throws Exception {
        SpellProgram program = SpellGraphCompiler.compile(branchGraph());

        assertEquals(0, program.rootNode());
        assertEquals(java.util.List.of(1, 2), program.node(0).next());
        assertEquals(SpellWordId.BOLT, program.node(1).word());
        assertEquals(SpellWordId.BREAK, program.node(2).word());
    }

    @Test
    void absentFalseBranchCompilesToExplicitTerminal() throws Exception {
        SpellGraphDraft graph = new SpellGraphDraft();
        int condition = graph.add(SpellWordId.IF, 0, 0).id();
        int bolt = graph.add(SpellWordId.BOLT, 140, -40).id();
        graph.connect(condition, SpellPort.TRUE, bolt);

        SpellProgram program = SpellGraphCompiler.compile(graph);

        assertEquals(java.util.List.of(bolt, SpellProgram.TERMINAL), program.node(condition).next());
    }

    @Test
    void semanticNonsenseCompilesButMalformedStructureDoesNot() throws Exception {
        SpellGraphDraft graph = new SpellGraphDraft();
        graph.add(SpellWordId.ACCELERATE, 20, 30);
        assertEquals(SpellWordId.ACCELERATE, SpellGraphCompiler.compile(graph).node(0).word());

        SpellGraphDraft malformed = new SpellGraphDraft();
        malformed.addLoaded(new SpellGraphDraft.Node(0, SpellWordId.BOLT, 0, 0, 0));
        malformed.setRootId(0);
        malformed.addLoaded(new SpellGraphDraft.Edge(0, 99, SpellPort.NEXT));
        assertThrows(SpellValidationException.class, () -> SpellGraphCompiler.compile(malformed));
    }

    @Test
    void disconnectedNodesPersistButDoNotAffectDescriptiveAnalysis() throws Exception {
        SpellGraphDraft graph = new SpellGraphDraft();
        int root = graph.add(SpellWordId.BOLT, 0, 0).id();
        graph.add(SpellWordId.BREAK, 400, 400);

        SpellGraphAnalysis.Summary analysis = SpellGraphAnalysis.summarize(graph);

        assertEquals(Set.of(root), SpellGraphCompiler.connectedNodeIds(graph));
        assertEquals(1, analysis.connectedNodes());
        assertEquals(1, analysis.disconnectedNodes());
        assertEquals("Combat", analysis.force());
    }

    private static SpellGraphDraft branchGraph() {
        SpellGraphDraft graph = new SpellGraphDraft();
        int branch = graph.add(SpellWordId.IF, 0, 0).id();
        int truth = graph.add(SpellWordId.BOLT, 160, -70).id();
        int lie = graph.add(SpellWordId.BREAK, 160, 70).id();
        graph.connect(branch, SpellPort.TRUE, truth);
        graph.connect(branch, SpellPort.FALSE, lie);
        return graph;
    }
}
