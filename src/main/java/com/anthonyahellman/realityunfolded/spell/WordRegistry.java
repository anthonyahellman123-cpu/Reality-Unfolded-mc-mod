package com.anthonyahellman.realityunfolded.spell;

import com.anthonyahellman.realityunfolded.spell.word.AmplifyWord;
import com.anthonyahellman.realityunfolded.spell.word.BoltWord;
import com.anthonyahellman.realityunfolded.spell.word.BreakWord;
import com.anthonyahellman.realityunfolded.spell.word.ExplosionWord;
import com.anthonyahellman.realityunfolded.spell.word.HomeWord;
import com.anthonyahellman.realityunfolded.spell.word.IgniteWord;
import com.anthonyahellman.realityunfolded.spell.word.ImpactWord;
import com.anthonyahellman.realityunfolded.spell.word.LinkWord;
import com.anthonyahellman.realityunfolded.spell.word.SplitWord;

import java.util.EnumMap;
import java.util.Map;

public final class WordRegistry {
    private static final Map<SpellWordId, SpellWord> WORDS = new EnumMap<>(SpellWordId.class);

    static {
        WORDS.put(SpellWordId.BOLT, new BoltWord());
        WORDS.put(SpellWordId.BREAK, new BreakWord());
        WORDS.put(SpellWordId.IGNITE, new IgniteWord());
        WORDS.put(SpellWordId.IMPACT, new ImpactWord());
        WORDS.put(SpellWordId.EXPLOSION, new ExplosionWord());
        WORDS.put(SpellWordId.AMPLIFY, new AmplifyWord());
        WORDS.put(SpellWordId.SPLIT, new SplitWord());
        WORDS.put(SpellWordId.LINK, new LinkWord());
        WORDS.put(SpellWordId.HOME, new HomeWord());
    }

    private WordRegistry() {}

    public static SpellWord get(SpellWordId id) {
        SpellWord word = WORDS.get(id);
        if (word == null) throw new IllegalStateException("No implementation registered for " + id);
        return word;
    }
}
