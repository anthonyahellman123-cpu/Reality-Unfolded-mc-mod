package com.anthonyahellman.realityunfolded.grimoire;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.spell.SpellCastService;
import com.anthonyahellman.realityunfolded.spell.SpellDebug;
import com.anthonyahellman.realityunfolded.spell.SpellParser;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellProgramAnalysis;
import com.anthonyahellman.realityunfolded.spell.SpellValidationException;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative operations for the player-owned Grimoire spell library. */
public final class GrimoireSpellService {
    private GrimoireSpellService() {}

    public static Result validate(String source) {
        String safeSource = source == null ? "" : source;
        try {
            SpellProgram program = SpellParser.parse(safeSource);
            int manifestations = SpellProgramAnalysis.estimatedManifestations(program);
            return Result.success("VALID — " + program.nodes().size() + " glyphs compiled.", manifestations);
        } catch (SpellValidationException exception) {
            SpellDebug.validation(safeSource, exception.getMessage());
            return Result.failure("INVALID: " + exception.getMessage());
        }
    }

    public static Result save(GrimoireData data, int slot, String name, String source, boolean select) {
        if (!GrimoireData.isValidSlot(slot)) {
            return Result.failure("INVALID: Spell slot is out of range.");
        }

        String safeSource = source == null ? "" : source;
        try {
            SpellProgram program = SpellParser.parse(safeSource);
            data.saveSlot(slot, name, program.source());
            if (select) data.setSelectedSlot(slot);
            return Result.success(select ? "VALID — saved and selected." : "VALID — spell saved.",
                SpellProgramAnalysis.estimatedManifestations(program));
        } catch (SpellValidationException exception) {
            SpellDebug.validation(safeSource, exception.getMessage());
            return Result.failure("INVALID: " + exception.getMessage());
        }
    }

    public static Result select(GrimoireData data, int slot) {
        if (!GrimoireData.isValidSlot(slot)) {
            return Result.failure("INVALID: Spell slot is out of range.");
        }

        GrimoireData.SpellSlot saved = data.slot(slot);
        if (saved.source().isBlank()) {
            return Result.failure("INVALID: Spell slot " + (slot + 1) + " is empty.");
        }
        Result validation = validate(saved.source());
        if (!validation.success()) {
            return Result.failure("INVALID SAVED SPELL: "
                + validation.message().substring("INVALID: ".length()));
        }

        data.setSelectedSlot(slot);
        return Result.success("Selected " + saved.name() + " for casting.", validation.manifestations());
    }

    /** Uses the same SpellCastService entry point as /ru cast. */
    public static Result castSelected(ServerPlayer player) {
        GrimoireData data = GrimoireData.get(player);
        GrimoireData.SpellSlot slot = data.slot(data.selectedSlot());
        if (slot.source().isBlank()) {
            return Result.failure("Selected Grimoire slot is empty.");
        }

        try {
            SpellCastService.CastResult cast = SpellCastService.cast(player, slot.source());
            return Result.success(slot.name() + " — cast " + cast.castId());
        } catch (SpellValidationException exception) {
            SpellDebug.validation(slot.source(), exception.getMessage());
            return Result.failure("Invalid saved spell: " + exception.getMessage());
        } catch (RuntimeException exception) {
            RealityUnfolded.LOGGER.error("[RU SPELL] Grimoire cast failure for source {}", slot.source(), exception);
            return Result.failure("Cast failed: " + exception.getMessage());
        }
    }

    public record Result(boolean success, String message, int manifestations) {
        public static Result success(String message) {
            return new Result(true, message, -1);
        }

        public static Result success(String message, int manifestations) {
            return new Result(true, message, manifestations);
        }

        public static Result failure(String message) {
            return new Result(false, message, -1);
        }
    }
}
