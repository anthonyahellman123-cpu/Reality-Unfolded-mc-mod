package com.anthonyahellman.realityunfolded.item;

import com.anthonyahellman.realityunfolded.grimoire.GrimoireSpellService;
import com.anthonyahellman.realityunfolded.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GrimoireItem extends Item {
    public GrimoireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);

        if (player.isShiftKeyDown()) {
            castSelected(serverPlayer);
        } else {
            ModNetwork.openGrimoire(serverPlayer, "Select, edit, or cast a spell.", true);
        }
        return InteractionResultHolder.consume(stack);
    }

    private static void castSelected(ServerPlayer player) {
        GrimoireSpellService.Result result = GrimoireSpellService.castSelected(player);
        player.displayClientMessage(Component.literal("[RU] " + result.message()), result.success());
    }
}
