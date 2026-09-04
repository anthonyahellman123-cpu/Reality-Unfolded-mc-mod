package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.*;
import com.anthonyahellman.realityunfolded.spell.runtime.TargetResolver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class TouchWord implements SpellWord {
    @Override public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        ServerPlayer caster = context.caster();
        if (caster == null) return WordOutcome.terminate(context.withCondition(false));

        Vec3 eye = caster.getEyePosition();
        double blockReach = TargetResolver.blockReach(caster);
        double entityReach = TargetResolver.entityReach(caster);
        BlockHitResult blockHit = caster.pick(blockReach, 0.0F, false) instanceof BlockHitResult hit
            && hit.getType() == HitResult.Type.BLOCK ? hit : null;
        Vec3 entityEnd = eye.add(caster.getLookAngle().scale(entityReach));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(caster, eye, entityEnd,
            caster.getBoundingBox().expandTowards(caster.getLookAngle().scale(entityReach)).inflate(1.0D),
            entity -> entity != caster && touchable(entity), entityReach * entityReach);

        double blockDistance = blockHit == null ? Double.MAX_VALUE : eye.distanceToSqr(blockHit.getLocation());
        double entityDistance = entityHit == null ? Double.MAX_VALUE : eye.distanceToSqr(entityHit.getLocation());
        SpellExecutionContext resolved;
        if (entityDistance < blockDistance) {
            resolved = context.withEntityTarget(entityHit.getEntity());
        } else if (blockHit != null) {
            resolved = context.withBlockTarget(blockHit.getBlockPos(), blockHit.getDirection());
        } else {
            resolved = context.withBlockTarget(null, null);
        }
        SpellDebug.target(resolved, resolved.entityTarget());
        return WordOutcome.continueWith(resolved);
    }

    private static boolean touchable(Entity entity) {
        return entity.isPickable() && !entity.isSpectator();
    }
}
