package com.aicompanion.mod.entity.ai.goal;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.entity.AICompanionEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * AI Goal that allows companions to use items (consume food, use tools, etc.)
 */
public class UseItemGoal extends Goal {
    private final AICompanionEntity companion;
    private final double moveSpeed;
    private final float maxDistance;
    
    private ItemStack targetItem;
    private BlockPos targetPos;
    private LivingEntity targetEntity;
    private int useItemTimer;
    private boolean isUsingItem;
    private int useItemWarmup;
    private int itemUseTicks;
    
    public UseItemGoal(AICompanionEntity companionEntity, double speed, float maxDistance) {
        this.companion = companionEntity;
        this.moveSpeed = speed;
        this.maxDistance = maxDistance;
        this.useItemTimer = 0;
        this.isUsingItem = false;
        this.useItemWarmup = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        // Check if companion has a current "use" task
        if (!"use".equals(companion.getCurrentTask())) {
            return false;
        }
        
        // Check if companion has an item to use
        targetItem = companion.getHeldItem();
        if (targetItem.isEmpty()) {
            return false;
        }
        
        // Get target position or entity
        targetPos = companion.getTargetPos();
        targetEntity = null;
        
        // Check if we have a valid target entity (perhaps from a UUID stored in NBT data)
        if (companion.getTargetEntityId() != null) {
            targetEntity = companion.getTargetEntity();
            if (targetEntity == null || !targetEntity.isAlive()) {
                return false;
            }
        }
        
        // Can use if we have an item and either a target position or entity
        return true;
    }
    
    @Override
    public void start() {
        // Reset timers
        useItemTimer = 0;
        isUsingItem = false;
        useItemWarmup = 0;
        
        // Move to use position if needed
        if (targetEntity != null) {
            companion.getNavigation().moveTo(targetEntity, moveSpeed);
        } else if (targetPos != null) {
            companion.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), moveSpeed);
        }
    }
    
    @Override
    public void stop() {
        // Stop using item if we were
        if (isUsingItem) {
            companion.stopUsingItem();
            isUsingItem = false;
        }
        
        // Reset navigation
        companion.getNavigation().stop();
        
        // Reset task when done
        if ("use".equals(companion.getCurrentTask())) {
            companion.setCurrentTask("idle");
        }
    }
    
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
    
    @Override
    public void tick() {
        World world = companion.level;
        
        // Update movement to target entity if it's moving
        if (targetEntity != null && targetEntity.isAlive()) {
            // If we're too far, keep following
            if (companion.distanceToSqr(targetEntity) > maxDistance * maxDistance) {
                companion.getNavigation().moveTo(targetEntity, moveSpeed);
                return;
            }
            
            // Look at target
            companion.getLookControl().setLookAt(targetEntity, 30.0F, 30.0F);
        } else if (targetPos != null) {
            // Look at target block
            double d0 = targetPos.getX() + 0.5D;
            double d1 = targetPos.getY() + 0.5D;
            double d2 = targetPos.getZ() + 0.5D;
            companion.getLookControl().setLookAt(d0, d1, d2, 30.0F, 30.0F);
            
            // If we're too far, keep moving
            double distanceSq = companion.distanceToSqr(d0, d1, d2);
            if (distanceSq > maxDistance * maxDistance) {
                companion.getNavigation().moveTo(d0, d1, d2, moveSpeed);
                return;
            }
        }
        
        // Handle using the item
        useItemTimer++;
        
        // Warmup period before using
        if (useItemWarmup < 20) {
            useItemWarmup++;
            return;
        }
        
        if (!isUsingItem) {
            // Start using the item
            UseItemResult result = tryUseItem();
            
            if (result == UseItemResult.SUCCESS) {
                isUsingItem = true;
                itemUseTicks = 0;
                
                // Log the action
                AICompanionMod.LOGGER.info("AI Companion started using item: " + targetItem.getItem().getRegistryName());
            } else if (result == UseItemResult.FAILED || useItemTimer > 60) {
                // If using failed or we waited too long, give up
                companion.setCurrentTask("idle");
                return;
            }
        } else {
            // Continue using the item
            itemUseTicks++;
            
            // Get the use duration for this item
            int useTime = targetItem.getItem().getUseDuration(targetItem);
            
            // For items with a use duration, continue until done
            if (useTime > 0 && itemUseTicks < useTime) {
                // Still using the item
                return;
            }
            
            // Finish using the item
            companion.stopUsingItem();
            isUsingItem = false;
            
            // Task complete
            companion.setCurrentTask("idle");
            
            // Log the completed action
            AICompanionMod.LOGGER.info("AI Companion finished using item: " + targetItem.getItem().getRegistryName());
        }
    }
    
    /**
     * Try to use the item, returning the result
     */
    private UseItemResult tryUseItem() {
        // Skip if we don't have a valid item
        if (targetItem.isEmpty()) {
            return UseItemResult.FAILED;
        }
        
        World world = companion.level;
        
        // Try to use the item
        ActionResult<ItemStack> actionResult = null;
        
        try {
            // Get item use action type
            UseAction useAction = targetItem.getUseAnimation();
            Item item = targetItem.getItem();
            
            // Set the item in hand first
            companion.setItemInHand(Hand.MAIN_HAND, targetItem);
            
            // Use item on entity if applicable
            if (targetEntity != null) {
                ActionResultType result = companion.interactOn(targetEntity, Hand.MAIN_HAND);
                if (result.consumesAction()) {
                    return UseItemResult.SUCCESS;
                }
            }
            
            // Use item in world
            actionResult = item.use(world, companion, Hand.MAIN_HAND);
            
            // Check result
            if (actionResult.getResult() == ActionResultType.SUCCESS) {
                // Update the companion's item if it changed
                if (actionResult.getObject() != targetItem) {
                    companion.setHeldItem(actionResult.getObject());
                    targetItem = actionResult.getObject();
                }
                return UseItemResult.SUCCESS;
            } else if (actionResult.getResult() == ActionResultType.CONSUME) {
                // Handle consumable items
                companion.startUsingItem(Hand.MAIN_HAND);
                return UseItemResult.SUCCESS;
            }
            
            return UseItemResult.FAILED;
        } catch (Exception e) {
            AICompanionMod.LOGGER.error("Error using item", e);
            return UseItemResult.FAILED;
        }
    }
    
    private enum UseItemResult {
        SUCCESS,
        FAILED,
        RETRY
    }
}