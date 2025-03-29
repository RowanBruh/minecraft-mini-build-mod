package com.aicompanion.mod.config;

import com.aicompanion.mod.AICompanionMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration for AI Companion Mod
 */
@Mod.EventBusSubscriber(modid = AICompanionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AICompanionConfig {
    
    public static class Client {
        // Client-side settings
        
        // Visual settings
        public final ForgeConfigSpec.BooleanValue showNameTags;
        public final ForgeConfigSpec.BooleanValue showStatusOverlay;
        public final ForgeConfigSpec.IntValue maxRenderDistance;
        
        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Client-side settings for AI Companions")
                   .push("client");
            
            showNameTags = builder
                    .comment("Show companion name tags")
                    .define("showNameTags", true);
            
            showStatusOverlay = builder
                    .comment("Show status overlay for companions")
                    .define("showStatusOverlay", true);
            
            maxRenderDistance = builder
                    .comment("Maximum distance to render companions (blocks)")
                    .defineInRange("maxRenderDistance", 32, 8, 64);
            
            builder.pop();
        }
    }
    
    public static class Server {
        // Server-side settings
        
        // Behavior settings
        public final ForgeConfigSpec.BooleanValue allowBreakingBlocks;
        public final ForgeConfigSpec.BooleanValue allowPlacingBlocks;
        public final ForgeConfigSpec.BooleanValue requiresFood;
        
        // Companion limits
        public final ForgeConfigSpec.IntValue maxCompanionsPerPlayer;
        
        // Attributes
        public final ForgeConfigSpec.DoubleValue movementSpeed;
        public final ForgeConfigSpec.DoubleValue healthAmount;
        public final ForgeConfigSpec.IntValue teleportDistance;
        
        // Web Interface settings
        public final ForgeConfigSpec.BooleanValue enableWebInterface;
        public final ForgeConfigSpec.IntValue webInterfacePort;
        public final ForgeConfigSpec.StringValue webInterfaceUsername;
        public final ForgeConfigSpec.StringValue webInterfacePassword;
        public final ForgeConfigSpec.StringValue webInterfaceSecretKey;
        
        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Server-side settings for AI Companions")
                   .push("server");
            
            // Behavior settings
            allowBreakingBlocks = builder
                    .comment("Allow companions to break blocks")
                    .define("allowBreakingBlocks", true);
            
            allowPlacingBlocks = builder
                    .comment("Allow companions to place blocks")
                    .define("allowPlacingBlocks", true);
            
            requiresFood = builder
                    .comment("Companions require food to function")
                    .define("requiresFood", true);
            
            // Limits
            maxCompanionsPerPlayer = builder
                    .comment("Maximum number of companions per player")
                    .defineInRange("maxCompanionsPerPlayer", 3, 1, 10);
            
            // Attributes
            movementSpeed = builder
                    .comment("Companion movement speed")
                    .defineInRange("movementSpeed", 0.3D, 0.1D, 0.5D);
            
            healthAmount = builder
                    .comment("Companion health points")
                    .defineInRange("healthAmount", 20.0D, 10.0D, 50.0D);
            
            teleportDistance = builder
                    .comment("Distance at which companions teleport to owner (blocks)")
                    .defineInRange("teleportDistance", 12, 6, 24);
            
            // Web interface settings
            builder.comment("Web Interface Settings")
                   .push("webInterface");
            
            enableWebInterface = builder
                    .comment("Enable the web interface for remote AI control")
                    .define("enableWebInterface", false);
            
            webInterfacePort = builder
                    .comment("Port for the web interface (requires restart)")
                    .defineInRange("webInterfacePort", 8080, 1024, 65535);
            
            webInterfaceUsername = builder
                    .comment("Username for web interface authentication")
                    .define("webInterfaceUsername", "admin");
            
            webInterfacePassword = builder
                    .comment("Password for web interface authentication")
                    .define("webInterfacePassword", "password");
                    
            webInterfaceSecretKey = builder
                    .comment("Secret key for JWT token generation (will be randomly generated if empty)")
                    .define("webInterfaceSecretKey", "");
            
            builder.pop(); // webInterface
            builder.pop(); // server
        }
    }
    
    // Create the config specifications
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;
    
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;
    
    static {
        // Build client spec
        final Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder()
                .configure(Client::new);
        CLIENT_SPEC = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();
        
        // Build server spec
        final Pair<Server, ForgeConfigSpec> serverSpecPair = new ForgeConfigSpec.Builder()
                .configure(Server::new);
        SERVER_SPEC = serverSpecPair.getRight();
        SERVER = serverSpecPair.getLeft();
    }
    
    /**
     * Handle configuration loading or reloading
     */
    @SubscribeEvent
    public static void onConfigLoading(final ModConfig.ModConfigEvent event) {
        final ModConfig config = event.getConfig();
        
        // Make sure it's our config
        if (config.getSpec() == CLIENT_SPEC || config.getSpec() == SERVER_SPEC) {
            AICompanionMod.LOGGER.info("Loading AI Companion configuration");
            
            // Could refresh any internal states here that depend on config
        }
    }
}