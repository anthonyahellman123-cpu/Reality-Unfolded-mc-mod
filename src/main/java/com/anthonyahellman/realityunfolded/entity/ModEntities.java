package com.anthonyahellman.realityunfolded.entity;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RealityUnfolded.MOD_ID);

    public static final RegistryObject<EntityType<SpellBoltEntity>> SPELL_BOLT = ENTITIES.register("spell_bolt",
        () -> EntityType.Builder.<SpellBoltEntity>of(SpellBoltEntity::new, MobCategory.MISC)
            .sized(0.3F, 0.3F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build(RealityUnfolded.MOD_ID + ":spell_bolt"));
    public static final RegistryObject<EntityType<SpellOrbEntity>> SPELL_ORB = ENTITIES.register("spell_orb",
        () -> EntityType.Builder.<SpellOrbEntity>of(SpellOrbEntity::new, MobCategory.MISC)
            .sized(0.7F, 0.7F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build(RealityUnfolded.MOD_ID + ":spell_orb"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
