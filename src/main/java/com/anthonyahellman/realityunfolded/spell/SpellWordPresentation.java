package com.anthonyahellman.realityunfolded.spell;

import java.util.List;
import java.util.Set;

/** Client-safe metadata used to present a registered word without UI-specific conditionals. */
public record SpellWordPresentation(
    SpellWordId id,
    String displayName,
    String glyph,
    String category,
    String subcategory,
    String description,
    List<SpellPort> outputs,
    Set<SpellForce> forces,
    int manaCost,
    ParameterSpec parameter
) {
    public SpellWordPresentation {
        outputs = List.copyOf(outputs);
        forces = Set.copyOf(forces);
    }

    public boolean hasPlayerParameter() {
        return parameter != null;
    }

    public record ParameterSpec(int minimum, int maximum, int defaultValue, int step, String unit) {}
}
