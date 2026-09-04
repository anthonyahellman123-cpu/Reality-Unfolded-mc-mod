package com.anthonyahellman.realityunfolded.spell;

/** Client-safe metadata used to present a registered word without UI-specific conditionals. */
public record SpellWordPresentation(
    SpellWordId id,
    String displayName,
    String glyph,
    String category,
    String description,
    ParameterSpec parameter
) {
    public boolean hasPlayerParameter() {
        return parameter != null;
    }

    public record ParameterSpec(int minimum, int maximum, int defaultValue, int step, String unit) {}
}
