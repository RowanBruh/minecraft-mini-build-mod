package com.aicompanion.mod.network.message;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.config.AICompanionConfig;
import com.aicompanion.mod.client.gui.CompanionRenameScreen;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network message for admin panel settings
 */
public class AdminPanelMessage {
    
    private final String settingKey;
    private final String settingValue;
    
    public AdminPanelMessage(String settingKey, String settingValue) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }
    
    public static void encode(AdminPanelMessage message, PacketBuffer buffer) {
        buffer.writeUtf(message.settingKey);
        buffer.writeUtf(message.settingValue);
    }
    
    public static AdminPanelMessage decode(PacketBuffer buffer) {
        String settingKey = buffer.readUtf(100);
        String settingValue = buffer.readUtf(100);
        
        return new AdminPanelMessage(settingKey, settingValue);
    }
    
    public static void handle(AdminPanelMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // We are on the server side here
            ServerPlayerEntity player = context.getSender();
            
            if (player != null && player.hasPermissions(2)) { // Only ops can change server settings
                applySetting(message.settingKey, message.settingValue);
            } else {
                AICompanionMod.LOGGER.warn("Player {} attempted to change server settings without permission",
                        player != null ? player.getName().getString() : "Unknown");
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * Apply a setting change
     */
    private static void applySetting(String key, String value) {
        AICompanionMod.LOGGER.info("Changing setting: {} to {}", key, value);
        
        // Handle boolean settings
        if (key.equals("options.aicompanion.allow_breaking_blocks")) {
            AICompanionConfig.SERVER.allowBreakingBlocks.set(Boolean.parseBoolean(value));
        }
        else if (key.equals("options.aicompanion.allow_placing_blocks")) {
            AICompanionConfig.SERVER.allowPlacingBlocks.set(Boolean.parseBoolean(value));
        }
        else if (key.equals("options.aicompanion.requires_food")) {
            AICompanionConfig.SERVER.requiresFood.set(Boolean.parseBoolean(value));
        }
        
        // Handle integer settings
        else if (key.equals("options.aicompanion.max_companions_per_player")) {
            try {
                int intValue = Integer.parseInt(value);
                AICompanionConfig.SERVER.maxCompanionsPerPlayer.set(intValue);
            } catch (NumberFormatException e) {
                AICompanionMod.LOGGER.error("Invalid integer value for setting {}: {}", key, value);
            }
        }
        else if (key.equals("options.aicompanion.teleport_distance")) {
            try {
                int intValue = Integer.parseInt(value);
                AICompanionConfig.SERVER.teleportDistance.set(intValue);
            } catch (NumberFormatException e) {
                AICompanionMod.LOGGER.error("Invalid integer value for setting {}: {}", key, value);
            }
        }
        
        // Handle double settings
        else if (key.equals("options.aicompanion.movement_speed")) {
            try {
                double doubleValue = Double.parseDouble(value);
                AICompanionConfig.SERVER.movementSpeed.set(doubleValue);
            } catch (NumberFormatException e) {
                AICompanionMod.LOGGER.error("Invalid double value for setting {}: {}", key, value);
            }
        }
        else if (key.equals("options.aicompanion.health_amount")) {
            try {
                double doubleValue = Double.parseDouble(value);
                AICompanionConfig.SERVER.healthAmount.set(doubleValue);
            } catch (NumberFormatException e) {
                AICompanionMod.LOGGER.error("Invalid double value for setting {}: {}", key, value);
            }
        }
        
        // Web interface settings
        else if (key.equals("options.aicompanion.enable_web_interface")) {
            boolean enableWebInterface = Boolean.parseBoolean(value);
            AICompanionConfig.SERVER.enableWebInterface.set(enableWebInterface);
            
            // Restart web server if needed
            AICompanionMod.restartWebServer();
        }
        else if (key.equals("options.aicompanion.web_interface_port")) {
            try {
                int intValue = Integer.parseInt(value);
                AICompanionConfig.SERVER.webInterfacePort.set(intValue);
                
                // Restart web server if it's enabled
                if (AICompanionConfig.SERVER.enableWebInterface.get()) {
                    AICompanionMod.restartWebServer();
                }
            } catch (NumberFormatException e) {
                AICompanionMod.LOGGER.error("Invalid integer value for setting {}: {}", key, value);
            }
        }
        else if (key.equals("command.aicompanion.web_username")) {
            if (value.equals("open")) {
                // TODO: Open text input screen for username
            } else {
                AICompanionConfig.SERVER.webInterfaceUsername.set(value);
            }
        }
        else if (key.equals("command.aicompanion.web_password")) {
            if (value.equals("open")) {
                // TODO: Open text input screen for password
            } else {
                AICompanionConfig.SERVER.webInterfacePassword.set(value);
            }
        }
        
        // Unknown setting
        else {
            AICompanionMod.LOGGER.warn("Unknown setting key: {}", key);
        }
    }
}