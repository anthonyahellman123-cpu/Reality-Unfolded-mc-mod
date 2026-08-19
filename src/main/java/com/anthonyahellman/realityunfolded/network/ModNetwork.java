package com.anthonyahellman.realityunfolded.network;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.mana.ManaData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(RealityUnfolded.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    private static int packetId;

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ManaSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ManaSyncPacket::encode)
            .decoder(ManaSyncPacket::decode)
            .consumerMainThread(ManaSyncPacket::handle)
            .add();
    }

    public static void syncMana(ServerPlayer player) {
        ManaData mana = ManaData.get(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new ManaSyncPacket(mana.current(), mana.maximum(), mana.regenPerSecond()));
    }
}
