package com.anthonyahellman.realityunfolded.network;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.grimoire.GrimoireData;
import com.anthonyahellman.realityunfolded.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "3";
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
        CHANNEL.messageBuilder(GrimoireFeedbackPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(GrimoireFeedbackPacket::encode)
            .decoder(GrimoireFeedbackPacket::decode)
            .consumerMainThread(GrimoireFeedbackPacket::handle)
            .add();
        CHANNEL.messageBuilder(GrimoireActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(GrimoireActionPacket::encode)
            .decoder(GrimoireActionPacket::decode)
            .consumerMainThread(GrimoireActionPacket::handle)
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

    public static void validateDraft(String source) {
        CHANNEL.sendToServer(new GrimoireActionPacket(
            GrimoireActionPacket.Action.VALIDATE_DRAFT, 0, source));
    }

    public static void selectSavedSlot(int slot) {
        CHANNEL.sendToServer(new GrimoireActionPacket(
            GrimoireActionPacket.Action.SELECT_SAVED, slot, ""));
    }

    public static void castSelected() {
        CHANNEL.sendToServer(new GrimoireActionPacket(
            GrimoireActionPacket.Action.CAST_SELECTED, 0, ""));
    }

    public static void feedback(ServerPlayer player, String message, boolean valid) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new GrimoireFeedbackPacket(message, valid));
    }

    public static boolean isHoldingGrimoire(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.GRIMOIRE.get())
            || player.getOffhandItem().is(ModItems.GRIMOIRE.get());
    }
}
