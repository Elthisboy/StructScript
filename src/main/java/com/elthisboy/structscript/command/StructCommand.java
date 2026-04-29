package com.elthisboy.structscript.command;

import com.elthisboy.structscript.item.StructureBlueprintItem;
import com.elthisboy.structscript.registry.ModItems;
import com.elthisboy.structscript.schematic.SchematicManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class StructCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(StructCommand::registerCommand);
    }

    private static void registerCommand(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(
            literal("structscript")
                .requires(source -> source.hasPermissionLevel(2))

                .then(literal("reload")
                    .executes(context -> {
                        SchematicManager.clearCache();
                        context.getSource().sendFeedback(() ->
                            Text.translatable("message.structscript.reload"), true);
                        return 1;
                    })
                )

                .then(literal("give")
                    .then(argument("file", StringArgumentType.string())
                        .then(argument("amount", IntegerArgumentType.integer(1))

                            // /structscript give <file> <amount> <targets>
                            .then(argument("targets", EntityArgumentType.players())
                                .executes(context -> {
                                    String file   = StringArgumentType.getString(context, "file");
                                    int    amount = IntegerArgumentType.getInteger(context, "amount");
                                    Collection<ServerPlayerEntity> targets =
                                        EntityArgumentType.getPlayers(context, "targets");

                                    for (ServerPlayerEntity target : targets) {
                                        ItemStack stack = new ItemStack(ModItems.STRUCTURE_BLUEPRINT, amount);
                                        StructureBlueprintItem.setStructure(stack, file);
                                        target.giveItemStack(stack);
                                    }

                                    if (targets.size() == 1) {
                                        String name = targets.iterator().next().getName().getString();
                                        context.getSource().sendFeedback(() ->
                                            Text.translatable("message.structscript.given_one",
                                                amount, file, name), true);
                                    } else {
                                        int count = targets.size();
                                        context.getSource().sendFeedback(() ->
                                            Text.translatable("message.structscript.given_many",
                                                amount, file, count), true);
                                    }
                                    return targets.size();
                                })
                            )

                            // /structscript give <file> <amount>  (sin targets = ejecutor)
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;

                                String file   = StringArgumentType.getString(context, "file");
                                int    amount = IntegerArgumentType.getInteger(context, "amount");

                                ItemStack stack = new ItemStack(ModItems.STRUCTURE_BLUEPRINT, amount);
                                StructureBlueprintItem.setStructure(stack, file);
                                player.giveItemStack(stack);

                                context.getSource().sendFeedback(() ->
                                    Text.translatable("message.structscript.given_self",
                                        amount, file), true);
                                return 1;
                            })
                        )
                    )
                )
        );
    }
}
