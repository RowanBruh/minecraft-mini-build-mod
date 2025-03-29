package com.aicompanion.mod.client.gui;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.entity.AICompanionEntity;
import com.aicompanion.mod.network.NetworkHandler;
import com.aicompanion.mod.network.message.CommandMessage;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

/**
 * Screen for sending commands to a specific AI Companion
 */
@OnlyIn(Dist.CLIENT)
public class CompanionCommandScreen extends Screen {
    
    private static final ResourceLocation BACKGROUND = new ResourceLocation(AICompanionMod.MOD_ID, "textures/gui/companion_command.png");
    private static final int BACKGROUND_WIDTH = 248;
    private static final int BACKGROUND_HEIGHT = 166;
    
    private final String companionUuid;
    private final String companionName;
    private TextFieldWidget commandField;
    
    // Quick command buttons
    private Button followButton;
    private Button stayButton;
    private Button helpButton;
    private Button cancelButton;
    private Button sendButton;
    
    /**
     * Constructor
     */
    public CompanionCommandScreen(String companionUuid, String companionName) {
        super(new TranslationTextComponent("screen.aicompanion.command_companion"));
        this.companionUuid = companionUuid;
        this.companionName = companionName;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Calculate centered position
        int guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        int guiTop = (this.height - BACKGROUND_HEIGHT) / 2;
        
        // Command input field
        this.commandField = new TextFieldWidget(this.font, 
                guiLeft + 20, guiTop + 50, 
                BACKGROUND_WIDTH - 40, 20, 
                new StringTextComponent(""));
        this.commandField.setMaxLength(100);
        this.commandField.setResponder(this::onTextChanged);
        this.children.add(this.commandField);
        
        // Quick command buttons
        this.followButton = new Button(guiLeft + 20, guiTop + 80, 60, 20, 
                new StringTextComponent("Follow"), this::onFollowClicked);
        this.stayButton = new Button(guiLeft + 90, guiTop + 80, 60, 20, 
                new StringTextComponent("Stay"), this::onStayClicked);
        this.helpButton = new Button(guiLeft + 160, guiTop + 80, 60, 20, 
                new StringTextComponent("Help"), this::onHelpClicked);
        
        // Action buttons
        this.cancelButton = new Button(guiLeft + 20, guiTop + 120, 100, 20, 
                new StringTextComponent("Cancel"), button -> this.onClose());
        this.sendButton = new Button(guiLeft + 130, guiTop + 120, 100, 20, 
                new StringTextComponent("Send"), this::onSendClicked);
        
        // Add all buttons to the screen
        this.addButton(this.followButton);
        this.addButton(this.stayButton);
        this.addButton(this.helpButton);
        this.addButton(this.cancelButton);
        this.addButton(this.sendButton);
        
        // Initial focus
        this.setInitialFocus(this.commandField);
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
        
        // Render companion name
        String subtitle = companionName;
        drawCenteredString(matrixStack, this.font, subtitle, this.width / 2, guiTop + 25, 0xDDDDDD);
        
        // Render command label
        this.font.draw(matrixStack, I18n.get("screen.aicompanion.command"), 
                guiLeft + 20, guiTop + 40, 0xFFFFFF);
        
        // Render quick commands label
        this.font.draw(matrixStack, I18n.get("screen.aicompanion.quick_commands"), 
                guiLeft + 20, guiTop + 70, 0xFFFFFF);
        
        // Render the text field
        this.commandField.render(matrixStack, mouseX, mouseY, partialTicks);
        
        // Render buttons
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle Enter key to send command
        if (keyCode == 257 || keyCode == 335) { // ENTER or NUMPAD ENTER
            this.onSendClicked(null);
            return true;
        }
        
        // Handle escape key
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        
        // Default handling (for the text field)
        return this.commandField.keyPressed(keyCode, scanCode, modifiers) || 
               this.commandField.canConsumeInput() || 
               super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * Handle text changes in the command field
     */
    private void onTextChanged(String text) {
        // Could add validation or autocomplete here if needed
    }
    
    /**
     * Handle "Follow" quick command
     */
    private void onFollowClicked(Button button) {
        this.commandField.setValue("follow");
        this.onSendClicked(button);
    }
    
    /**
     * Handle "Stay" quick command
     */
    private void onStayClicked(Button button) {
        this.commandField.setValue("stay");
        this.onSendClicked(button);
    }
    
    /**
     * Handle "Help" quick command
     */
    private void onHelpClicked(Button button) {
        this.commandField.setValue("help");
        this.onSendClicked(button);
    }
    
    /**
     * Handle "Send" button click
     */
    private void onSendClicked(Button button) {
        String command = this.commandField.getValue().trim();
        
        if (!command.isEmpty()) {
            // Send command to the server
            NetworkHandler.sendToServer(new CommandMessage(companionUuid, command));
            
            // Close the screen
            this.onClose();
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}