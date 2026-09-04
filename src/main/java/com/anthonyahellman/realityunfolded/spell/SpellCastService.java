package com.anthonyahellman.realityunfolded.spell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** The single server-authoritative entry point shared by commands and the Grimoire. */
public final class SpellCastService {
    private SpellCastService() {}

    public static CastResult cast(ServerPlayer player, String source) throws SpellValidationException {
        SpellProgram program = SpellParser.parse(source);
        UUID castId = UUID.randomUUID();
        HitResult lookedAt = player.pick(64.0D, 0.0F, false);
        BlockHitResult blockHit = lookedAt instanceof BlockHitResult hit ? hit : null;
        Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.6D));
        SpellExecutionContext execution = new SpellExecutionContext((ServerLevel) player.level(),
            player.getUUID(), castId, castId, null, SpellPhase.CAST, origin, null, null,
            blockHit == null ? null : blockHit.getBlockPos(),
            blockHit == null ? null : blockHit.getDirection(), null, null, false, false, 1.0D);
        SpellExecutor.begin(program, execution);
        return new CastResult(castId, program.source());
    }

    public record CastResult(UUID castId, String source) {}
}
