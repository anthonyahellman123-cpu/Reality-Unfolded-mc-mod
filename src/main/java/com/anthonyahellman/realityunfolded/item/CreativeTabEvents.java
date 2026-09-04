package com.anthonyahellman.realityunfolded.item;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealityUnfolded.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabEvents {
    private CreativeTabEvents() {}

    @SubscribeEvent
    public static void addItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.GRIMOIRE);
            event.accept(ModItems.VOID_CASTER_GAUNTLET);
        }
    }
}
