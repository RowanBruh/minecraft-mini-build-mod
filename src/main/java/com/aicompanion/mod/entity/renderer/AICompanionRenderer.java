package com.aicompanion.mod.entity.renderer;

import com.aicompanion.mod.entity.AICompanionEntity;
import com.aicompanion.mod.entity.model.AICompanionModel;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;

public class AICompanionRenderer extends MobRenderer<AICompanionEntity, AICompanionModel> {
    
    public AICompanionRenderer(EntityRendererManager rendererManager) {
        super(rendererManager, new AICompanionModel(), 0.5F);
    }
    
    @Override
    public ResourceLocation getTextureLocation(AICompanionEntity entity) {
        return AICompanionModel.TEXTURE;
    }
    
    @Override
    protected boolean isShaking(AICompanionEntity entity) {
        // Make the companion shake when inactive
        return !entity.isActive();
    }
}
