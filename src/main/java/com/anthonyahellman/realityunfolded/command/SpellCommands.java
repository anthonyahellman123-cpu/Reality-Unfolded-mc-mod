package com.anthonyahellman.realityunfolded.command;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.spell.SpellDebug;
import com.anthonyahellman.realityunfolded.spell.SpellExecutionContext;
import com.anthonyahellman.realityunfolded.spell.SpellExecutor;
import com.anthonyahellman.realityunfolded.spell.SpellNode;
import com.anthonyahellman.realityunfolded.spell.SpellParser;
import com.anthonyahellman.realityunfolded.spell.SpellPhase;
import com.anthonyahellman.realityunfolded.spell.SpellProgram;
import com.anthonyahellman.realityunfolded.spell.SpellValidationException;
import com.anthonyahellman.realityunfolded.spell.runtime.LinkRuntime;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = RealityUnfolded.MOD_ID)
public final class SpellCommands {
    private SpellCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ru")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("cast")
                .then(Commands.argument("words", StringArgumentType.greedyString())
                    .executes(context -> cast(context.getSource(), StringArgumentType.getString(context, "words")))))
            .then(Commands.literal("inspect")
                .then(Commands.argument("words", StringArgumentType.greedyString())
                    .executes(context -> inspect(context.getSource(), StringArgumentType.getString(context, "words")))))
            .then(Commands.literal("links")
                .executes(context -> links(context.getSource())))
            .then(Commands.literal("examples")
                .executes(context -> examples(context.getSource()))));
    }

    private static int cast(CommandSourceStack source, String words) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            SpellProgram program = SpellParser.parse(words);
            UUID castId = UUID.randomUUID();
            HitResult lookedAt = player.pick(64.0D, 0.0F, false);
            BlockHitResult blockHit = lookedAt instanceof BlockHitResult hit ? hit : null;
            Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.6D));
            SpellExecutionContext execution = new SpellExecutionContext((ServerLevel) player.level(),
                player.getUUID(), castId, castId, null, SpellPhase.CAST, origin, null, null,
                blockHit == null ? null : blockHit.getBlockPos(),
                blockHit == null ? null : blockHit.getDirection(), 1.0D);
            SpellExecutor.begin(program, execution);
            source.sendSuccess(() -> Component.literal("[RU] Cast " + castId + ": " + program.source()), false);
            return 1;
        } catch (SpellValidationException exception) {
            SpellDebug.validation(words, exception.getMessage());
            source.sendFailure(Component.literal("[RU] Invalid spell: " + exception.getMessage()));
            return 0;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("[RU] Cast failed: " + exception.getMessage()));
            RealityUnfolded.LOGGER.error("[RU SPELL] Cast failure for source {}", words, exception);
            return 0;
        }
    }

    private static int inspect(CommandSourceStack source, String words) {
        try {
            SpellProgram program = SpellParser.parse(words);
            source.sendSuccess(() -> Component.literal("[RU] Program root=" + program.rootNode()
                + " nodes=" + program.nodes().size()), false);
            for (SpellNode node : program.nodes()) {
                source.sendSuccess(() -> Component.literal(String.format(
                    "  #%d %s arg=%d power=%.2f next=%s", node.id(), node.word(),
                    node.integerArgument(), node.powerMultiplier(), node.next())), false);
            }
            return 1;
        } catch (SpellValidationException exception) {
            SpellDebug.validation(words, exception.getMessage());
            source.sendFailure(Component.literal("[RU] Invalid spell: " + exception.getMessage()));
            return 0;
        }
    }

    private static int links(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int relationships = LinkRuntime.relationshipCount(level);
        int members = LinkRuntime.memberCount(level);
        source.sendSuccess(() -> Component.literal("[RU] Active link relationships: "
            + relationships + " | manifestations: " + members), false);
        return relationships;
    }

    private static int examples(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[RU] /ru cast BOLT"), false);
        source.sendSuccess(() -> Component.literal("[RU] /ru cast BOLT IMPACT IGNITE"), false);
        source.sendSuccess(() -> Component.literal("[RU] /ru cast BOLT IMPACT EXPLOSION AMPLIFY"), false);
        source.sendSuccess(() -> Component.literal("[RU] /ru cast BREAK"), false);
        source.sendSuccess(() -> Component.literal("[RU] /ru cast BOLT HOME IMPACT IGNITE"), false);
        source.sendSuccess(() -> Component.literal("[RU] /ru cast BOLT SPLIT(3) LINK IMPACT IGNITE"), false);
        return 1;
    }
}
