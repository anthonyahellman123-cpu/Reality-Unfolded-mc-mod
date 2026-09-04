package com.anthonyahellman.realityunfolded.spell;

/** Immutable entity-query description carried by one active cast branch. */
public record EntityQuery(Kind kind, boolean hostileOnly, boolean includeCaster) {
    public enum Kind {
        ENTITY,
        PLAYER
    }

    public static EntityQuery entities() {
        return new EntityQuery(Kind.ENTITY, false, false);
    }

    public static EntityQuery players() {
        return new EntityQuery(Kind.PLAYER, false, false);
    }

    public EntityQuery hostile() {
        return new EntityQuery(kind, true, includeCaster);
    }
}
