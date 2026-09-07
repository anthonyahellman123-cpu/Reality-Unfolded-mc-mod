package com.anthonyahellman.realityunfolded.command;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.testreality.TestRealityData;
import com.anthonyahellman.realityunfolded.testreality.TestRealityService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealityUnfolded.MOD_ID)
public final class TestRealityCommands {
    private TestRealityCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                 net.minecraft.commands.CommandBuildContext buildContext) {
        var fill = Commands.literal("fill").then(Commands.argument("from", BlockPosArgument.blockPos())
            .then(Commands.argument("to", BlockPosArgument.blockPos())
                .then(Commands.argument("block", BlockStateArgument.block(buildContext))
                    .executes(context -> fill(context.getSource(),
                        BlockPosArgument.getLoadedBlockPos(context, "from"),
                        BlockPosArgument.getLoadedBlockPos(context, "to"),
                        BlockStateArgument.getBlock(context, "block"))))));
        var reality = Commands.literal("reality")
            .then(Commands.literal("enter").executes(context -> reply(context.getSource(),
                TestRealityService.enter(context.getSource().getPlayerOrException()))))
            .then(Commands.literal("exit").executes(context -> reply(context.getSource(),
                TestRealityService.exit(context.getSource().getPlayerOrException()))))
            .then(Commands.literal("status").executes(context -> status(context.getSource())))
            .then(Commands.literal("seed")
                .then(Commands.argument("value", LongArgumentType.longArg()).executes(context -> reply(
                    context.getSource(), TestRealityService.setSeed(context.getSource().getPlayerOrException(),
                        LongArgumentType.getLong(context, "value"))))))
            .then(Commands.literal("edit")
                .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> reply(
                    context.getSource(), TestRealityService.setEditMode(context.getSource().getPlayerOrException(),
                        BoolArgumentType.getBool(context, "enabled"))))))
            .then(Commands.literal("save").executes(context -> reply(context.getSource(),
                TestRealityService.saveEdits(context.getSource().getPlayerOrException()))))
            .then(Commands.literal("diagnostics")
                .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> diagnostics(
                    context.getSource(), BoolArgumentType.getBool(context, "enabled")))))
            .then(fill);
        dispatcher.register(Commands.literal("ru").requires(source -> source.hasPermission(2)).then(reality));
    }

    private static int fill(CommandSourceStack source, net.minecraft.core.BlockPos from,
                            net.minecraft.core.BlockPos to, BlockInput block) throws CommandSyntaxException {
        return reply(source, TestRealityService.stageFill(source.getPlayerOrException(), from, to,
            block.getState().getBlock()));
    }

    private static int diagnostics(CommandSourceStack source, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TestRealityData.get(player).setDiagnostics(enabled);
        source.sendSuccess(() -> Component.literal("[RU] Test Reality diagnostics "
            + (enabled ? "enabled" : "disabled") + " for the next instance."), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        source.sendSuccess(() -> Component.literal("[RU] "
            + TestRealityService.status(source.getPlayerOrException())), false);
        return 1;
    }

    private static int reply(CommandSourceStack source, TestRealityService.Result result) {
        if (result.success()) source.sendSuccess(() -> Component.literal("[RU] " + result.message()), false);
        else source.sendFailure(Component.literal("[RU] " + result.message()));
        return result.success() ? 1 : 0;
    }
}
