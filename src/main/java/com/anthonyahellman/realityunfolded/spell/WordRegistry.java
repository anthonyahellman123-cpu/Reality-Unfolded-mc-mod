package com.anthonyahellman.realityunfolded.spell;

import com.anthonyahellman.realityunfolded.spell.word.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WordRegistry {
    private static final Map<SpellWordId, SpellWord> WORDS = new EnumMap<>(SpellWordId.class);
    private static final Map<SpellWordId, SpellWordPresentation> PRESENTATIONS =
        new EnumMap<>(SpellWordId.class);

    static {
        register(SpellWordId.BOLT, new BoltWord(), "Bolt", "◆", "MANIFESTATION",
            "Creates a directed bolt manifestation.");
        register(SpellWordId.BREAK, new BreakWord(), "Break", "▰", "WORLD",
            "Breaks the targeted block.");
        register(SpellWordId.IGNITE, new IgniteWord(), "Ignite", "♨", "EFFECT",
            "Ignites the impacted entity or surface.");
        register(SpellWordId.IMPACT, new ImpactWord(), "Impact", "⊙", "FLOW",
            "Defers following words until impact.");
        register(SpellWordId.EXPLOSION, new ExplosionWord(), "Explosion", "✦", "EFFECT",
            "Releases a damaging blast.");
        register(SpellWordId.AMPLIFY, new AmplifyWord(), "Amplify", "▲", "MODIFIER",
            "Doubles the preceding compatible word's power.");
        register(SpellWordId.SPLIT, new SplitWord(), "Split", "⑂", "FLOW",
            "Duplicates every compatible manifestation: 1→2→4.");
        register(SpellWordId.LINK, new LinkWord(), "Link", "∞", "FLOW",
            "Links compatible manifestations in this cast.");
        register(SpellWordId.HOME, new HomeWord(), "Home", "⌖", "MOTION",
            "Steers compatible manifestations toward a target.");
        register(SpellWordId.SELF, new SelfWord(), "Self", "◎", "CONTEXT",
            "Resolves the caster as explicit target context.");
        register(SpellWordId.ENTITY, new EntityWord(), "Entity", "◇", "CONTEXT",
            "Begins a living-entity query excluding the caster.");
        register(SpellWordId.PLAYER, new PlayerWord(), "Player", "♙", "CONTEXT",
            "Begins an other-player query; SELF remains distinct.");
        register(SpellWordId.NEAREST, new NearestWord(), "Nearest", "⌁", "CONTEXT",
            "Resolves the nearest candidate matching current filters.");
        register(SpellWordId.HOSTILE, new HostileWord(), "Hostile", "☠", "CONTEXT",
            "Restricts an entity query to hostile mobs.");
        register(SpellWordId.TOUCH, new TouchWord(), "Touch", "✋", "CONTEXT",
            "Resolves only what the caster can physically reach.");
        register(SpellWordId.SENSE, new SenseWord(), "Sense", "◉", "LOGIC",
            "Begins an immediate world-state condition query.");
        register(SpellWordId.IF, new IfWord(), "If", "?", "LOGIC",
            "Runs its TRUE continuation only when the condition is true.");
        register(SpellWordId.NOT, new NotWord(), "Not", "¬", "LOGIC",
            "Negates the current boolean condition.");
        register(SpellWordId.DELAY, new DelayWord(), "Delay", "◷", "FLOW",
            "Suspends this branch and resumes it after server ticks.",
            new SpellWordPresentation.ParameterSpec(1, 200, 20, 5, "ticks"));
        register(SpellWordId.RELEASE, new ReleaseWord(), "Release", "▷", "FLOW",
            "Consumes an available delayed-release boundary.");
        register(SpellWordId.ACCELERATE, new AccelerateWord(), "Accelerate", "»", "MOTION",
            "Multiplies current manifestation speed; repeats compose.");
        register(SpellWordId.GRAVITY, new GravityWord(), "Gravity", "↓", "MOTION",
            "Applies continuous downward influence.");
        register(SpellWordId.ANTI_GRAVITY, new AntiGravityWord(), "Anti Gravity", "↑", "MOTION",
            "Applies continuous upward influence; it is not FLOAT.");
        register(SpellWordId.ORB, new OrbWord(), "Orb", "●", "MANIFESTATION",
            "Creates a persistent non-impacting magical manifestation.");
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
            new SpellWordPresentation(id, displayName, glyph, category, description, null));
    }

    private static void register(SpellWordId id, SpellWord implementation, String displayName,
                                 String glyph, String category, String description,
                                 SpellWordPresentation.ParameterSpec parameter) {
        WORDS.put(id, implementation);
        PRESENTATIONS.put(id,
            new SpellWordPresentation(id, displayName, glyph, category, description, parameter));
    }
}
