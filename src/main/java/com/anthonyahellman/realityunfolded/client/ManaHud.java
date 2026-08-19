package com.anthonyahellman.realityunfolded.client;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealityUnfolded.MOD_ID, value = Dist.CLIENT)
public final class ManaHud {
    private static final int WIDTH = 90;
    private static final int HEIGHT = 7;

    private ManaHud() {}

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 8;
        int y = 8;
        double maximum = Math.max(1.0D, ClientMana.maximum());
        int filled = (int) Math.round(WIDTH * Math.max(0.0D, Math.min(1.0D, ClientMana.current() / maximum)));

        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xCC090716);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF24183C);
        graphics.fill(x, y, x + filled, y + HEIGHT, 0xFF8D5CFF);
        graphics.fill(x, y, x + filled, y + 1, 0xFFC7AFFF);

        String label = String.format("Mana %.1f / %.1f", ClientMana.current(), ClientMana.maximum());
        graphics.drawString(minecraft.font, label, x, y + HEIGHT + 3, 0xFFE8DFFF, true);
    }
}
