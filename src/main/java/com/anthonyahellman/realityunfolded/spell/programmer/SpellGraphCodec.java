package com.anthonyahellman.realityunfolded.spell.programmer;

import com.anthonyahellman.realityunfolded.spell.SpellPort;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellValidationException;
import com.anthonyahellman.realityunfolded.spell.SpellWordId;
import com.anthonyahellman.realityunfolded.spell.WordRegistry;

import java.util.HashSet;
import java.util.Set;

/** Compact deterministic player-data/network representation; not a player-facing language. */
public final class SpellGraphCodec {
    public static final int MAX_ENCODED_LENGTH = 16_384;
    private static final String VERSION = "RU_GRAPH_1";

    private SpellGraphCodec() {}

    public static SpellGraphDraft fromProgram(SpellProgram program) {
        SpellGraphDraft graph = new SpellGraphDraft();
        int index = 0;
        for (var node : program.nodes()) {
            graph.addLoaded(new SpellGraphDraft.Node(node.id(), node.word(), node.integerArgument(),
                index % 5 * 150, index / 5 * 82));
            index++;
        }
        graph.setRootId(program.rootNode());
        for (var node : program.nodes()) {
            var ports = WordRegistry.presentation(node.word()).outputs();
            for (int edge = 0; edge < node.next().size() && edge < ports.size(); edge++) {
                int target = node.next().get(edge);
                if (target != SpellProgram.TERMINAL) {
                    graph.addLoaded(new SpellGraphDraft.Edge(node.id(), target, ports.get(edge)));
                }
            }
        }
        return graph;
    }

    public static String encode(SpellGraphDraft graph) {
        StringBuilder value = new StringBuilder(VERSION).append('|').append(graph.rootId()).append('|');
        for (SpellGraphDraft.Node node : graph.nodes()) {
            value.append('N').append(',').append(node.id()).append(',').append(node.word().name())
                .append(',').append(node.integerArgument()).append(',').append(node.x()).append(',')
                .append(node.y()).append(';');
        }
        value.append('|');
        for (SpellGraphDraft.Edge edge : graph.edges()) {
            value.append('E').append(',').append(edge.from()).append(',').append(edge.to()).append(',')
                .append(edge.port().name()).append(';');
        }
        if (value.length() > MAX_ENCODED_LENGTH) throw new IllegalStateException("Spell graph is too large");
        return value.toString();
    }

    public static SpellGraphDraft decode(String encoded) throws SpellValidationException {
        if (encoded == null || encoded.isBlank()) return new SpellGraphDraft();
        if (encoded.length() > MAX_ENCODED_LENGTH) throw new SpellValidationException("Graph exceeds safety size");
        String[] sections = encoded.split("\\|", -1);
        if (sections.length != 4 || !VERSION.equals(sections[0])) {
            throw new SpellValidationException("Unsupported spell graph format");
        }
        SpellGraphDraft graph = new SpellGraphDraft();
        int root = integer(sections[1], "root");
        Set<Integer> ids = new HashSet<>();
        if (!sections[2].isBlank()) {
            for (String token : sections[2].split(";")) {
                if (token.isBlank()) continue;
                String[] fields = token.split(",", -1);
                if (fields.length != 6 || !"N".equals(fields[0])) malformed("node");
                int id = integer(fields[1], "node id");
                if (id < 0 || id > 4095 || !ids.add(id)) malformed("duplicate/invalid node id");
                SpellWordId word;
                try { word = SpellWordId.parse(fields[2]); }
                catch (IllegalArgumentException exception) { throw new SpellValidationException("Unknown graph word"); }
                int argument = integer(fields[3], "argument");
                int x = integer(fields[4], "x");
                int y = integer(fields[5], "y");
                if (Math.abs(x) > SpellGraphDraft.MAX_COORDINATE || Math.abs(y) > SpellGraphDraft.MAX_COORDINATE) {
                    throw new SpellValidationException("Graph coordinate exceeds safety bounds");
                }
                graph.addLoaded(new SpellGraphDraft.Node(id, word, argument, x, y));
            }
        }
        if (graph.nodes().size() > SpellGraphDraft.MAX_NODES) {
            throw new SpellValidationException("Graph exceeds node safety limit");
        }
        graph.setRootId(root);
        if (!sections[3].isBlank()) {
            for (String token : sections[3].split(";")) {
                if (token.isBlank()) continue;
                String[] fields = token.split(",", -1);
                if (fields.length != 4 || !"E".equals(fields[0])) malformed("edge");
                int from = integer(fields[1], "edge source");
                int to = integer(fields[2], "edge target");
                SpellPort port;
                try { port = SpellPort.valueOf(fields[3]); }
                catch (IllegalArgumentException exception) { throw new SpellValidationException("Unknown graph port"); }
                graph.addLoaded(new SpellGraphDraft.Edge(from, to, port));
            }
        }
        if (graph.edges().size() > SpellGraphDraft.MAX_EDGES) {
            throw new SpellValidationException("Graph exceeds edge safety limit");
        }
        return graph;
    }

    private static int integer(String value, String field) throws SpellValidationException {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw new SpellValidationException("Malformed graph " + field); }
    }

    private static void malformed(String detail) throws SpellValidationException {
        throw new SpellValidationException("Malformed graph " + detail);
    }
}
