package com.aicompanion.mod.network.message;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.entity.AICompanionEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Network message for companion management actions (teleport, rename, remove)
 */
public class CompanionManagementMessage {
    
    public enum Action {
        TELEPORT,
        RENAME,
        REMOVE
    }
    
    private final Action action;
    private final String companionUuid;
    private final String extraData; // Used for rename (new name)
    
    public CompanionManagementMessage(Action action, String companionUuid, String extraData) {
        this.action = action;
        this.companionUuid = companionUuid;
        this.extraData = extraData;
    }
    
    public static void encode(CompanionManagementMessage message, PacketBuffer buffer) {
        buffer.writeEnum(message.action);
        buffer.writeUtf(message.companionUuid);
        buffer.writeBoolean(message.extraData != null);
        if (message.extraData != null) {
            buffer.writeUtf(message.extraData);
        }
    }
    
    public static CompanionManagementMessage decode(PacketBuffer buffer) {
        Action action = buffer.readEnum(Action.class);
        String companionUuid = buffer.readUtf(36); // UUID string length
        String extraData = null;
        if (buffer.readBoolean()) {
            extraData = buffer.readUtf(32);
        }
        
        return new CompanionManagementMessage(action, companionUuid, extraData);
    }
    
    public static void handle(CompanionManagementMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
                                // Process the requested action
                                switch (message.action) {
                                    case TELEPORT:
                                        handleTeleport(companion, player);
                                        break;
                                    case RENAME:
                                        handleRename(companion, message.extraData);
                                        break;
                                    case REMOVE:
                                        handleRemove(companion);
                                        break;
                                }
                            } else {
                                AICompanionMod.LOGGER.warn("Player {} attempted to manage a companion they don't own", 
                                        player.getName().getString());
                            }
                            break;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    AICompanionMod.LOGGER.error("Invalid UUID format in management message: {}", message.companionUuid);
                }
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * Handle teleport action
     */
    private static void handleTeleport(AICompanionEntity companion, ServerPlayerEntity player) {
        BlockPos playerPos = player.blockPosition();
        
        // Find a safe position around the player
        for (int i = 0; i < 10; i++) {
            int offsetX = (i % 3) - 1;
            int offsetZ = (i / 3) - 1;
            BlockPos targetPos = playerPos.offset(offsetX, 0, offsetZ);
            
            // Teleport companion to player
            companion.teleportTo(
                    targetPos.getX() + 0.5, 
                    targetPos.getY(), 
                    targetPos.getZ() + 0.5);
            break;
        }
    }
    
    /**
     * Handle rename action
     */
    private static void handleRename(AICompanionEntity companion, String newName) {
        if (newName != null && !newName.isEmpty()) {
            companion.setCustomName(new net.minecraft.util.text.StringTextComponent(newName));
        }
    }
    
    /**
     * Handle remove action
     */
    private static void handleRemove(AICompanionEntity companion) {
        companion.remove();
    }
}