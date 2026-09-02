package com.anthonyahellman.realityunfolded.spell.word;

import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import com.anthonyahellman.realityunfolded.spell.SpellNode;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellWord;
import com.anthonyahellman.realityunfolded.spell.WordOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseFireBlock;

public final class IgniteWord implements SpellWord {
    @Override
    public WordOutcome execute(SpellProgram program, SpellNode node, SpellExecutionContext context) {
        if (context.entityTarget() != null) {
            context.entityTarget().setSecondsOnFire(Math.max(1, (int) Math.round(4.0D * context.power(node))));
            return WordOutcome.continueWith(context);
        }
        BlockPos hit = context.blockTarget() != null ? context.blockTarget() : BlockPos.containing(context.position());
        Direction face = context.blockFace() != null ? context.blockFace() : Direction.UP;
        BlockPos firePosition = hit.relative(face);
        if (context.level().getBlockState(firePosition).canBeReplaced()) {
            context.level().setBlockAndUpdate(firePosition, BaseFireBlock.getState(context.level(), firePosition));
            return WordOutcome.continueWith(context);
        }
        return WordOutcome.terminate(context);
    }
}
