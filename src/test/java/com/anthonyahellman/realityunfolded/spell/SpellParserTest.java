package com.anthonyahellman.realityunfolded.spell;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellParserTest {
    @Test
    void preservesWordOrderAndImpactContinuation() throws Exception {
        SpellProgram program = SpellParser.parse("BOLT -> IMPACT -> IGNITE");
        assertEquals(List.of(SpellWordId.BOLT, SpellWordId.IMPACT, SpellWordId.IGNITE),
            program.nodes().stream().map(SpellNode::word).toList());
        assertEquals(List.of(1), program.node(0).next());
        assertEquals(List.of(2), program.node(1).next());
    }

    @Test
    void postfixAmplifyChangesOnlyTheCompatibleOperation() throws Exception {
        SpellProgram program = SpellParser.parse("BOLT IMPACT EXPLOSION AMPLIFY");
        assertEquals(1.0D, program.node(0).powerMultiplier());
        assertEquals(2.0D, program.node(2).powerMultiplier());
        assertEquals(SpellWordId.AMPLIFY, program.node(3).word());
    }

    @Test
    void legacySplitRetainsConfigurableChildCount() throws Exception {
        SpellProgram program = SpellParser.parse("BOLT SPLIT(3) IMPACT IGNITE");
        assertEquals(3, program.node(1).integerArgument());
    }

    @Test
    void parameterlessSplitDuplicatesAndRepeatedSplitEstimatesFour() throws Exception {
        SpellProgram program = SpellParser.parse("BOLT SPLIT SPLIT HOME IMPACT IGNITE");
        assertEquals(2, program.node(1).integerArgument());
        assertEquals(2, program.node(2).integerArgument());
        assertEquals(4, SpellProgramAnalysis.estimatedManifestations(program));
        assertEquals("BOLT SPLIT SPLIT HOME IMPACT IGNITE", program.source());
    }

    @Test
    void rejectsInvalidModifiersAndSplitCounts() {
        assertThrows(SpellValidationException.class, () -> SpellParser.parse("AMPLIFY BOLT"));
        assertThrows(SpellValidationException.class, () -> SpellParser.parse("BOLT SPLIT(1)"));
        assertThrows(SpellValidationException.class, () -> SpellParser.parse("SPLIT BOLT"));
        assertThrows(SpellValidationException.class,
            () -> SpellParser.parse("BOLT SPLIT SPLIT SPLIT SPLIT SPLIT SPLIT SPLIT"));
    }

    @Test
    void graphRepresentationAllowsParallelChildren() {
        SpellProgram program = new SpellProgram(0, List.of(
            new SpellNode(0, SpellWordId.BOLT, 0, 1.0D, List.of(1, 2)),
            new SpellNode(1, SpellWordId.IGNITE, 0, 1.0D, List.of()),
            new SpellNode(2, SpellWordId.EXPLOSION, 0, 1.0D, List.of())
        ), "test graph");
        assertEquals(List.of(1, 2), program.node(0).next());
    }

    @Test
    void programRoundTripsThroughManifestationNbt() throws Exception {
        SpellProgram original = SpellParser.parse("BOLT HOME SPLIT LINK IMPACT IGNITE");
        SpellProgram restored = SpellProgram.load(original.save());
        assertEquals(original.source(), restored.source());
        assertEquals(original.nodes(), restored.nodes());
    }

    @Test
    void stageTwoRegressionProgramsCompileThroughTheSameParser() throws Exception {
        List<String> programs = List.of(
            "BOLT IMPACT IGNITE",
            "BOLT HOME IMPACT IGNITE",
            "BOLT IMPACT EXPLOSION",
            "BOLT IMPACT EXPLOSION AMPLIFY",
            "BOLT SPLIT IMPACT IGNITE",
            "BOLT SPLIT SPLIT HOME IMPACT IGNITE",
            "BREAK",
            "BOLT LINK IMPACT IGNITE"
        );

        for (String source : programs) {
            assertTrue(SpellParser.parse(source).nodes().size() > 0, source);
        }
    }

    @Test
    void registryProvidesPresentationForEveryRuntimeWord() {
        assertEquals(List.of(SpellWordId.values()),
            WordRegistry.presentations().stream().map(SpellWordPresentation::id).toList());
        for (SpellWordId id : SpellWordId.values()) {
            assertEquals(id, WordRegistry.presentation(id).id());
            WordRegistry.get(id);
        }
    }
}
