package com.anthonyahellman.realityunfolded.spell;

import com.anthonyahellman.realityunfolded.spell.word.*;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        registerBranch(SpellWordId.IF, new IfWord(), "If", "?", "LOGIC",
            "Runs one of two explicit condition branches.");
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
            presentation(id, displayName, glyph, category, description, null, List.of(SpellPort.NEXT)));
    }

    private static void register(SpellWordId id, SpellWord implementation, String displayName,
                                 String glyph, String category, String description,
                                 SpellWordPresentation.ParameterSpec parameter) {
        WORDS.put(id, implementation);
        PRESENTATIONS.put(id,
            presentation(id, displayName, glyph, category, description, parameter, List.of(SpellPort.NEXT)));
    }

    private static void registerBranch(SpellWordId id, SpellWord implementation, String displayName,
                                       String glyph, String category, String description) {
        WORDS.put(id, implementation);
        PRESENTATIONS.put(id, presentation(id, displayName, glyph, category, description, null,
            List.of(SpellPort.TRUE, SpellPort.FALSE)));
    }

    private static SpellWordPresentation presentation(SpellWordId id, String displayName, String glyph,
                                                       String category, String description,
                                                       SpellWordPresentation.ParameterSpec parameter,
                                                       List<SpellPort> outputs) {
        return new SpellWordPresentation(id, displayName, glyph, category, subcategory(category),
            description, outputs, forces(category), manaCost(category), parameter);
    }

    private static String subcategory(String category) {
        return switch (category) {
            case "MANIFESTATION" -> "Forms";
            case "CONTEXT" -> "Selectors";
            case "LOGIC" -> "Conditions";
            case "FLOW" -> "Events & Time";
            case "MOTION" -> "Physics";
            case "EFFECT" -> "Entity & Area Effects";
            case "MODIFIER" -> "Power";
            case "WORLD" -> "World Interaction";
            default -> "General";
        };
    }

    private static Set<SpellForce> forces(String category) {
        return switch (category) {
            case "EFFECT", "MANIFESTATION", "MOTION" -> EnumSet.of(SpellForce.COMBAT);
            case "WORLD" -> EnumSet.of(SpellForce.MINING, SpellForce.UTILITY);
            case "CONTEXT", "LOGIC", "FLOW" -> EnumSet.of(SpellForce.UTILITY);
            case "MODIFIER" -> EnumSet.of(SpellForce.COMBAT, SpellForce.UTILITY);
            default -> EnumSet.of(SpellForce.UTILITY);
        };
    }

    private static int manaCost(String category) {
        return switch (category) {
            case "MANIFESTATION" -> 8;
            case "EFFECT", "WORLD" -> 6;
            case "MOTION", "MODIFIER" -> 3;
            case "FLOW" -> 2;
            default -> 1;
        };
    }
}
