package com.aicompanion.mod.client.key;

import com.aicompanion.mod.AICompanionMod;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

/**
 * Manages all keybindings for the mod
 */
@OnlyIn(Dist.CLIENT)
public class KeyBindings {
    
    private static final String CATEGORY = "key.categories." + AICompanionMod.MOD_ID;
    
    public static KeyBinding openAdminPanel;
    public static KeyBinding quickCommand1;
    public static KeyBinding quickCommand2;
    public static KeyBinding quickCommand3;
    
    /**
     * Initialize all keybindings
     */
    public static void init() {
        // Admin panel key (default: P)
        openAdminPanel = registerKey("open_admin_panel", GLFW.GLFW_KEY_P);
        
        // Quick command keys
        quickCommand1 = registerKey("quick_command_1", GLFW.GLFW_KEY_Z);
        quickCommand2 = registerKey("quick_command_2", GLFW.GLFW_KEY_X);
        quickCommand3 = registerKey("quick_command_3", GLFW.GLFW_KEY_C);
    }
    
    /**
     * Registers a keybinding
     */
    private static KeyBinding registerKey(String name, int keyCode) {
        final KeyBinding key = new KeyBinding(
                "key." + AICompanionMod.MOD_ID + "." + name,
                InputMappings.Type.KEYSYM, 
                keyCode, 
                CATEGORY
        );
        ClientRegistry.registerKeyBinding(key);
        return key;
    }
}