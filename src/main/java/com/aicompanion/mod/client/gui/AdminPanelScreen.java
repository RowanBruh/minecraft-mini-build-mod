package com.aicompanion.mod.client.gui;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.config.AICompanionConfig;
import com.aicompanion.mod.network.NetworkHandler;
import com.aicompanion.mod.network.message.AdminPanelMessage;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Main admin panel screen for the mod
 */
@OnlyIn(Dist.CLIENT)
public class AdminPanelScreen extends Screen {
    
    private static final ResourceLocation BACKGROUND = new ResourceLocation(AICompanionMod.MOD_ID, "textures/gui/admin_panel.png");
    private static final int BACKGROUND_WIDTH = 248;
    private static final int BACKGROUND_HEIGHT = 200;
    
    private Button companionsButton;
    private Button clientSettingsButton;
    private Button serverSettingsButton;
    private Button webInterfaceButton;
    private Button doneButton;
    
    private enum Tab {
        COMPANIONS,
        CLIENT_SETTINGS,
        SERVER_SETTINGS,
        WEB_INTERFACE
    }
    
    private Tab currentTab = Tab.COMPANIONS;
    private OptionsRowList optionsRowList;
    private List<Widget> tabWidgets = new ArrayList<>();
    private List<OptionsRowList> tabOptionLists = new ArrayList<>();
    
