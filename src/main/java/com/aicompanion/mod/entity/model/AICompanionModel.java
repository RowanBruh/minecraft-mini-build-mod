package com.aicompanion.mod.entity.model;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.entity.AICompanionEntity;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class AICompanionModel extends BipedModel<AICompanionEntity> {
    // Define resource location for texture
    public static final ResourceLocation TEXTURE = new ResourceLocation(AICompanionMod.MOD_ID, 
            "textures/entity/ai_companion_texture.png");
    
    private final ModelRenderer body;
    private final ModelRenderer head;
    private final ModelRenderer rightArm;
    private final ModelRenderer leftArm;
    private final ModelRenderer rightLeg;
    private final ModelRenderer leftLeg;
    
    public AICompanionModel() {
        super(0.0F, 0.0F, 64, 64);
        
        // Body
        this.body = new ModelRenderer(this, 16, 16);
        this.body.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, 0.0F);
        this.body.setPos(0.0F, 0.0F, 0.0F);
        
        // Head
        this.head = new ModelRenderer(this, 0, 0);
        this.head.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, 0.0F);
        this.head.setPos(0.0F, 0.0F, 0.0F);
        
        // Right arm
        this.rightArm = new ModelRenderer(this, 40, 16);
        this.rightArm.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F);
        this.rightArm.setPos(-5.0F, 2.0F, 0.0F);
        
        // Left arm
        this.leftArm = new ModelRenderer(this, 40, 16);
        this.leftArm.mirror = true;
        this.leftArm.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F);
        this.leftArm.setPos(5.0F, 2.0F, 0.0F);
        
        // Right leg
        this.rightLeg = new ModelRenderer(this, 0, 16);
        this.rightLeg.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F);
        this.rightLeg.setPos(-1.9F, 12.0F, 0.0F);
        
        // Left leg
        this.leftLeg = new ModelRenderer(this, 0, 16);
        this.leftLeg.mirror = true;
        this.leftLeg.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F);
        this.leftLeg.setPos(1.9F, 12.0F, 0.0F);
    }
    
    @Override
    public void setupAnim(AICompanionEntity entity, float limbSwing, float limbSwingAmount, 
                               float ageInTicks, float netHeadYaw, float headPitch) {
        // Reset all parts
        this.head.xRot = 0.0F;
        this.head.yRot = 0.0F;
        this.body.xRot = 0.0F;
        this.rightArm.xRot = 0.0F;
        this.rightArm.yRot = 0.0F;
        this.leftArm.xRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.rightLeg.xRot = 0.0F;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.xRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        
        // Head rotation
        this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        this.head.xRot = headPitch * ((float)Math.PI / 180F);
        
        // Arm and leg swing animation during movement
        this.rightArm.xRot = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.leftArm.xRot = MathHelper.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.rightLeg.xRot = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        
        // Special animations for tasks
        String currentTask = entity.getCurrentTask();
        if (currentTask.equals("break")) {
            // Mining animation
            float swingProgress = entity.getAttackAnim(ageInTicks - entity.tickCount);
            this.rightArm.xRot = -((float)Math.PI / 2F) + swingProgress * (float)Math.PI;
        } else if (currentTask.equals("place")) {
            // Placing animation 
            this.rightArm.xRot = -((float)Math.PI / 2F);
            this.rightArm.yRot = 0.2F;
        }
    }
    
    @Override
    public void prepareMobModel(AICompanionEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        
        // Additional model modifications if needed
    }
}
