package com.aicompanion.mod.network;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.network.message.AdminPanelMessage;
import com.aicompanion.mod.network.message.CommandMessage;
import com.aicompanion.mod.network.message.CompanionManagementMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AICompanionMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int id = 0;
    
    public static void init() {
        // Register messages
        INSTANCE.registerMessage(
                id++,
                CommandMessage.class,
                CommandMessage::encode,
                CommandMessage::decode,
                CommandMessage::handle
        );
        
        INSTANCE.registerMessage(
                id++,
                AdminPanelMessage.class,
                AdminPanelMessage::encode,
                AdminPanelMessage::decode,
                AdminPanelMessage::handle
        );
        
        INSTANCE.registerMessage(
                id++,
                CompanionManagementMessage.class,
                CompanionManagementMessage::encode,
                CompanionManagementMessage::decode,
                CompanionManagementMessage::handle
        );
    }
    
    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }
    
    public static void sendToPlayer(Object message, ServerPlayerEntity player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    
    public static void sendToAllTracking(Object message, Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }
    
    public static void sendToAll(Object message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
