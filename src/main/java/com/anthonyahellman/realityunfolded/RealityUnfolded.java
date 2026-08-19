package com.anthonyahellman.realityunfolded;

import com.anthonyahellman.realityunfolded.network.ModNetwork;
import net.minecraftforge.fml.common.Mod;

@Mod(RealityUnfolded.MOD_ID)
public final class RealityUnfolded {
    public static final String MOD_ID = "reality_unfolded";

    public RealityUnfolded() {
        ModNetwork.register();
    }
}
