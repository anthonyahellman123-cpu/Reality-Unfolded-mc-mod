package com.anthonyahellman.realityunfolded.spell.programmer;

import com.anthonyahellman.realityunfolded.spell.SpellPort;
import com.anthonyahellman.realityunfolded.spell.SpellWordId;
import com.anthonyahellman.realityunfolded.spell.WordRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Editable positioned graph shared by the client canvas and server safety compiler. */
public final class SpellGraphDraft {
    public static final int MAX_NODES = 64;
    public static final int MAX_EDGES = 128;
    public static final int MAX_COORDINATE = 8192;

    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private int rootId = -1;
    private int nextId;

    public List<Node> nodes() { return List.copyOf(nodes); }
    public List<Edge> edges() { return List.copyOf(edges); }
    public int rootId() { return rootId; }

    public Node add(SpellWordId word, int x, int y) {
        if (nodes.size() >= MAX_NODES) return null;
        int argument = word == SpellWordId.SPLIT ? 2 : 0;
        var parameter = WordRegistry.presentation(word).parameter();
        if (parameter != null) argument = parameter.defaultValue();
        Node node = new Node(nextId++, word, argument, clampCoordinate(x), clampCoordinate(y));
        nodes.add(node);
        if (rootId < 0) rootId = node.id();
        return node;
    }

    public void addLoaded(Node node) {
        nodes.add(node);
        nextId = Math.max(nextId, node.id() + 1);
    }

    public void addLoaded(Edge edge) { edges.add(edge); }
    public void setRootId(int value) { rootId = value; }

    public void move(int id, int x, int y) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node.id() == id) {
                nodes.set(i, new Node(id, node.word(), node.integerArgument(),
                    clampCoordinate(x), clampCoordinate(y)));
                return;
            }
        }
    }

    public void adjustParameter(int id, int direction) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node.id() != id) continue;
            var parameter = WordRegistry.presentation(node.word()).parameter();
            if (parameter == null) return;
            int argument = Math.max(parameter.minimum(), Math.min(parameter.maximum(),
                node.integerArgument() + Integer.compare(direction, 0) * parameter.step()));
            nodes.set(i, new Node(id, node.word(), argument, node.x(), node.y()));
            return;
        }
    }

    public boolean connect(int from, SpellPort port, int to) {
        if (from == to || node(from) == null || node(to) == null) return false;
        if (!WordRegistry.presentation(node(from).word()).outputs().contains(port)) return false;
        edges.removeIf(edge -> edge.from() == from && edge.port() == port);
        if (edges.size() >= MAX_EDGES) return false;
        Edge edge = new Edge(from, to, port);
        if (!edges.contains(edge)) edges.add(edge);
        return true;
    }

    public void disconnect(int from, SpellPort port) {
        edges.removeIf(edge -> edge.from() == from && edge.port() == port);
    }

    public void remove(int id) {
        nodes.removeIf(node -> node.id() == id);
        edges.removeIf(edge -> edge.from() == id || edge.to() == id);
        if (rootId == id) rootId = nodes.stream().min(Comparator.comparingInt(Node::id))
            .map(Node::id).orElse(-1);
    }

    public Node node(int id) {
        return nodes.stream().filter(node -> node.id() == id).findFirst().orElse(null);
    }

    public void clear() {
        nodes.clear();
        edges.clear();
        rootId = -1;
        nextId = 0;
    }

    private static int clampCoordinate(int value) {
        return Math.max(-MAX_COORDINATE, Math.min(MAX_COORDINATE, value));
    }

    public record Node(int id, SpellWordId word, int integerArgument, int x, int y) {}
    public record Edge(int from, int to, SpellPort port) {}
}
