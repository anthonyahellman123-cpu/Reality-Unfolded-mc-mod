package com.anthonyahellman.realityunfolded.mana;

import com.anthonyahellman.realityunfolded.network.ModNetwork;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public final class ManaManager {
    private ManaManager() {}

    public static void regenerate(ServerPlayer player, int elapsedTicks) {
        ManaData mana = ManaData.get(player);
        double amount = mana.regenPerSecond() * dimensionMultiplier(player.level().dimension()) * elapsedTicks / 20.0D;
        if (mana.add(amount)) {
            ModNetwork.syncMana(player);
        }
    }

    public static double dimensionMultiplier(ResourceKey<Level> dimension) {
        if (dimension == Level.END) return 2.0D;
        if (dimension == Level.NETHER) return 1.25D;
        return 1.0D;
    }
}
