package com.anthonyahellman.realityunfolded.grimoire;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.spell.SpellCastService;
import com.anthonyahellman.realityunfolded.spell.SpellDebug;
import com.anthonyahellman.realityunfolded.spell.SpellParser;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellProgramAnalysis;
import com.anthonyahellman.realityunfolded.spell.SpellValidationException;
import com.anthonyahellman.realityunfolded.spell.programmer.SpellGraphCodec;
import com.anthonyahellman.realityunfolded.spell.programmer.SpellGraphCompiler;
import com.anthonyahellman.realityunfolded.spell.programmer.SpellGraphDraft;
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

    /** Saves structurally safe graph instructions even when their semantics resolve to nothing. */
    public static Result saveGraph(GrimoireData data, int slot, String name, String encodedGraph, boolean select) {
        if (!GrimoireData.isValidSlot(slot)) return Result.failure("Spell slot is out of range.");
        try {
            SpellGraphDraft graph = SpellGraphCodec.decode(encodedGraph);
            if (graph.nodes().isEmpty()) {
                data.saveSlot(slot, name, "", SpellGraphCodec.encode(graph));
                return Result.success("Empty instructions saved.", 0);
            }
            SpellProgram program = SpellGraphCompiler.compile(graph);
            String canonicalGraph = SpellGraphCodec.encode(graph);
            data.saveSlot(slot, name, program.source(), canonicalGraph);
            if (select) data.setSelectedSlot(slot);
            return Result.success(select ? "Instructions saved and selected." : "Instructions saved.",
                SpellProgramAnalysis.estimatedManifestations(program));
        } catch (SpellValidationException | IllegalStateException exception) {
            SpellDebug.validation("visual graph", exception.getMessage());
            return Result.failure("Unsafe graph rejected: " + exception.getMessage());
        }
    }

    public static Result select(GrimoireData data, int slot) {
        if (!GrimoireData.isValidSlot(slot)) {
            return Result.failure("INVALID: Spell slot is out of range.");
        }

        GrimoireData.SpellSlot saved = data.slot(slot);
        if (saved.source().isBlank() && saved.graph().isBlank()) {
            return Result.failure("Spell slot " + (slot + 1) + " is empty.");
        }
        try {
            SpellProgram program = compile(saved);
            data.setSelectedSlot(slot);
            return Result.success("Selected " + saved.name() + " for casting.",
                SpellProgramAnalysis.estimatedManifestations(program));
        } catch (SpellValidationException exception) {
            return Result.failure("Stored graph failed safety checks: " + exception.getMessage());
        }
    }

    /** Uses the same SpellCastService entry point as /ru cast. */
    public static Result castSelected(ServerPlayer player) {
        GrimoireData data = GrimoireData.get(player);
        GrimoireData.SpellSlot slot = data.slot(data.selectedSlot());
        if (slot.source().isBlank() && slot.graph().isBlank()) {
            return Result.failure("Selected Grimoire slot is empty.");
        }

        try {
            SpellCastService.CastResult cast = SpellCastService.cast(player, compile(slot));
            return Result.success(slot.name() + " — cast " + cast.castId());
        } catch (SpellValidationException exception) {
            SpellDebug.validation(slot.source(), exception.getMessage());
            return Result.failure("Invalid saved spell: " + exception.getMessage());
        } catch (RuntimeException exception) {
            RealityUnfolded.LOGGER.error("[RU SPELL] Grimoire cast failure for source {}", slot.source(), exception);
            return Result.failure("Cast failed: " + exception.getMessage());
        }
    }

    public static SpellGraphDraft graph(GrimoireData.SpellSlot slot) throws SpellValidationException {
        if (!slot.graph().isBlank()) return SpellGraphCodec.decode(slot.graph());
        if (slot.source().isBlank()) return new SpellGraphDraft();
        return SpellGraphCodec.fromProgram(SpellParser.parse(slot.source()));
    }

    private static SpellProgram compile(GrimoireData.SpellSlot slot) throws SpellValidationException {
        return slot.graph().isBlank() ? SpellParser.parse(slot.source())
            : SpellGraphCompiler.compile(SpellGraphCodec.decode(slot.graph()));
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
