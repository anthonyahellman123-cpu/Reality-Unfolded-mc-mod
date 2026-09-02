package com.anthonyahellman.realityunfolded.network;

import com.anthonyahellman.realityunfolded.grimoire.GrimoireData;
import com.anthonyahellman.realityunfolded.item.ModItems;
import com.anthonyahellman.realityunfolded.spell.SpellDebug;
import com.anthonyahellman.realityunfolded.spell.SpellParser;
import com.anthonyahellman.realityunfolded.spell.SpellValidationException;
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
        boolean holdingGrimoire = player.getMainHandItem().is(ModItems.GRIMOIRE.get())
            || player.getOffhandItem().is(ModItems.GRIMOIRE.get());
        if (!holdingGrimoire) {
            ModNetwork.updateGrimoire(player, "INVALID: Hold the Grimoire while editing.", false);
            return;
        }
        if (packet.slot < 0 || packet.slot >= GrimoireData.SLOT_COUNT) {
            ModNetwork.updateGrimoire(player, "INVALID: Spell slot is out of range.", false);
            return;
        }
        try {
            SpellParser.parse(packet.source);
            GrimoireData data = GrimoireData.get(player);
            data.saveSlot(packet.slot, packet.name, packet.source);
            if (packet.select) data.setSelectedSlot(packet.slot);
            ModNetwork.updateGrimoire(player,
                packet.select ? "VALID — saved and selected." : "VALID — spell saved.", true);
        } catch (SpellValidationException exception) {
            SpellDebug.validation(packet.source, exception.getMessage());
            ModNetwork.updateGrimoire(player, "INVALID: " + exception.getMessage(), false);
        }
    }
}
