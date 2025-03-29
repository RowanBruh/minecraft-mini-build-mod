package com.aicompanion.mod.command;

import com.aicompanion.mod.entity.AICompanionEntity;
import com.aicompanion.mod.network.NetworkHandler;
import com.aicompanion.mod.network.message.CommandMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.ItemArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AICompanionCommand {
    
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            Commands.literal("aicompanion")
                .requires(source -> source.hasPermission(0)) // Anyone can use
                .then(Commands.argument("command", StringArgumentType.word())
                    .suggests((context, builder) -> builder
                        .suggest("follow")
                        .suggest("stay")
                        .suggest("move")
                        .suggest("break")
                        .suggest("place")
                        .suggest("help")
                        .suggest("list")
                        .build())
                    .executes(context -> executeCommand(
                        context.getSource(), 
                        StringArgumentType.getString(context, "command"),
                        null, 
                        ItemStack.EMPTY
                    ))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> executeCommand(
                            context.getSource(),
                            StringArgumentType.getString(context, "command"),
                            BlockPosArgument.getBlockPos(context, "pos"),
                            ItemStack.EMPTY
                        ))
                        .then(Commands.argument("item", ItemArgument.item())
                            .executes(context -> executeCommand(
                                context.getSource(),
                                StringArgumentType.getString(context, "command"),
                                BlockPosArgument.getBlockPos(context, "pos"),
                                ItemArgument.getItem(context, "item").getDefaultInstance()
                            ))
                        )
                    )
                )
        );
    }
    
    private static int executeCommand(CommandSource source, String command, BlockPos pos, ItemStack item) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        
        // Handle help command
        if (command.equalsIgnoreCase("help")) {
            displayHelp(player);
            return 1;
        }
        
        // Handle list command - find all owned companions
        if (command.equalsIgnoreCase("list")) {
            listCompanions(player);
            return 1;
        }
        
        // Find the closest owned AI companion
        List<AICompanionEntity> companions = player.level.getEntitiesOfClass(
                AICompanionEntity.class, 
                player.getBoundingBox().inflate(32.0D),
                entity -> entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID())
        );
        
        if (companions.isEmpty()) {
            player.sendMessage(new StringTextComponent("You don't have any AI companions nearby"), UUID.randomUUID());
            return 0;
        }
        
        // Get the closest companion
        AICompanionEntity companion = companions.get(0);
        if (companions.size() > 1) {
            companion = companions.stream()
                    .min((e1, e2) -> 
                            (int)(e1.distanceToSqr(player) - e2.distanceToSqr(player)))
                    .orElse(companions.get(0));
            
            player.sendMessage(new StringTextComponent("Commanding your nearest AI companion"), UUID.randomUUID());
        }
        
        // Process the command
        switch (command.toLowerCase()) {
            case "follow":
                companion.processCommand("follow", null, ItemStack.EMPTY);
                break;
                
            case "stay":
                companion.processCommand("stay", null, ItemStack.EMPTY);
                break;
                
            case "move":
                if (pos != null) {
                    companion.processCommand("move", pos, ItemStack.EMPTY);
                } else {
                    player.sendMessage(new StringTextComponent("Usage: /aicompanion move <x> <y> <z>"), UUID.randomUUID());
                    return 0;
                }
                break;
                
            case "break":
                if (pos != null) {
                    companion.processCommand("break", pos, ItemStack.EMPTY);
                } else {
                    player.sendMessage(new StringTextComponent("Usage: /aicompanion break <x> <y> <z>"), UUID.randomUUID());
                    return 0;
                }
                break;
                
            case "place":
                if (pos != null) {
                    if (!item.isEmpty()) {
                        companion.processCommand("place", pos, item);
                    } else {
                        // Try to use what the player is holding
                        ItemStack heldItem = player.getMainHandItem();
                        if (!heldItem.isEmpty()) {
                            companion.processCommand("place", pos, heldItem.copy());
                        } else {
                            player.sendMessage(new StringTextComponent(
                                    "Usage: /aicompanion place <x> <y> <z> <item>"), UUID.randomUUID());
                            return 0;
                        }
                    }
                } else {
                    player.sendMessage(new StringTextComponent(
                            "Usage: /aicompanion place <x> <y> <z> <item>"), UUID.randomUUID());
                    return 0;
                }
                break;
                
            default:
                player.sendMessage(new StringTextComponent("Unknown command: " + command), UUID.randomUUID());
                displayHelp(player);
                return 0;
        }
        
        // Send network message to update clients
        NetworkHandler.sendToAllTracking(
                new CommandMessage(companion.getId(), command, pos, item),
                companion);
        
        return 1;
    }
    
    private static void displayHelp(ServerPlayerEntity player) {
        player.sendMessage(new StringTextComponent("=== AI Companion Commands ==="), UUID.randomUUID());
        player.sendMessage(new StringTextComponent("/aicompanion follow - Make companion follow you"), UUID.randomUUID());
        player.sendMessage(new StringTextComponent("/aicompanion stay - Make companion stay in place"), UUID.randomUUID());
        player.sendMessage(new StringTextComponent("/aicompanion move <x> <y> <z> - Send companion to position"), UUID.randomUUID());
        player.sendMessage(new StringTextComponent("/aicompanion break <x> <y> <z> - Make companion break block at position"), UUID.randomUUID());
        player.sendMessage(new StringTextComponent("/aicompanion place <x> <y> <z> [item] - Make companion place item at position"), UUID.randomUUID());
        player.sendMessage(new StringTextComponent("/aicompanion list - List all your companions"), UUID.randomUUID());
    }
    
    private static void listCompanions(ServerPlayerEntity player) {
        List<AICompanionEntity> companions = player.level.getEntitiesOfClass(
                AICompanionEntity.class, 
                player.getBoundingBox().inflate(100.0D),
                entity -> entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID())
        );
        
        if (companions.isEmpty()) {
            player.sendMessage(new StringTextComponent("You don't have any AI companions"), UUID.randomUUID());
            return;
        }
        
        player.sendMessage(new StringTextComponent("=== Your AI Companions ==="), UUID.randomUUID());
        for (int i = 0; i < companions.size(); i++) {
            AICompanionEntity companion = companions.get(i);
            BlockPos pos = companion.blockPosition();
            double distance = Math.sqrt(companion.distanceToSqr(player));
            
            player.sendMessage(new StringTextComponent(
                    String.format("#%d: %s at [%d, %d, %d] (%.1f blocks away)",
                            i + 1,
                            companion.isActive() ? "Active" : "Inactive",
                            pos.getX(), pos.getY(), pos.getZ(),
                            distance)), UUID.randomUUID());
        }
    }
}
