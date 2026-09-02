package com.anthonyahellman.realityunfolded.spell;

import java.util.Locale;

public enum SpellWordId {
    BOLT(true),
    BREAK(false),
    IGNITE(true),
    IMPACT(false),
    EXPLOSION(true),
    AMPLIFY(false),
    SPLIT(false),
    LINK(false),
    HOME(false);

    private final boolean acceptsPower;

    SpellWordId(boolean acceptsPower) {
        this.acceptsPower = acceptsPower;
    }

    public boolean acceptsPower() {
        return acceptsPower;
    }

    public static SpellWordId parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
