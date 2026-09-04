package com.anthonyahellman.realityunfolded.network;

import com.anthonyahellman.realityunfolded.grimoire.GrimoireData;
import com.anthonyahellman.realityunfolded.grimoire.GrimoireSpellService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Small command-free UI actions. Every action is revalidated and executed on the server. */
public record GrimoireActionPacket(Action action, int slot, String source) {
    public enum Action {
        VALIDATE_DRAFT,
        SELECT_SAVED,
        UNKNOWN;

        private static Action byId(int id) {
            Action[] values = values();
            return id >= 0 && id < values.length - 1 ? values[id] : UNKNOWN;
        }
    }

    public static void encode(GrimoireActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action.ordinal());
        buffer.writeVarInt(packet.slot);
        buffer.writeUtf(packet.source, GrimoireData.MAX_SOURCE_LENGTH);
    }

    public static GrimoireActionPacket decode(FriendlyByteBuf buffer) {
        return new GrimoireActionPacket(Action.byId(buffer.readVarInt()), buffer.readVarInt(),
            buffer.readUtf(GrimoireData.MAX_SOURCE_LENGTH));
    }

    public static void handle(GrimoireActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) context.enqueueWork(() -> perform(player, packet));
        context.setPacketHandled(true);
    }

    private static void perform(ServerPlayer player, GrimoireActionPacket packet) {
        if (!ModNetwork.isHoldingGrimoire(player)) {
            ModNetwork.feedback(player, "INVALID: Hold the Grimoire while using Spellcraft.", false, -1);
            return;
        }

        GrimoireSpellService.Result result;
        switch (packet.action) {
            case VALIDATE_DRAFT -> {
                result = GrimoireSpellService.validate(packet.source);
                ModNetwork.feedback(player, result.message(), result.success(), result.manifestations());
            }
            case SELECT_SAVED -> {
                result = GrimoireSpellService.select(GrimoireData.get(player), packet.slot);
                ModNetwork.updateGrimoire(player, result.message(), result.success(), result.manifestations());
            }
            default -> ModNetwork.feedback(player, "INVALID: Unknown Grimoire action.", false, -1);
        }
    }
}
