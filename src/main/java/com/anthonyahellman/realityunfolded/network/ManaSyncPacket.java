package com.anthonyahellman.realityunfolded.network;

import com.anthonyahellman.realityunfolded.client.ClientMana;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ManaSyncPacket(double current, double maximum, double regenPerSecond) {
    public static void encode(ManaSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.current);
        buffer.writeDouble(packet.maximum);
        buffer.writeDouble(packet.regenPerSecond);
    }

    public static ManaSyncPacket decode(FriendlyByteBuf buffer) {
        return new ManaSyncPacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(ManaSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> ClientMana.update(
            packet.current, packet.maximum, packet.regenPerSecond));
        contextSupplier.get().setPacketHandled(true);
    }
}
