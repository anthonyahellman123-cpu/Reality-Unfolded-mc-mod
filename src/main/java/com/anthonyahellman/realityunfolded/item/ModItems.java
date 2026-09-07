package com.anthonyahellman.realityunfolded.item;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, RealityUnfolded.MOD_ID);

    public static final RegistryObject<Item> GRIMOIRE = ITEMS.register("grimoire",
        () -> new GrimoireItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> VOID_CASTER_GAUNTLET = ITEMS.register("void_caster_gauntlet",
        () -> new VoidCasterGauntletItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
