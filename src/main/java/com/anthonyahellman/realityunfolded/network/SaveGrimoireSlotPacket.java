package com.anthonyahellman.realityunfolded.network;

import com.anthonyahellman.realityunfolded.grimoire.GrimoireData;
import com.anthonyahellman.realityunfolded.grimoire.GrimoireSpellService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SaveGrimoireSlotPacket(int slot, String name, String source, boolean select) {
    public static void encode(SaveGrimoireSlotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
        buffer.writeUtf(packet.name, GrimoireData.MAX_NAME_LENGTH);
        buffer.writeUtf(packet.source, GrimoireData.MAX_SOURCE_LENGTH);
        buffer.writeBoolean(packet.select);
    }

    public static SaveGrimoireSlotPacket decode(FriendlyByteBuf buffer) {
        return new SaveGrimoireSlotPacket(buffer.readVarInt(),
            buffer.readUtf(GrimoireData.MAX_NAME_LENGTH),
            buffer.readUtf(GrimoireData.MAX_SOURCE_LENGTH), buffer.readBoolean());
    }

    public static void handle(SaveGrimoireSlotPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) contextSupplier.get().enqueueWork(() -> save(player, packet));
        contextSupplier.get().setPacketHandled(true);
    }

    private static void save(ServerPlayer player, SaveGrimoireSlotPacket packet) {
        if (!ModNetwork.isHoldingGrimoire(player)) {
            ModNetwork.feedback(player, "INVALID: Hold the Grimoire while editing.", false);
            return;
        }
        GrimoireSpellService.Result result = GrimoireSpellService.save(
            GrimoireData.get(player), packet.slot, packet.name, packet.source, packet.select);
        if (result.success()) {
            ModNetwork.updateGrimoire(player,
                result.message(), true);
        } else {
            ModNetwork.feedback(player, result.message(), false);
        }
    }
}
