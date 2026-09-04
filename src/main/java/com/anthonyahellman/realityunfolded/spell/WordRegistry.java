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
import java.util.List;
import java.util.Map;

public final class WordRegistry {
    private static final Map<SpellWordId, SpellWord> WORDS = new EnumMap<>(SpellWordId.class);
    private static final Map<SpellWordId, SpellWordPresentation> PRESENTATIONS =
        new EnumMap<>(SpellWordId.class);

    static {
        register(SpellWordId.BOLT, new BoltWord(), "Bolt", "◆", "Manifestation",
            "Creates a directed bolt manifestation.");
        register(SpellWordId.BREAK, new BreakWord(), "Break", "▰", "World",
            "Breaks the targeted block.");
        register(SpellWordId.IGNITE, new IgniteWord(), "Ignite", "♨", "Effect",
            "Ignites the impacted entity or surface.");
        register(SpellWordId.IMPACT, new ImpactWord(), "Impact", "⊙", "Flow",
            "Defers following words until impact.");
        register(SpellWordId.EXPLOSION, new ExplosionWord(), "Explosion", "✦", "Effect",
            "Releases a damaging blast.");
        register(SpellWordId.AMPLIFY, new AmplifyWord(), "Amplify", "▲", "Modifier",
            "Doubles the preceding compatible word's power.");
        register(SpellWordId.SPLIT, new SplitWord(), "Split", "⑂", "Flow",
            "Duplicates every compatible manifestation: 1→2→4.");
        register(SpellWordId.LINK, new LinkWord(), "Link", "∞", "Flow",
            "Links compatible manifestations in this cast.");
        register(SpellWordId.HOME, new HomeWord(), "Home", "⌖", "Motion",
            "Steers compatible manifestations toward a target.");
    }

    private WordRegistry() {}

    public static SpellWord get(SpellWordId id) {
        SpellWord word = WORDS.get(id);
        if (word == null) throw new IllegalStateException("No implementation registered for " + id);
        return word;
    }

    public static SpellWordPresentation presentation(SpellWordId id) {
        SpellWordPresentation presentation = PRESENTATIONS.get(id);
        if (presentation == null) throw new IllegalStateException("No presentation registered for " + id);
        return presentation;
    }

    public static List<SpellWordPresentation> presentations() {
        return List.copyOf(PRESENTATIONS.values());
    }

    private static void register(SpellWordId id, SpellWord implementation, String displayName,
                                 String glyph, String category, String description) {
        WORDS.put(id, implementation);
        PRESENTATIONS.put(id,
            new SpellWordPresentation(id, displayName, glyph, category, description));
    }
}