    /**
     * Constructor
     */
    public AdminPanelScreen() {
        super(new TranslationTextComponent("screen.aicompanion.admin_panel"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Calculate centered position
        int guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        int guiTop = (this.height - BACKGROUND_HEIGHT) / 2;
        
        // Create tab buttons
        this.companionsButton = new Button(guiLeft + 10, guiTop + 30, 70, 20, 
                new StringTextComponent("Companions"), button -> this.setTab(Tab.COMPANIONS));
        
        this.clientSettingsButton = new Button(guiLeft + 85, guiTop + 30, 55, 20, 
                new StringTextComponent("Client"), button -> this.setTab(Tab.CLIENT_SETTINGS));
        
        this.serverSettingsButton = new Button(guiLeft + 145, guiTop + 30, 55, 20, 
                new StringTextComponent("Server"), button -> this.setTab(Tab.SERVER_SETTINGS));
                
        this.webInterfaceButton = new Button(guiLeft + 205, guiTop + 30, 33, 20,
                new StringTextComponent("Web"), button -> this.setTab(Tab.WEB_INTERFACE));
        
        // Create done button
        this.doneButton = new Button(guiLeft + (BACKGROUND_WIDTH - 100) / 2, guiTop + 170, 100, 20, 
                new StringTextComponent("Done"), button -> this.onClose());
        
        // Add all fixed buttons
        this.addButton(this.companionsButton);
        this.addButton(this.clientSettingsButton);
        this.addButton(this.serverSettingsButton);
        this.addButton(this.webInterfaceButton);
        this.addButton(this.doneButton);
        
        // Initialize current tab
        setTab(Tab.COMPANIONS);
    }
    
    /**
     * Set the current tab and initialize its contents
     */
    private void setTab(Tab tab) {
        this.currentTab = tab;
        
        // Remove previous tab widgets and options lists
        this.children.removeAll(tabWidgets);
        for (OptionsRowList list : tabOptionLists) {
            this.children.remove(list);
        }
        tabWidgets.clear();
        tabOptionLists.clear();
        
        // Create widgets for the selected tab
        switch (tab) {
            case COMPANIONS:
                initCompanionsTab();
                break;
            case CLIENT_SETTINGS:
                initClientSettingsTab();
                break;
            case SERVER_SETTINGS:
                initServerSettingsTab();
                break;
            case WEB_INTERFACE:
                initWebInterfaceTab();
                break;
        }
        
        // Update button states
        this.companionsButton.active = tab != Tab.COMPANIONS;
        this.clientSettingsButton.active = tab != Tab.CLIENT_SETTINGS;
        this.serverSettingsButton.active = tab != Tab.SERVER_SETTINGS;
        this.webInterfaceButton.active = tab != Tab.WEB_INTERFACE;
    }
    
    /**
     * Initialize companions management tab
     */
    private void initCompanionsTab() {
        int guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        int guiTop = (this.height - BACKGROUND_HEIGHT) / 2;
        
        // Create manage companions button
        Button manageButton = new Button(guiLeft + 20, guiTop + 70, 208, 20, 
                new TranslationTextComponent("screen.aicompanion.manage_companions"), 
                button -> this.minecraft.setScreen(new CompanionManagementScreen(this)));
        
        // Add button to tab widgets and children
        tabWidgets.add(manageButton);
        this.addButton(manageButton);
    }
    
    /**
     * Initialize client settings tab
     */
    private void initClientSettingsTab() {
        int guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        int guiTop = (this.height - BACKGROUND_HEIGHT) / 2;
        
        // Create options list
        this.optionsRowList = new OptionsRowList(
                this.minecraft, 
                BACKGROUND_WIDTH - 40, 
                110, 
                guiTop + 60, 
                guiTop + 170, 
                25);
        
        // Add boolean options
        addBooleanOption(
                "options.aicompanion.show_name_tags",
                AICompanionConfig.CLIENT.showNameTags);
        
        addBooleanOption(
                "options.aicompanion.show_status_overlay",
                AICompanionConfig.CLIENT.showStatusOverlay);
        
        // Add slider options
        addIntSliderOption(
                "options.aicompanion.max_render_distance",
                AICompanionConfig.CLIENT.maxRenderDistance,
                8, 64);
        
        // Position the list
        // Position is handled by the widget system in 1.16.5
        
        // Add to tab options lists and children
        tabOptionLists.add(optionsRowList);
        this.children.add(optionsRowList);
        
    }
    
    /**
     * Initialize web interface tab
     */
    private void initWebInterfaceTab() {
        int guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        int guiTop = (this.height - BACKGROUND_HEIGHT) / 2;
        
        // Create options list
        this.optionsRowList = new OptionsRowList(
                this.minecraft, 
                BACKGROUND_WIDTH - 40, 
                110, 
                guiTop + 60, 
                guiTop + 170, 
                25);
        
        // Add enable/disable web interface option
        addServerBooleanOption(
                "options.aicompanion.enable_web_interface",
                AICompanionConfig.SERVER.enableWebInterface);
        
        // Add web interface port option
        addServerIntSliderOption(
                "options.aicompanion.web_interface_port",
                AICompanionConfig.SERVER.webInterfacePort,
                1024, 65535);
        
        // Add username text field
        Button usernameButton = new Button(guiLeft + 20, guiTop + 110, 208, 20,
                new StringTextComponent("Username: " + AICompanionConfig.SERVER.webInterfaceUsername.get()),
                button -> {
                    // Send message to server to open text input screen for username
                    NetworkHandler.sendToServer(new AdminPanelMessage(
                            "command.aicompanion.web_username",
                            "open"));
                });
        
        // Add password text field
        Button passwordButton = new Button(guiLeft + 20, guiTop + 135, 208, 20,
                new StringTextComponent("Password: Change Password"),
                button -> {
                    // Send message to server to open text input screen for password
                    NetworkHandler.sendToServer(new AdminPanelMessage(
                            "command.aicompanion.web_password",
                            "open"));
                });
        
        // Note: Web interface URL is rendered in the render method
        
        // Position the list and add additional buttons
        // Position is handled by the widget system in 1.16.5
        
        // Add to tab options lists and tab widgets
        tabOptionLists.add(optionsRowList);
        tabWidgets.add(usernameButton);
        tabWidgets.add(passwordButton);
        
        this.children.add(optionsRowList);
        
        this.addButton(usernameButton);
        this.addButton(passwordButton);
    }
    
    /**
     * Initialize server settings tab
     */
    private void initServerSettingsTab() {
        int guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        int guiTop = (this.height - BACKGROUND_HEIGHT) / 2;
        
        // Create options list
        this.optionsRowList = new OptionsRowList(
                this.minecraft, 
                BACKGROUND_WIDTH - 40, 
                110, 
                guiTop + 60, 
                guiTop + 170, 
                25);
        
        // Add boolean options
        addServerBooleanOption(
                "options.aicompanion.allow_breaking_blocks",
                AICompanionConfig.SERVER.allowBreakingBlocks);
        
        addServerBooleanOption(
                "options.aicompanion.allow_placing_blocks",
                AICompanionConfig.SERVER.allowPlacingBlocks);
        
        addServerBooleanOption(
                "options.aicompanion.requires_food",
                AICompanionConfig.SERVER.requiresFood);
        
        // Add slider options
        addServerIntSliderOption(
                "options.aicompanion.max_companions_per_player",
                AICompanionConfig.SERVER.maxCompanionsPerPlayer,
                1, 10);
        
        addServerDoubleSliderOption(
                "options.aicompanion.movement_speed",
                AICompanionConfig.SERVER.movementSpeed,
                0.1D, 0.5D);
        
        addServerDoubleSliderOption(
                "options.aicompanion.health_amount",
                AICompanionConfig.SERVER.healthAmount,
                10.0D, 50.0D);
        
        addServerIntSliderOption(
                "options.aicompanion.teleport_distance",
                AICompanionConfig.SERVER.teleportDistance,
                6, 24);
        
        // Position the list
        // Position is handled by the widget system in 1.16.5
        
        // Add to tab options lists and children
        tabOptionLists.add(optionsRowList);
        this.children.add(optionsRowList);
        
    }
    
    /**
     * Helper to add a boolean option (checkbox) to the options list
     */
    private void addBooleanOption(String translationKey, ForgeConfigSpec.BooleanValue configValue) {
        boolean initialValue = configValue.get();
        
        // Create a button that acts like a checkbox
        Button checkbox = new Button(0, 0, 150, 20, 
                new StringTextComponent(I18n.get(translationKey) + ": " + (initialValue ? "On" : "Off")),
                button -> {
                    // Parse current state from button text
                    boolean currentState = button.getMessage().getString().endsWith("On");
                    boolean newValue = !currentState;
                    
                    // Set the config value
                    configValue.set(newValue);
                    
                    // Update button text with new state
                    button.setMessage(new StringTextComponent(I18n.get(translationKey) + ": " + (newValue ? "On" : "Off")));
                }
        );
        
        // Add widget to screen and options list
        this.addWidget(checkbox);
        addWidgetToOptionsRow(checkbox, false);
    }
    
    /**
     * Helper to add an integer slider option
     */
    private void addIntSliderOption(String translationKey, ForgeConfigSpec.IntValue configValue, 
                                   int min, int max) {
        
        SliderPercentageOption slider = new SliderPercentageOption(
                translationKey,
                min, max, 1f,
                gameSettings -> (double) configValue.get(),
                (gameSettings, value) -> configValue.set(value.intValue()),
                (gameSettings, option) -> new StringTextComponent(
                        I18n.get(translationKey) + ": " + configValue.get()));
        
        // Using a fixed Widget instance instead of createButton for Forge compatibility
        Widget sliderWidget = slider.createButton(this.minecraft.options, this.width / 2 - 155, 0, 150);
        this.addWidget(sliderWidget);
        addWidgetToOptionsRow(sliderWidget, true);
    }
    
    /**
     * Helper to add a server-side boolean option (checkbox)
     */
    private void addServerBooleanOption(String translationKey, ForgeConfigSpec.BooleanValue configValue) {
        boolean initialValue = configValue.get();
        
        // Create a button that acts like a checkbox
        Button checkbox = new Button(0, 0, 150, 20, 
                new StringTextComponent(I18n.get(translationKey) + ": " + (initialValue ? "On" : "Off")),
                button -> {
                    // Parse current state from button text
                    boolean currentState = button.getMessage().getString().endsWith("On");
                    boolean newValue = !currentState;
                    
                    // Update button text with new state
                    button.setMessage(new StringTextComponent(I18n.get(translationKey) + ": " + (newValue ? "On" : "Off")));
                    
                    // Send update to server
                    NetworkHandler.sendToServer(new AdminPanelMessage(
                            translationKey,
                            String.valueOf(newValue)));
                }
        );
        
        // Add the widget to our UI
        this.addWidget(checkbox);
        
        // Add the widget to a row in the options list
        addWidgetToOptionsRow(checkbox, false);
    }
    
    /**
     * Helper method to add a widget to the options row list
     * This uses a workaround for the incompatible types issue
     */
    private void addWidgetToOptionsRow(Widget widget, boolean wide) {
        // Create a list of widgets for the row
        List<Widget> widgetList = new ArrayList<>();
        widgetList.add(widget);
        
        // If not a wide widget, add a dummy widget for the second column
        if (!wide) {
            Button dummyButton = new Button(0, 0, 0, 0, StringTextComponent.EMPTY, (b) -> {});
            widgetList.add(dummyButton);
            this.addWidget(dummyButton);
        }
        
        // Create a new row and add it to the options list manually
        // Since we can't directly add widgets in 1.16.5, we'll need to update our list separately
        if (wide) {
            // We can't use addBig with a Widget directly since it requires an AbstractOption
            // Just add to children collection which should handle the input
            this.children.add(widgetList.get(0));
        } else {
            // For 1.16.5 we need SliderOption or BooleanOption, not Widgets
            // We've already added these widgets manually above, so it's safe to skip
            // Just add the widgets to the list without registering them as options
            this.children.add(widgetList.get(0));
            this.children.add(widgetList.get(1));
        }
    }
    
    /**
     * Helper to add a server-side integer slider option
     */
    private void addServerIntSliderOption(String translationKey, ForgeConfigSpec.IntValue configValue, 
                                         int min, int max) {
        int initialValue = configValue.get();
        final int[] currentValue = {initialValue}; // Mutable container
        
        SliderPercentageOption slider = new SliderPercentageOption(
                translationKey,
                min, max, 1f,
                gameSettings -> (double) currentValue[0],
                (gameSettings, value) -> {
                    currentValue[0] = value.intValue();
                    // Send update to server
                    NetworkHandler.sendToServer(new AdminPanelMessage(
                            translationKey,
                            String.valueOf(currentValue[0])));
                },
                (gameSettings, option) -> new StringTextComponent(
                        I18n.get(translationKey) + ": " + currentValue[0]));
        
        // Using a fixed Widget instance instead of createButton for Forge compatibility
        Widget sliderWidget = slider.createButton(this.minecraft.options, this.width / 2 - 155, 0, 150);
        this.addWidget(sliderWidget);
        addWidgetToOptionsRow(sliderWidget, true);
    }
    
    /**
     * Helper to add a server-side double slider option
     */
    private void addServerDoubleSliderOption(String translationKey, ForgeConfigSpec.DoubleValue configValue, 
                                            double min, double max) {
        double initialValue = configValue.get();
        final double[] currentValue = {initialValue}; // Mutable container
        
        SliderPercentageOption slider = new SliderPercentageOption(
                translationKey,
                min, max, 0.01f,
                gameSettings -> currentValue[0],
                (gameSettings, value) -> {
                    currentValue[0] = value;
                    // Send update to server
                    NetworkHandler.sendToServer(new AdminPanelMessage(
                            translationKey,
                            String.valueOf(currentValue[0])));
                },
                (gameSettings, option) -> new StringTextComponent(
                        I18n.get(translationKey) + ": " + String.format("%.2f", currentValue[0])));
        
        // Using a fixed Widget instance instead of createButton for Forge compatibility
        Widget sliderWidget = slider.createButton(this.minecraft.options, this.width / 2 - 155, 0, 150);
        this.addWidget(sliderWidget);
        addWidgetToOptionsRow(sliderWidget, true);
    }
    
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // Render the background
        this.renderBackground(matrixStack);
        
        // Render the GUI background
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(BACKGROUND);
        int guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        int guiTop = (this.height - BACKGROUND_HEIGHT) / 2;
        this.blit(matrixStack, guiLeft, guiTop, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        
        // Render title
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, guiTop + 10, 0xFFFFFF);
        
        // Render tab-specific content
        if (currentTab == Tab.CLIENT_SETTINGS || currentTab == Tab.SERVER_SETTINGS || currentTab == Tab.WEB_INTERFACE) {
            // Render all option lists for this tab
            for (OptionsRowList list : tabOptionLists) {
                list.render(matrixStack, mouseX, mouseY, partialTicks);
            }
        }
        
        // Render web interface URL and status (read-only)
        if (currentTab == Tab.WEB_INTERFACE) {
            String urlText = "URL: http://localhost:" + AICompanionConfig.SERVER.webInterfacePort.get();
            drawString(matrixStack, this.font, urlText, guiLeft + 20, guiTop + 160, 0xFFFFFF);
            
            String statusText = "Status: " + (AICompanionConfig.SERVER.enableWebInterface.get() ? "Enabled" : "Disabled");
            drawString(matrixStack, this.font, statusText, guiLeft + 20, guiTop + 172, AICompanionConfig.SERVER.enableWebInterface.get() ? 0x55FF55 : 0xFF5555);
        }
        
        // Render buttons
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}