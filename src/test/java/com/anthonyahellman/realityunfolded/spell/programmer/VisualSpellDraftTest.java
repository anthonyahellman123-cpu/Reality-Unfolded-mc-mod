package com.anthonyahellman.realityunfolded.spell.programmer;

import com.anthonyahellman.realityunfolded.spell.SpellWordId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void registryDrivenDelayGlyphStartsWithAndAdjustsAValidDuration() throws Exception {
        VisualSpellDraft draft = new VisualSpellDraft();
        draft.append(SpellWordId.BOLT);
        draft.append(SpellWordId.DELAY);
        draft.append(SpellWordId.ACCELERATE);

        assertEquals("BOLT DELAY(20) ACCELERATE", draft.source());
        draft.adjustParameter(1, 1);
        assertEquals("BOLT DELAY(25) ACCELERATE", draft.source());
        draft.adjustParameter(1, -1);
        assertEquals("BOLT DELAY(20) ACCELERATE", draft.source());
        assertTrue(VisualSpellDraft.fromSource(draft.source()).nodes().size() == 3);
    }

    @Test
    void allNewWordsCanEnterTheVisualDraftWithoutRawTyping() {
        VisualSpellDraft draft = new VisualSpellDraft();
        for (SpellWordId word : SpellWordId.values()) draft.append(word);

        assertEquals(SpellWordId.values().length, draft.nodes().size());
        assertTrue(draft.source().contains("SELF"));
        assertTrue(draft.source().contains("DELAY(20)"));
        assertTrue(draft.source().contains("ANTI_GRAVITY"));
        assertTrue(draft.source().contains("ORB"));
    }
}
