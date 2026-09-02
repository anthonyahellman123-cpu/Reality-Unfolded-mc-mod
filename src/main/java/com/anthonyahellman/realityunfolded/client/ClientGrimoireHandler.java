package com.anthonyahellman.realityunfolded.client;

import com.anthonyahellman.realityunfolded.network.GrimoireStatePacket;
import net.minecraft.client.Minecraft;

public final class ClientGrimoireHandler {
    private ClientGrimoireHandler() {}

    public static void accept(GrimoireStatePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.openScreen()) {
            minecraft.setScreen(new GrimoireScreen(packet));
        } else if (minecraft.screen instanceof GrimoireScreen screen) {
            screen.acceptServerState(packet);
        }
    }
}
