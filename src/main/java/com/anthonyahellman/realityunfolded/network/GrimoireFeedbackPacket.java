package com.anthonyahellman.realityunfolded.network;

import com.anthonyahellman.realityunfolded.client.ClientGrimoireHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Status-only response so draft validation never overwrites the client's editor contents. */
public record GrimoireFeedbackPacket(String message, boolean valid) {
    public static void encode(GrimoireFeedbackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.message, 256);
        buffer.writeBoolean(packet.valid);
    }

    public static GrimoireFeedbackPacket decode(FriendlyByteBuf buffer) {
        return new GrimoireFeedbackPacket(buffer.readUtf(256), buffer.readBoolean());
    }

    public static void handle(GrimoireFeedbackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientGrimoireHandler.accept(packet)));
        contextSupplier.get().setPacketHandled(true);
    }
}
