package com.anthonyahellman.realityunfolded.client;

import com.anthonyahellman.realityunfolded.entity.SpellOrbEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public final class SpellOrbRenderer extends EntityRenderer<SpellOrbEntity> {
    public SpellOrbRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public ResourceLocation getTextureLocation(SpellOrbEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
