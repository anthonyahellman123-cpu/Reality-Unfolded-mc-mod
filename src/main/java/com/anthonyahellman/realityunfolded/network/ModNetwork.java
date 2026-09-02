package com.anthonyahellman.realityunfolded.network;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.grimoire.GrimoireData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(RealityUnfolded.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );
    private static int packetId;

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(GrimoireStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(GrimoireStatePacket::encode)
            .decoder(GrimoireStatePacket::decode)
            .consumerMainThread(GrimoireStatePacket::handle)
            .add();
        CHANNEL.messageBuilder(SaveGrimoireSlotPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(SaveGrimoireSlotPacket::encode)
            .decoder(SaveGrimoireSlotPacket::decode)
            .consumerMainThread(SaveGrimoireSlotPacket::handle)
            .add();
    }

    public static void openGrimoire(ServerPlayer player, String message, boolean valid) {
        GrimoireData.Snapshot snapshot = GrimoireData.get(player).snapshot();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            GrimoireStatePacket.from(snapshot, message, valid, true));
    }

    public static void updateGrimoire(ServerPlayer player, String message, boolean valid) {
        GrimoireData.Snapshot snapshot = GrimoireData.get(player).snapshot();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            GrimoireStatePacket.from(snapshot, message, valid, false));
    }

    public static void saveSlot(int slot, String name, String source, boolean select) {
        CHANNEL.sendToServer(new SaveGrimoireSlotPacket(slot, name, source, select));
    }
}
