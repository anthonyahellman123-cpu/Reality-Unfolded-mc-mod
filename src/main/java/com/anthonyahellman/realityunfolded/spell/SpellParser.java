package com.anthonyahellman.realityunfolded.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpellParser {
    private static final Pattern ARGUMENT = Pattern.compile("([A-Z_]+)\\(([-+]?\\d+)\\)");
    private static final int MAX_SPLIT_CHILDREN = 16;

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

            if (matcher.matches() && word != SpellWordId.SPLIT) {
                throw new SpellValidationException(word + " does not accept an integer argument");
            }
            if (!matcher.matches() && word == SpellWordId.SPLIT) {
                throw new SpellValidationException("SPLIT requires a child count, for example SPLIT(3)");
            }
            if (word == SpellWordId.SPLIT && (argument < 2 || argument > MAX_SPLIT_CHILDREN)) {
                throw new SpellValidationException("SPLIT child count must be from 2 to " + MAX_SPLIT_CHILDREN);
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
            List<Integer> next = i + 1 < nodes.size() ? List.of(i + 1) : List.of();
            immutable.add(new SpellNode(node.id, node.word, node.argument, node.powerMultiplier, next));
        }
        return new SpellProgram(0, immutable, source.trim());
    }

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
