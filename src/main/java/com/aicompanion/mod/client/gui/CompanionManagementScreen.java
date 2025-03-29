package com.aicompanion.mod.client.gui;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.entity.AICompanionEntity;
import com.aicompanion.mod.network.NetworkHandler;
import com.aicompanion.mod.network.message.CompanionManagementMessage;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Screen for managing AI Companions
 */
@OnlyIn(Dist.CLIENT)
public class CompanionManagementScreen extends Screen {
    
    private static final ResourceLocation BACKGROUND = new ResourceLocation(AICompanionMod.MOD_ID, "textures/gui/companion_management.png");
    private static final int BACKGROUND_WIDTH = 248;
    private static final int BACKGROUND_HEIGHT = 200;
    
    private final Screen parentScreen;
    private CompanionList companionList;
    private Button backButton;
    private Button teleportButton;
    private Button renameButton;
    private Button removeButton;
    private Button commandButton;
    private TextFieldWidget searchField;
    private List<CompanionEntry> allCompanions;
    
    /**
     * Constructor
     */
    public CompanionManagementScreen(Screen parentScreen) {
        super(new TranslationTextComponent("screen.aicompanion.manage_companions"));
        this.parentScreen = parentScreen;
        this.allCompanions = new ArrayList<>();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Calculate centered position
        int guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        int guiTop = (this.height - BACKGROUND_HEIGHT) / 2;
        
        // Create search field
        this.searchField = new TextFieldWidget(this.font, 
                guiLeft + 95, guiTop + 25, 
                BACKGROUND_WIDTH - 110, 16, 
                new StringTextComponent(""));
        this.searchField.setMaxLength(32);
        this.searchField.setResponder(this::onSearchTextChanged);
        this.children.add(this.searchField);
        
        // Create companion list
        this.companionList = new CompanionList(guiLeft + 10, guiTop + 45, BACKGROUND_WIDTH - 20, 100);
        this.children.add(companionList);
        
        // Populate companion list
        populateCompanionList();
        
        // Create action buttons
        this.teleportButton = new Button(guiLeft + 10, guiTop + 150, 55, 20,
                new TranslationTextComponent("button.aicompanion.teleport"), this::onTeleportClicked);
        
        this.renameButton = new Button(guiLeft + 70, guiTop + 150, 55, 20,
                new TranslationTextComponent("button.aicompanion.rename"), this::onRenameClicked);
        
        this.removeButton = new Button(guiLeft + 130, guiTop + 150, 55, 20,
                new TranslationTextComponent("button.aicompanion.remove"), this::onRemoveClicked);
        
        this.commandButton = new Button(guiLeft + 190, guiTop + 150, 55, 20,
                new TranslationTextComponent("button.aicompanion.command"), this::onCommandClicked);
        
        this.backButton = new Button(guiLeft + (BACKGROUND_WIDTH - 100) / 2, guiTop + 175, 100, 20,
                new StringTextComponent("Back"), button -> this.onClose());
        
        // Add buttons
        this.addButton(teleportButton);
        this.addButton(renameButton);
        this.addButton(removeButton);
        this.addButton(commandButton);
        this.addButton(backButton);
        
        // Initial state: disable action buttons until selection
        updateButtonStates();
    }
    
