package com.aicompanion.mod.entity.ai.goal;

import com.aicompanion.mod.entity.AICompanionEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.server.ServerWorld;

import java.util.EnumSet;
import java.util.UUID;

public class BreakBlockGoal extends Goal {
    private final AICompanionEntity companion;
    private int breakingTime;
    private int lastBreakProgress = -1;
    private BlockPos targetBlock;
    private boolean reachedBlock = false;
    private int timeoutCounter = 0;
    
    public BreakBlockGoal(AICompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.companion.isActive() || !this.companion.getCurrentTask().equals("break")) {
            return false;
        }
        
        // Check if the owner is in creative mode
        if (this.companion.getOwner() instanceof PlayerEntity) {
            PlayerEntity owner = (PlayerEntity) this.companion.getOwner();
            if (owner.abilities.instabuild) {
                // Allow breaking in creative mode
                return true;
            }
        }
        
        // Check if we have a valid target block
        this.targetBlock = this.companion.getTargetPos();
        if (this.targetBlock == null || this.targetBlock.equals(BlockPos.ZERO)) {
            return false;
        }
        
        // Check if the block is valid for breaking
        BlockState blockState = this.companion.level.getBlockState(this.targetBlock);
        return !blockState.isAir(this.companion.level, this.targetBlock);
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.companion.isActive() || !this.companion.getCurrentTask().equals("break")) {
            return false;
        }
        
        // Check if the block is still valid
        BlockState blockState = this.companion.level.getBlockState(this.targetBlock);
        if (blockState.isAir(this.companion.level, this.targetBlock)) {
            // Block has been broken
            if (this.companion.getOwner() != null) {
                this.companion.getOwner().sendMessage(
                        new StringTextComponent("AI Companion has broken the block"), UUID.randomUUID());
            }
            this.companion.setCurrentTask("idle");
            return false;
        }
        
        // Timeout after 200 ticks (10 seconds) if we can't reach the block
        if (!this.reachedBlock && this.timeoutCounter++ > 200) {
            if (this.companion.getOwner() != null) {
                this.companion.getOwner().sendMessage(
                        new StringTextComponent("AI Companion couldn't reach the block to break it"), UUID.randomUUID());
            }
            this.companion.setCurrentTask("idle");
            return false;
        }
        
        return true;
    }

    @Override
    public void start() {
        this.companion.getNavigation().moveTo(
                this.targetBlock.getX() + 0.5, 
                this.targetBlock.getY(), 
                this.targetBlock.getZ() + 0.5, 
                1.0);
        this.breakingTime = 0;
        this.reachedBlock = false;
        this.timeoutCounter = 0;
    }

    @Override
    public void stop() {
        this.companion.getNavigation().stop();
        this.companion.level.destroyBlockProgress(this.companion.getId(), this.targetBlock, -1);
        this.lastBreakProgress = -1;
    }

    @Override
    public void tick() {
        // Look at the target block
        this.companion.getLookControl().setLookAt(
                this.targetBlock.getX() + 0.5, 
                this.targetBlock.getY() + 0.5, 
                this.targetBlock.getZ() + 0.5, 
                10.0F, (float)this.companion.getMaxHeadXRot());
        
        // Check if we're close enough to the block
        double distanceSq = this.companion.distanceToSqr(
                this.targetBlock.getX() + 0.5, 
                this.targetBlock.getY() + 0.5, 
                this.targetBlock.getZ() + 0.5);
        
        if (distanceSq > 4.0) { // Need to be within 2 blocks
            // Not close enough, keep moving
            if (this.companion.getNavigation().isDone()) {
                // Try to pathfind again
                this.companion.getNavigation().moveTo(
                        this.targetBlock.getX() + 0.5, 
                        this.targetBlock.getY(), 
                        this.targetBlock.getZ() + 0.5, 
                        1.0);
            }
            return;
        }
        
        // We're close enough to the block
        this.reachedBlock = true;
        
        // Check if the owner is in creative mode for instant breaking
        boolean creativeMode = false;
        if (this.companion.getOwner() instanceof PlayerEntity) {
            PlayerEntity owner = (PlayerEntity) this.companion.getOwner();
            creativeMode = owner.abilities.instabuild;
        }
        
        if (creativeMode) {
            // Creative mode: break instantly
            if (this.companion.level instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) this.companion.level;
                serverWorld.destroyBlock(this.targetBlock, true, this.companion);
                this.companion.setCurrentTask("idle");
                
                if (this.companion.getOwner() != null) {
                    this.companion.getOwner().sendMessage(
                            new StringTextComponent("AI Companion has broken the block"), UUID.randomUUID());
                }
            }
            return;
        }
        
        // Survival mode: break progressively
        BlockState blockState = this.companion.level.getBlockState(this.targetBlock);
        if (!blockState.isAir(this.companion.level, this.targetBlock)) {
            this.breakingTime++;
            
            // Animate the breaking
            int i = (int)(this.breakingTime / 20.0F * 10.0F);
            if (i != this.lastBreakProgress) {
                this.companion.level.destroyBlockProgress(this.companion.getId(), this.targetBlock, i);
                this.lastBreakProgress = i;
            }
            
            // Break the block after a certain time (depending on block hardness)
            float hardness = blockState.getDestroySpeed(this.companion.level, this.targetBlock);
            int breakTime = (int)(hardness * 30); // Roughly equivalent to player breaking speed
            
            if (this.breakingTime >= breakTime) {
                // Break the block
                if (this.companion.level instanceof ServerWorld) {
                    ServerWorld serverWorld = (ServerWorld) this.companion.level;
                    serverWorld.destroyBlock(this.targetBlock, true, this.companion);
                    
                    // Swing arm animation
                    this.companion.swing(Hand.MAIN_HAND);
                    
                    // Task complete
                    this.companion.setCurrentTask("idle");
                    
                    if (this.companion.getOwner() != null) {
                        this.companion.getOwner().sendMessage(
                                new StringTextComponent("AI Companion has broken the block"), UUID.randomUUID());
                    }
                }
            }
        }
    }
}
