package com.anthonyahellman.realityunfolded.item;

import com.anthonyahellman.realityunfolded.grimoire.GrimoireSpellService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Early-game channeling apparatus. The selected spell remains in player-persistent data. */
public final class VoidCasterGauntletItem extends Item {
    private static final int CAST_COOLDOWN_TICKS = 5;

    public VoidCasterGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);

        GrimoireSpellService.Result result = GrimoireSpellService.castSelected(serverPlayer);
        serverPlayer.displayClientMessage(Component.literal("[RU] " + result.message()), true);
        if (result.success()) {
            serverPlayer.getCooldowns().addCooldown(this, CAST_COOLDOWN_TICKS);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }
}
