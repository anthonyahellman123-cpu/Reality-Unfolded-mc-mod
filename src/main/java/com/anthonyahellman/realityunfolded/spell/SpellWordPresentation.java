package com.anthonyahellman.realityunfolded.spell;

/** Client-safe metadata used to present a registered word without UI-specific conditionals. */
public record SpellWordPresentation(
    SpellWordId id,
    String displayName,
    String glyph,
    String category,
    String description
) {}
