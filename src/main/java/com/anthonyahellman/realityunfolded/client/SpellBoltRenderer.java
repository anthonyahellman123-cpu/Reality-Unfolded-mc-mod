package com.anthonyahellman.realityunfolded.client;

import com.anthonyahellman.realityunfolded.entity.SpellBoltEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public final class SpellBoltRenderer extends EntityRenderer<SpellBoltEntity> {
    public SpellBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SpellBoltEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
