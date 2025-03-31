package com.aicompanion.mod.entity.renderer;

import com.aicompanion.mod.entity.MiniBuildEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;

public class MiniBuildRenderer extends EntityRenderer<MiniBuildEntity> {
    private static final float SCALE_FACTOR = 0.1f;
    
    public MiniBuildRenderer(EntityRendererManager rendererManager) {
        super(rendererManager);
        this.shadowRadius = 0.0F; // No shadow
    }

    @Override
    public ResourceLocation getTextureLocation(MiniBuildEntity entity) {
        // This entity doesn't use a texture but uses block rendering instead
        return new ResourceLocation("minecraft", "textures/block/stone.png");
    }
    
    @Override
    public boolean shouldRender(MiniBuildEntity entity, ClippingHelper camera, double camX, double camY, double camZ) {
        // Add a bit more generous culling since we're rendering multiple blocks
        return entity.isInRange(camX, camY, camZ, 64.0D);
    }
    
    @Override
    public void render(MiniBuildEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, 
                      IRenderTypeBuffer buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
        
        matrixStack.pushPose();
        
        // Apply scale to make it small
        matrixStack.scale(SCALE_FACTOR, SCALE_FACTOR, SCALE_FACTOR);
        
        // Get the block renderer
        BlockRendererDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        
        // Render each mini block
        for (MiniBuildEntity.MiniBlock miniBlock : entity.getMiniBlocks()) {
            BlockPos relPos = miniBlock.getRelativePos();
            BlockState blockState = miniBlock.getBlockState();
            
            if (blockState != null && blockState.getRenderShape() != BlockRenderType.INVISIBLE) {
                matrixStack.pushPose();
                
                // Position the block relative to the entity
                matrixStack.translate(relPos.getX(), relPos.getY(), relPos.getZ());
                
                // Render the block
                blockRenderer.renderBlock(blockState, matrixStack, buffer, 
                        packedLight, OverlayTexture.NO_OVERLAY, 
                        net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
                
                matrixStack.popPose();
            }
        }
        
        // If giant player is visible, render a giant player model in the sky
        if (entity.isGiantPlayerVisible()) {
            renderGiantPlayer(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
        }
        
        matrixStack.popPose();
    }
    
    /**
     * Render a giant version of the player in the sky
     */
    private void renderGiantPlayer(MiniBuildEntity entity, float entityYaw, float partialTicks, 
                                 MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        // Get the owner player
        Minecraft minecraft = Minecraft.getInstance();
        String ownerUUID = entity.getOwnerUUID();
        
        if (ownerUUID.isEmpty()) {
            return;
        }
        
        // Check if it's our own player
        if (minecraft.player != null && minecraft.player.getUUID().toString().equals(ownerUUID)) {
            matrixStack.pushPose();
            
            // Position high in the sky
            matrixStack.translate(0, 50, 0);
            // Make it giant (20x normal size)
            matrixStack.scale(20.0F, 20.0F, 20.0F);
            
            // Render the player model
            // Note: This is a simplified implementation. In a real mod,
            // you would need to properly render the player's skin model
            
            matrixStack.popPose();
        }
    }
}