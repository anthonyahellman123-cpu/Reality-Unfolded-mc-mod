package com.anthonyahellman.realityunfolded;

import com.anthonyahellman.realityunfolded.entity.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RealityUnfolded.MOD_ID)
public final class RealityUnfolded {
    public static final String MOD_ID = "reality_unfolded";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RealityUnfolded() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.register(modBus);
    }
}
