package com.anthonyahellman.realityunfolded.command;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.mana.ManaData;
import com.anthonyahellman.realityunfolded.mana.ManaManager;
import com.anthonyahellman.realityunfolded.network.ModNetwork;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealityUnfolded.MOD_ID)
public final class ManaCommands {
    private ManaCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("realitymana")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("get")
                .executes(context -> show(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> show(context.getSource(), EntityArgument.getPlayer(context, "target")))))
            .then(Commands.literal("refill")
                .executes(context -> mutate(context.getSource(), context.getSource().getPlayerOrException(), Action.REFILL, 0.0D))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> mutate(context.getSource(), EntityArgument.getPlayer(context, "target"), Action.REFILL, 0.0D))))
            .then(valueCommand("set", Action.SET))
            .then(valueCommand("add", Action.ADD))
            .then(valueCommand("drain", Action.DRAIN))
            .then(valueCommand("capacity", Action.CAPACITY))
            .then(valueCommand("regen", Action.REGEN)));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> valueCommand(String name, Action action) {
        return Commands.literal(name)
            .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                .executes(context -> mutate(context.getSource(), context.getSource().getPlayerOrException(), action,
                    DoubleArgumentType.getDouble(context, "amount")))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> mutate(context.getSource(), EntityArgument.getPlayer(context, "target"), action,
                        DoubleArgumentType.getDouble(context, "amount")))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        ManaData mana = ManaData.get(player);
        double multiplier = ManaManager.dimensionMultiplier(player.level().dimension());
        source.sendSuccess(() -> Component.literal(String.format(
            "%s — Mana %.2f/%.2f | Regen %.2f/s × %.2f dimension",
            player.getGameProfile().getName(), mana.current(), mana.maximum(), mana.regenPerSecond(), multiplier)), false);
        return 1;
    }

    private static int mutate(CommandSourceStack source, ServerPlayer player, Action action, double amount) {
        ManaData mana = ManaData.get(player);
        switch (action) {
            case SET -> mana.setCurrent(amount);
            case ADD -> mana.add(amount);
            case DRAIN -> mana.setCurrent(mana.current() - amount);
            case CAPACITY -> mana.setMaximum(amount);
            case REGEN -> mana.setRegenPerSecond(amount);
            case REFILL -> mana.setCurrent(mana.maximum());
        }
        ModNetwork.syncMana(player);
        return show(source, player);
    }

    private enum Action { SET, ADD, DRAIN, CAPACITY, REGEN, REFILL }
}
