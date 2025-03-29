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
