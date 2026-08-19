package com.anthonyahellman.realityunfolded.mana;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealityUnfolded.MOD_ID)
public final class ManaEvents {
    private ManaEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (event.player.tickCount % 10 == 0) {
            ManaManager.regenerate((ServerPlayer) event.player, 10);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ModNetwork.syncMana(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ModNetwork.syncMana(player);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ModNetwork.syncMana(player);
    }
}
