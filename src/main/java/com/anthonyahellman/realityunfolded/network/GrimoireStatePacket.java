package com.anthonyahellman.realityunfolded.network;

import com.anthonyahellman.realityunfolded.client.ClientGrimoireHandler;
import com.anthonyahellman.realityunfolded.grimoire.GrimoireData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record GrimoireStatePacket(int selectedSlot, List<GrimoireData.SpellSlot> slots,
                                  String message, boolean valid, int manifestations, boolean openScreen) {
    public GrimoireStatePacket {
        slots = List.copyOf(slots);
    }

    public static GrimoireStatePacket from(GrimoireData.Snapshot snapshot, String message,
                                           boolean valid, int manifestations, boolean openScreen) {
        return new GrimoireStatePacket(snapshot.selectedSlot(), snapshot.slots(), message, valid,
            manifestations, openScreen);
    }

    public static void encode(GrimoireStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.selectedSlot);
        buffer.writeVarInt(packet.slots.size());
        for (GrimoireData.SpellSlot slot : packet.slots) {
            buffer.writeUtf(slot.name(), GrimoireData.MAX_NAME_LENGTH);
            buffer.writeUtf(slot.source(), GrimoireData.MAX_SOURCE_LENGTH);
        }
        buffer.writeUtf(packet.message, 256);
        buffer.writeBoolean(packet.valid);
        buffer.writeVarInt(packet.manifestations + 1);
        buffer.writeBoolean(packet.openScreen);
    }

    public static GrimoireStatePacket decode(FriendlyByteBuf buffer) {
        int selected = buffer.readVarInt();
        int count = Math.min(GrimoireData.SLOT_COUNT, buffer.readVarInt());
        List<GrimoireData.SpellSlot> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add(new GrimoireData.SpellSlot(
                buffer.readUtf(GrimoireData.MAX_NAME_LENGTH),
                buffer.readUtf(GrimoireData.MAX_SOURCE_LENGTH)));
        }
        return new GrimoireStatePacket(selected, slots, buffer.readUtf(256),
            buffer.readBoolean(), buffer.readVarInt() - 1, buffer.readBoolean());
    }

    public static void handle(GrimoireStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientGrimoireHandler.accept(packet)));
        contextSupplier.get().setPacketHandled(true);
    }
}
