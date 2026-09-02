package com.anthonyahellman.realityunfolded.spell;

import com.anthonyahellman.realityunfolded.RealityUnfolded;

public final class SpellDebug {
    private SpellDebug() {}

    public static void cast(SpellProgram program, SpellExecutionContext context) {
        RealityUnfolded.LOGGER.info("[RU SPELL] CAST_INSTANCE cast={} caster={} source=\"{}\"",
            context.castId(), context.casterId(), program.source());
    }

    public static void node(SpellNode node, SpellExecutionContext context) {
        RealityUnfolded.LOGGER.info(
            "[RU SPELL] WORD_NODE cast={} branch={} node={} word={} phase={} pos={} targetEntity={} targetBlock={} power={}",
            context.castId(), context.branchId(), node.id(), node.word(), context.phase(),
            context.position(), context.entityTarget() == null ? "none" : context.entityTarget().getUUID(),
            context.blockTarget() == null ? "none" : context.blockTarget(), context.power(node));
    }

    public static void validation(String source, String reason) {
        RealityUnfolded.LOGGER.warn("[RU SPELL] VALIDATION_FAILURE source=\"{}\" reason={}", source, reason);
    }

    public static void termination(SpellExecutionContext context, String reason) {
        RealityUnfolded.LOGGER.info("[RU SPELL] SPELL_TERMINATION cast={} branch={} reason={}",
            context.castId(), context.branchId(), reason);
    }

    public static void child(SpellExecutionContext parent, java.util.UUID child) {
        RealityUnfolded.LOGGER.info("[RU SPELL] CHILD_MANIFESTATION cast={} parent={} child={}",
            parent.castId(), parent.branchId(), child);
    }

    public static void impact(SpellExecutionContext context) {
        RealityUnfolded.LOGGER.info("[RU SPELL] IMPACT_EVENT cast={} branch={} pos={} entity={} block={}",
            context.castId(), context.branchId(), context.position(),
            context.entityTarget() == null ? "none" : context.entityTarget().getUUID(),
            context.blockTarget() == null ? "none" : context.blockTarget());
    }
}
