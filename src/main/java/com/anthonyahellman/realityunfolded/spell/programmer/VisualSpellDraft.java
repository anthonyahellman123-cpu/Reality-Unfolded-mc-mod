package com.anthonyahellman.realityunfolded.spell.programmer;

import com.anthonyahellman.realityunfolded.spell.SpellNode;
import com.anthonyahellman.realityunfolded.spell.SpellParser;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellValidationException;
import com.anthonyahellman.realityunfolded.spell.SpellWordId;

import java.util.ArrayList;
import java.util.List;

/**
 * Editable first-pass graph projection. The current screen edits one ordered path, while the
 * parser-produced SpellProgram remains the backend truth and retains its graph structure.
 */
public final class VisualSpellDraft {
    public static final int MAX_GLYPHS = 32;
    private final List<GlyphNode> nodes = new ArrayList<>();

    public static VisualSpellDraft fromSource(String source) throws SpellValidationException {
        VisualSpellDraft draft = new VisualSpellDraft();
        if (source == null || source.isBlank()) return draft;
        SpellProgram program = SpellParser.parse(source);
        for (SpellNode node : program.nodes()) {
            draft.nodes.add(new GlyphNode(node.word(), node.integerArgument()));
        }
        return draft;
    }

    public List<GlyphNode> nodes() {
        return List.copyOf(nodes);
    }

    public void append(SpellWordId word) {
        if (nodes.size() < MAX_GLYPHS) nodes.add(new GlyphNode(word, word == SpellWordId.SPLIT ? 2 : 0));
    }

    public void remove(int index) {
        if (index >= 0 && index < nodes.size()) nodes.remove(index);
    }

    public int move(int index, int offset) {
        int target = index + offset;
        if (index < 0 || index >= nodes.size() || target < 0 || target >= nodes.size()) return index;
        GlyphNode moving = nodes.remove(index);
        nodes.add(target, moving);
        return target;
    }

    public String source() {
        return nodes.stream().map(GlyphNode::sourceToken)
            .reduce((left, right) -> left + " " + right).orElse("");
    }

    public record GlyphNode(SpellWordId word, int integerArgument) {
        public String sourceToken() {
            return word == SpellWordId.SPLIT && integerArgument != 2
                ? "SPLIT(" + integerArgument + ")" : word.name();
        }
    }
}
