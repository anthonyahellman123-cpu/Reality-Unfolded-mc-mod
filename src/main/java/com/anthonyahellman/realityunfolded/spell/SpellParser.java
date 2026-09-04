package com.anthonyahellman.realityunfolded.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpellParser {
    private static final Pattern ARGUMENT = Pattern.compile("([A-Z_]+)\\(([-+]?\\d+)\\)");
    private static final int MAX_SPLIT_CHILDREN = 16;
    private static final int DEFAULT_SPLIT_CHILDREN = 2;
    private static final int MAX_DELAY_TICKS = 200;

    private SpellParser() {}

    public static SpellProgram parse(String source) throws SpellValidationException {
        String normalized = source.replace("→", " ").replace("->", " ").trim();
        if (normalized.isEmpty()) throw new SpellValidationException("Spell contains no words");

        String[] tokens = normalized.split("\\s+");
        List<MutableNode> nodes = new ArrayList<>();
        MutableNode lastPowerTarget = null;

        for (String raw : tokens) {
            String token = raw.toUpperCase(Locale.ROOT);
            Matcher matcher = ARGUMENT.matcher(token);
            String wordText = matcher.matches() ? matcher.group(1) : token;
            int argument = matcher.matches() ? parseInteger(matcher.group(2), token) : 0;
            SpellWordId word;
            try {
                word = SpellWordId.parse(wordText);
            } catch (IllegalArgumentException exception) {
                throw new SpellValidationException("Unknown word: " + raw);
            }

            if (matcher.matches() && word != SpellWordId.SPLIT && word != SpellWordId.DELAY) {
                throw new SpellValidationException(word + " does not accept an integer argument");
            }
            if (!matcher.matches() && word == SpellWordId.SPLIT) argument = DEFAULT_SPLIT_CHILDREN;
            if (!matcher.matches() && word == SpellWordId.DELAY) {
                throw new SpellValidationException("DELAY requires ticks, for example DELAY(20)");
            }
            if (word == SpellWordId.SPLIT && (argument < 2 || argument > MAX_SPLIT_CHILDREN)) {
                throw new SpellValidationException("SPLIT child count must be from 2 to " + MAX_SPLIT_CHILDREN);
            }
            if (word == SpellWordId.DELAY && (argument < 1 || argument > MAX_DELAY_TICKS)) {
                throw new SpellValidationException("DELAY must be from 1 to " + MAX_DELAY_TICKS + " ticks");
            }

            MutableNode node = new MutableNode(nodes.size(), word, argument);
            nodes.add(node);
            if (word == SpellWordId.AMPLIFY) {
                if (lastPowerTarget == null) {
                    throw new SpellValidationException("AMPLIFY must follow a power-bearing operation");
                }
                lastPowerTarget.powerMultiplier *= 2.0D;
            } else {
                lastPowerTarget = word.acceptsPower() ? node : null;
            }
        }

        List<SpellNode> immutable = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            MutableNode node = nodes.get(i);
            List<Integer> next;
            if (node.word == SpellWordId.IF) {
                next = List.of(i + 1 < nodes.size() ? i + 1 : SpellProgram.TERMINAL,
                    SpellProgram.TERMINAL);
            } else {
                next = i + 1 < nodes.size() ? List.of(i + 1) : List.of();
            }
            immutable.add(new SpellNode(node.id, node.word, node.argument, node.powerMultiplier, next));
        }
        SpellProgram program = new SpellProgram(0, immutable, source.trim());
        validateRuntimeRequirements(program);
        int manifestations = SpellProgramAnalysis.estimatedManifestations(program);
        if (manifestations > SpellProgramAnalysis.MAX_MANIFESTATIONS) {
            throw new SpellValidationException("Spell may create more than "
                + SpellProgramAnalysis.MAX_MANIFESTATIONS + " manifestations");
        }
        return new SpellProgram(0, immutable, SpellProgramAnalysis.canonicalSource(program));
    }

    private static void validateRuntimeRequirements(SpellProgram program) throws SpellValidationException {
        ManifestationKind manifestation = ManifestationKind.NONE;
        EntityQuery.Kind queryKind = null;
        boolean sensing = false;
        boolean hasCondition = false;
        boolean hasDelayedBoundary = false;
        for (SpellNode node : program.nodes()) {
            SpellWordId word = node.word();
            if (word == SpellWordId.SENSE) {
                sensing = true;
                hasCondition = false;
            } else if (word == SpellWordId.SELF || word == SpellWordId.TOUCH) {
                hasCondition = true;
            } else if (word == SpellWordId.ENTITY) {
                queryKind = EntityQuery.Kind.ENTITY;
                hasCondition = sensing;
            } else if (word == SpellWordId.PLAYER) {
                queryKind = EntityQuery.Kind.PLAYER;
                hasCondition = sensing;
            } else if (word == SpellWordId.HOSTILE) {
                if (queryKind != EntityQuery.Kind.ENTITY) {
                    throw new SpellValidationException("HOSTILE requires ENTITY context");
                }
                hasCondition = hasCondition || sensing;
            } else if (word == SpellWordId.NEAREST) {
                if (queryKind == null) throw new SpellValidationException("NEAREST requires ENTITY or PLAYER context");
                hasCondition = true;
            } else if (word == SpellWordId.NOT) {
                if (!hasCondition) throw new SpellValidationException("NOT requires a condition result");
            } else if (word == SpellWordId.IF) {
                if (!hasCondition) throw new SpellValidationException("IF requires a condition");
            } else if (word == SpellWordId.BOLT) {
                manifestation = ManifestationKind.BOLT;
            } else if (word == SpellWordId.ORB) {
                manifestation = ManifestationKind.ORB;
            } else if (word == SpellWordId.IMPACT) {
                if (manifestation == ManifestationKind.NONE) {
                    throw new SpellValidationException("IMPACT requires a compatible manifestation");
                }
                if (manifestation == ManifestationKind.ORB) {
                    throw new SpellValidationException("IMPACT is not compatible with persistent ORB");
                }
            } else if (requiresManifestation(word) && manifestation == ManifestationKind.NONE) {
                throw new SpellValidationException(word + " requires a compatible manifestation");
            }
            if (word == SpellWordId.DELAY) hasDelayedBoundary = true;
            if (word == SpellWordId.RELEASE) {
                if (!hasDelayedBoundary) throw new SpellValidationException("RELEASE requires a preceding DELAY");
                hasDelayedBoundary = false;
            }
        }
    }

    private static boolean requiresManifestation(SpellWordId word) {
        return word == SpellWordId.HOME || word == SpellWordId.SPLIT || word == SpellWordId.LINK
            || word == SpellWordId.ACCELERATE || word == SpellWordId.GRAVITY
            || word == SpellWordId.ANTI_GRAVITY;
    }

    private enum ManifestationKind { NONE, BOLT, ORB }

    private static int parseInteger(String value, String token) throws SpellValidationException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new SpellValidationException("Invalid integer argument in " + token);
        }
    }

    private static final class MutableNode {
        private final int id;
        private final SpellWordId word;
        private final int argument;
        private double powerMultiplier = 1.0D;

        private MutableNode(int id, SpellWordId word, int argument) {
            this.id = id;
            this.word = word;
            this.argument = argument;
        }
    }
}