    /**
     * Populate the list with current companions
     */
    private void populateCompanionList() {
        allCompanions.clear();
        
        // Get player's companions from the world
        if (minecraft != null && minecraft.level != null) {
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof AICompanionEntity) {
                    AICompanionEntity companion = (AICompanionEntity) entity;
                    
                    // Only show companions owned by this player
                    UUID ownerUuid = companion.getOwnerUUID();
                    if (ownerUuid != null && ownerUuid.equals(minecraft.player.getUUID())) {
                        allCompanions.add(new CompanionEntry(
                                companion.getUUID().toString(), 
                                companion.getName().getString(), 
                                (int)companion.distanceTo(minecraft.player)));
                    }
                }
            }
        }
        
        // Apply any current filter
        applyFilter(searchField.getValue());
    }
    
    /**
     * Handle search filter changes
     */
    private void onSearchTextChanged(String text) {
        applyFilter(text);
    }
    
    /**
     * Apply search filter to the list
     */
    private void applyFilter(String filter) {
        companionList.clearEntries();
        
        if (filter.isEmpty()) {
            // Show all companions
            for (CompanionEntry entry : allCompanions) {
                companionList.addEntry(entry);
            }
        } else {
            // Filter companions by name
            String lowerFilter = filter.toLowerCase();
            for (CompanionEntry entry : allCompanions) {
                if (entry.name.toLowerCase().contains(lowerFilter)) {
                    companionList.addEntry(entry);
                }
            }
        }
        
        // Update button states after filtering
        updateButtonStates();
    }
    
    /**
     * Update button states based on current selection
     */
    private void updateButtonStates() {
        boolean hasSelection = companionList.getSelected() != null;
        
        teleportButton.active = hasSelection;
        renameButton.active = hasSelection;
        removeButton.active = hasSelection;
        commandButton.active = hasSelection;
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
        
        // Draw search label
        this.font.draw(matrixStack, "Search:", guiLeft + 10, guiTop + 30, 0xFFFFFF);
        
        // Render the text field
        this.searchField.render(matrixStack, mouseX, mouseY, partialTicks);
        
        // Render the companion list
        this.companionList.render(matrixStack, mouseX, mouseY, partialTicks);
        
        // Render buttons
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * Handle "Teleport" button click
     */
    private void onTeleportClicked(Button button) {
        CompanionEntry selected = companionList.getSelected();
        if (selected != null) {
            // Send teleport request to server
            NetworkHandler.sendToServer(new CompanionManagementMessage(
                    CompanionManagementMessage.Action.TELEPORT, 
                    selected.uuid, 
                    null));
        }
    }
    
    /**
     * Handle "Rename" button click
     */
    private void onRenameClicked(Button button) {
        CompanionEntry selected = companionList.getSelected();
        if (selected != null) {
            // Open rename screen
            minecraft.setScreen(new CompanionRenameScreen(selected.uuid, selected.name, this));
        }
    }
    
    /**
     * Handle "Remove" button click
     */
    private void onRemoveClicked(Button button) {
        CompanionEntry selected = companionList.getSelected();
        if (selected != null) {
            // Send remove request to server
            NetworkHandler.sendToServer(new CompanionManagementMessage(
                    CompanionManagementMessage.Action.REMOVE, 
                    selected.uuid, 
                    null));
            
            // Remove from local list and refresh
            allCompanions.remove(selected);
            applyFilter(searchField.getValue());
        }
    }
    
    /**
     * Handle "Command" button click
     */
    private void onCommandClicked(Button button) {
        CompanionEntry selected = companionList.getSelected();
        if (selected != null) {
            // Open command screen
            minecraft.setScreen(new CompanionCommandScreen(selected.uuid, selected.name));
        }
    }
    
    /**
     * List widget for companions
     */
    private class CompanionList extends ExtendedList<CompanionEntry> {
        
        public CompanionList(int x, int y, int width, int height) {
            super(Minecraft.getInstance(), width, height, y, y + height, 20);
            this.x0 = x;
            this.x1 = x + width;
        }
        
        @Override
        protected boolean isSelectedItem(int index) {
            return this.getSelected() == this.children().get(index);
        }
        
        @Override
        protected void clearEntries() {
            super.clearEntries();
        }
        
        @Override
        public void setSelected(CompanionEntry entry) {
            super.setSelected(entry);
            updateButtonStates();
        }
        
        @Override
        public int getRowWidth() {
            return width - 12;
        }
    }
    
    /**
     * Companion list entry
     */
    private class CompanionEntry extends ExtendedList.AbstractListEntry<CompanionEntry> {
        
        private final String uuid;
        private final String name;
        private final int distance;
        
        public CompanionEntry(String uuid, String name, int distance) {
            this.uuid = uuid;
            this.name = name;
            this.distance = distance;
        }
        
        @Override
        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks) {
            // Render name
            font.draw(matrixStack, name, left + 5, top + 5, 0xFFFFFF);
            
            // Render distance
            String distanceText = distance + " blocks away";
            font.draw(matrixStack, distanceText, 
                    left + width - font.width(distanceText) - 5, top + 5, 0xAAAAAA);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            companionList.setSelected(this);
            return true;
        }
    }
}