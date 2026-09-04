package com.anthonyahellman.realityunfolded.spell.programmer;

import com.anthonyahellman.realityunfolded.spell.SpellWordId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VisualSpellDraftTest {
    @Test
    void visualGlyphsProduceCanonicalParserSource() throws Exception {
        VisualSpellDraft draft = new VisualSpellDraft();
        draft.append(SpellWordId.BOLT);
        draft.append(SpellWordId.HOME);
        draft.append(SpellWordId.IMPACT);
        draft.append(SpellWordId.IGNITE);

        assertEquals("BOLT HOME IMPACT IGNITE", draft.source());
        assertEquals(draft.source(), VisualSpellDraft.fromSource(draft.source()).source());
    }

    @Test
    void visualNodesCanBeRemovedAndReorderedWithoutChangingWordIdentity() {
        VisualSpellDraft draft = new VisualSpellDraft();
        draft.append(SpellWordId.BOLT);
        draft.append(SpellWordId.IGNITE);
        draft.append(SpellWordId.IMPACT);

        int moved = draft.move(2, -1);
        draft.remove(2);

        assertEquals(1, moved);
        assertEquals("BOLT IMPACT", draft.source());
    }

    @Test
    void repeatedVisualSplitUsesParameterlessDoublingSemantics() {
        VisualSpellDraft draft = new VisualSpellDraft();
        draft.append(SpellWordId.BOLT);
        draft.append(SpellWordId.SPLIT);
        draft.append(SpellWordId.SPLIT);

        assertEquals("BOLT SPLIT SPLIT", draft.source());
    }
}
