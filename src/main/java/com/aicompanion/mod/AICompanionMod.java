package com.aicompanion.mod;

import com.aicompanion.mod.client.gui.AdminPanelScreen;
import com.aicompanion.mod.client.key.KeyBindings;
import com.aicompanion.mod.command.AICompanionCommand;
import com.aicompanion.mod.config.AICompanionConfig;
import com.aicompanion.mod.init.ModEntities;
import com.aicompanion.mod.network.NetworkHandler;
import com.aicompanion.mod.web.WebServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AICompanionMod.MOD_ID)
public class AICompanionMod {
    public static final String MOD_ID = "aicompanion";
    public static final Logger LOGGER = LogManager.getLogger();
    
    // Reference to the Minecraft server instance, set when server starts
    public static net.minecraft.server.MinecraftServer SERVER;

    public AICompanionMod() {
        LOGGER.info("Initializing AI Companion Mod");
        
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register mod components
        ModEntities.register(modEventBus);
        
        // Register setup methods
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::clientSetup);
        
        // Register configuration
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AICompanionConfig.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AICompanionConfig.SERVER_SPEC);
        
        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Initialize network handler
        NetworkHandler.init();
        
        // Setup entity attributes
        event.enqueueWork(() -> {
            ModEntities.registerEntityAttributes();
        });
    }
    
    /**
     * Restart the web server if it's running to apply new settings
     */
    public static void restartWebServer() {
        if (AICompanionConfig.SERVER.enableWebInterface.get()) {
            LOGGER.info("Restarting web interface server with new settings");
            WebServer.getInstance().stop();
            WebServer.getInstance().start();
        } else {
            LOGGER.info("Stopping web interface server");
            WebServer.getInstance().stop();
        }
    }
    
    /**
     * Execute a command on an entity with the given ID
     * 
     * @param entityId The entity ID as a string
     * @param command The command to execute
     * @param args Optional arguments encoded in a BlockPos
     * @return true if command was executed successfully, false otherwise
     */
    public static boolean executeEntityCommand(String entityId, String command, net.minecraft.util.math.BlockPos args) {
        if (SERVER == null) {
            LOGGER.error("Cannot execute entity command: Server is not running");
            return false;
        }
        
        try {
            // Parse the entity ID to a UUID if possible
            java.util.UUID uuid = null;
            try {
                uuid = java.util.UUID.fromString(entityId);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid entity UUID format: " + entityId);
                // Continue with numeric ID approach
            }
            
            // Search for the entity in all worlds
            net.minecraft.entity.Entity targetEntity = null;
            
            // Try by UUID first if we have one
            if (uuid != null) {
                for (net.minecraft.world.server.ServerWorld world : SERVER.getAllLevels()) {
                    targetEntity = world.getEntity(uuid);
                    if (targetEntity != null) break;
                }
            }
            
            // If not found by UUID, try by numeric ID
            if (targetEntity == null) {
                try {
                    int numericId = Integer.parseInt(entityId);
                    for (net.minecraft.world.server.ServerWorld world : SERVER.getAllLevels()) {
                        for (net.minecraft.entity.Entity entity : world.getAllEntities()) {
                            if (entity.getId() == numericId && 
                                entity instanceof com.aicompanion.mod.entity.AICompanionEntity) {
                                targetEntity = entity;
                                break;
                            }
                        }
                        if (targetEntity != null) break;
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Entity ID is not a valid UUID or numeric ID: " + entityId);
                    return false;
                }
            }
            
            // Handle AI Companion entity commands
            if (targetEntity instanceof com.aicompanion.mod.entity.AICompanionEntity) {
                com.aicompanion.mod.entity.AICompanionEntity companion = 
                    (com.aicompanion.mod.entity.AICompanionEntity) targetEntity;
                
                // Handle different commands
                if ("skin".equals(command)) {
                    int skinType = args.getX();
                    int skinPath = args.getY();
                    
                    // Convert the character codes back to strings
                    String skinTypeStr = String.valueOf((char) skinType);
                    String skinPathStr = skinPath > 0 ? String.valueOf((char) skinPath) : "";
                    
                    LOGGER.info("Setting skin for companion " + entityId + 
                               ": type=" + skinTypeStr + ", path=" + skinPathStr);
                    
                    // Call the appropriate method on the companion entity
                    companion.setSkinType(skinTypeStr);
                    if (!skinPathStr.isEmpty()) {
                        companion.setSkinPath(skinPathStr);
                    }
                    
                    return true;
                } else {
                    LOGGER.warn("Unknown command for AI Companion: " + command);
                    return false;
                }
            } 
            // Handle Mini Build entity commands
            else if (targetEntity instanceof com.aicompanion.mod.entity.MiniBuildEntity) {
                com.aicompanion.mod.entity.MiniBuildEntity miniBuild = 
                    (com.aicompanion.mod.entity.MiniBuildEntity) targetEntity;
                
                // Toggle wall visibility
                if ("toggle_walls".equals(command)) {
                    boolean newState = !miniBuild.isWallsVisible();
                    miniBuild.setWallsVisible(newState);
                    LOGGER.info("Toggled walls visibility for mini build " + entityId + ": " + newState);
                    return true;
                } 
                // Toggle giant player visibility
                else if ("toggle_giant_player".equals(command)) {
                    boolean newState = !miniBuild.isGiantPlayerVisible();
                    miniBuild.setGiantPlayerVisible(newState);
                    LOGGER.info("Toggled giant player visibility for mini build " + entityId + ": " + newState);
                    return true;
                }
                // Update a block in the mini build
                else if ("update_block".equals(command)) {
                    int x = args.getX();
                    int y = args.getY();
                    int z = args.getZ();
                    
                    // This is a simplified implementation
                    // In a real implementation, you would need to get the block state as well
                    LOGGER.info("Update block request for mini build " + entityId + 
                               " at position: " + x + ", " + y + ", " + z);
                    
                    // TODO: Implement block update logic
                    return true;
                } else {
                    LOGGER.warn("Unknown command for Mini Build: " + command);
                    return false;
                }
            } else {
                LOGGER.error("Entity is not a known mod entity: " + entityId);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error executing entity command", e);
            return false;
        }
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // Register entity renderers
        ModEntities.registerRenderers();
        
        // Initialize keybindings
        KeyBindings.init();
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            AICompanionCommand.register(event.getDispatcher());
        }
        
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            // Store reference to the server
            SERVER = event.getServer();
            
            // Start web server if enabled in config
            if (AICompanionConfig.SERVER.enableWebInterface.get()) {
                LOGGER.info("Starting web interface server");
                WebServer.getInstance().start();
            }
        }
        
        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            // Stop web server if it's running
            LOGGER.info("Stopping web interface server");
            WebServer.getInstance().stop();
        }
    }
    
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.KeyInputEvent event) {
            handleKeyInputs();
        }
        
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                handleKeyInputs();
            }
        }
        
        @OnlyIn(Dist.CLIENT)
        private static void handleKeyInputs() {
            Minecraft minecraft = Minecraft.getInstance();
            
            if (minecraft.player != null && minecraft.screen == null) {
                if (KeyBindings.openAdminPanel.consumeClick()) {
                    // Open the admin panel when the key is pressed
                    minecraft.setScreen(new AdminPanelScreen());
                }
            }
        }
    }
}
