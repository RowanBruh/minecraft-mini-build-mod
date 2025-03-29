package com.aicompanion.mod.network.message;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.entity.AICompanionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CommandMessage {
    private final String companionUuid;
    private final String command;
    private final BlockPos targetPos;
    private final ItemStack item;
    
    public CommandMessage(String companionUuid, String command) {
        this(companionUuid, command, null, ItemStack.EMPTY);
    }
    
    public CommandMessage(String companionUuid, String command, BlockPos targetPos, ItemStack item) {
        this.companionUuid = companionUuid;
        this.command = command;
        this.targetPos = targetPos;
        this.item = item != null ? item : ItemStack.EMPTY;
    }
    
    public static void encode(CommandMessage message, PacketBuffer buffer) {
        buffer.writeUtf(message.companionUuid);
        buffer.writeUtf(message.command);
        buffer.writeBoolean(message.targetPos != null);
        if (message.targetPos != null) {
            buffer.writeBlockPos(message.targetPos);
        }
        buffer.writeItemStack(message.item);
    }
    
    public static CommandMessage decode(PacketBuffer buffer) {
        String companionUuid = buffer.readUtf(36); // UUID string length
        String command = buffer.readUtf(100);
        BlockPos targetPos = null;
        if (buffer.readBoolean()) {
            targetPos = buffer.readBlockPos();
        }
        ItemStack item = buffer.readItem();
        
        return new CommandMessage(companionUuid, command, targetPos, item);
    }
    
    public static void handle(CommandMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // We are on the server side here
            ServerPlayerEntity player = context.getSender();
            
            if (player != null) {
                ServerWorld world = player.getLevel();
                
                try {
                    UUID uuid = UUID.fromString(message.companionUuid);
                    
                    // Find the companion entity by UUID
                    for (Entity entity : world.getAllEntities()) {
                        if (entity instanceof AICompanionEntity && entity.getUUID().equals(uuid)) {
                            AICompanionEntity companion = (AICompanionEntity) entity;
                            
                            // Verify ownership
                            UUID ownerUuid = companion.getOwnerUUID();
                            if (ownerUuid != null && ownerUuid.equals(player.getUUID())) {
                                companion.processCommand(message.command, message.targetPos, message.item);
                            } else {
                                AICompanionMod.LOGGER.warn("Player {} attempted to command a companion they don't own", 
                                        player.getName().getString());
                            }
                            break;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    AICompanionMod.LOGGER.error("Invalid UUID format in command: {}", message.companionUuid);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
