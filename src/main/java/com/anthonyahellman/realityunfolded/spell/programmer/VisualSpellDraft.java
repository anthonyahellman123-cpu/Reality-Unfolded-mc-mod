package com.anthonyahellman.realityunfolded.spell.programmer;

import com.anthonyahellman.realityunfolded.spell.SpellNode;
import com.anthonyahellman.realityunfolded.spell.SpellParser;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellValidationException;
import com.anthonyahellman.realityunfolded.spell.SpellWordId;
import com.anthonyahellman.realityunfolded.spell.SpellWordPresentation;
import com.anthonyahellman.realityunfolded.spell.WordRegistry;

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
        if (nodes.size() >= MAX_GLYPHS) return;
        SpellWordPresentation presentation = WordRegistry.presentation(word);
        int argument = presentation.parameter() == null ? (word == SpellWordId.SPLIT ? 2 : 0)
            : presentation.parameter().defaultValue();
        nodes.add(new GlyphNode(word, argument));
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

    public void adjustParameter(int index, int direction) {
        if (index < 0 || index >= nodes.size()) return;
        GlyphNode node = nodes.get(index);
        SpellWordPresentation.ParameterSpec spec = WordRegistry.presentation(node.word()).parameter();
        if (spec == null) return;
        int next = Math.max(spec.minimum(), Math.min(spec.maximum(),
            node.integerArgument() + Integer.compare(direction, 0) * spec.step()));
        nodes.set(index, new GlyphNode(node.word(), next));
    }

    public String source() {
        return nodes.stream().map(GlyphNode::sourceToken)
            .reduce((left, right) -> left + " " + right).orElse("");
    }

    public record GlyphNode(SpellWordId word, int integerArgument) {
        public String sourceToken() {
            if (word == SpellWordId.SPLIT && integerArgument == 2) return "SPLIT";
            return integerArgument > 0 ? word.name() + "(" + integerArgument + ")" : word.name();
        }
    }
}
