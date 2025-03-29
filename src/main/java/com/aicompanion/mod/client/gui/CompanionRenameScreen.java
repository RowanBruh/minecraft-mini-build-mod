package com.aicompanion.mod.client.gui;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.network.NetworkHandler;
import com.aicompanion.mod.network.message.CompanionManagementMessage;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Screen for renaming an AI Companion
 */
@OnlyIn(Dist.CLIENT)
public class CompanionRenameScreen extends Screen {
    
    private static final ResourceLocation BACKGROUND = new ResourceLocation(AICompanionMod.MOD_ID, "textures/gui/companion_rename.png");
    private static final int BACKGROUND_WIDTH = 248;
    private static final int BACKGROUND_HEIGHT = 150;
    
    private final String companionUuid;
    private final String currentName;
    private final Screen parentScreen;
    private TextFieldWidget nameField;
    private Button cancelButton;
    private Button renameButton;
    
    /**
     * Constructor
     */
    public CompanionRenameScreen(String companionUuid, String currentName, Screen parentScreen) {
        super(new TranslationTextComponent("screen.aicompanion.rename_companion"));
        this.companionUuid = companionUuid;
        this.currentName = currentName;
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Calculate centered position
        int guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        int guiTop = (this.height - BACKGROUND_HEIGHT) / 2;
        
        // Name input field
        this.nameField = new TextFieldWidget(this.font, 
                guiLeft + 20, guiTop + 50, 
                BACKGROUND_WIDTH - 40, 20, 
                new StringTextComponent(""));
        this.nameField.setMaxLength(32);
        this.nameField.setValue(currentName);
        this.nameField.setResponder(this::onTextChanged);
        this.nameField.setHighlightPos(0);
        this.children.add(this.nameField);
        
        // Action buttons
        this.cancelButton = new Button(guiLeft + 20, guiTop + 100, 100, 20, 
                new StringTextComponent("Cancel"), button -> this.onClose());
        
        this.renameButton = new Button(guiLeft + 130, guiTop + 100, 100, 20, 
                new StringTextComponent("Rename"), this::onRenameClicked);
        
        // Add buttons
        this.addButton(this.cancelButton);
        this.addButton(this.renameButton);
        
        // Initial focus
        this.setInitialFocus(this.nameField);
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
        
        // Render description
        String description = I18n.get("screen.aicompanion.rename_companion.description");
        this.font.draw(matrixStack, description, guiLeft + 20, guiTop + 35, 0xFFFFFF);
        
        // Render the text field
        this.nameField.render(matrixStack, mouseX, mouseY, partialTicks);
        
        // Render buttons
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle Enter key to confirm rename
        if (keyCode == 257 || keyCode == 335) { // ENTER or NUMPAD ENTER
            this.onRenameClicked(null);
            return true;
        }
        
        // Handle escape key
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        
        // Default handling (for the text field)
        return this.nameField.keyPressed(keyCode, scanCode, modifiers) || 
               this.nameField.canConsumeInput() || 
               super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }
    
    /**
     * Handle text changes in the name field
     */
    private void onTextChanged(String text) {
        // Enable rename button only if name is not empty and different from current
        boolean validName = !text.trim().isEmpty() && !text.equals(currentName);
        this.renameButton.active = validName;
    }
    
    /**
     * Handle rename button click
     */
    private void onRenameClicked(Button button) {
        String newName = this.nameField.getValue().trim();
        
        if (!newName.isEmpty() && !newName.equals(currentName)) {
            // Send rename request to server
            NetworkHandler.sendToServer(new CompanionManagementMessage(
                    CompanionManagementMessage.Action.RENAME, 
                    companionUuid, 
                    newName));
            
            // Close the screen
            this.onClose();
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}