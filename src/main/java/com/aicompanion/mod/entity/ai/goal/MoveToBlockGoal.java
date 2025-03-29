package com.aicompanion.mod.entity.ai.goal;

import com.aicompanion.mod.entity.AICompanionEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;

import java.util.EnumSet;
import java.util.UUID;

public class MoveToBlockGoal extends Goal {
    private final AICompanionEntity companion;
    private final double speedModifier;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilTimeout;
    
    public MoveToBlockGoal(AICompanionEntity companion, double speedModifier) {
        this.companion = companion;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.companion.isActive() || !this.companion.getCurrentTask().equals("move")) {
            return false;
        }
        
        // Get target position
        BlockPos targetPos = this.companion.getTargetPos();
        if (targetPos.getX() == 0 && targetPos.getY() == 0 && targetPos.getZ() == 0) {
            return false;
        }
        
        // Check if we're already at the target
        BlockPos companionPos = this.companion.blockPosition();
        if (targetPos.closerThan(companionPos, 2.0)) {
            // Already at destination
            this.companion.setCurrentTask("idle");
            
            if (this.companion.getOwner() != null) {
                this.companion.getOwner().sendMessage(
                        new StringTextComponent("AI Companion reached the destination"), UUID.randomUUID());
            }
            return false;
        }
        
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.ticksUntilTimeout <= 0) {
            return false;
        } else if (!this.companion.isActive() || !this.companion.getCurrentTask().equals("move")) {
            return false;
        } else {
            BlockPos targetPos = this.companion.getTargetPos();
            BlockPos companionPos = this.companion.blockPosition();
            
            return !targetPos.closerThan(companionPos, 2.0);
        }
    }

    @Override
    public void start() {
        BlockPos targetPos = this.companion.getTargetPos();
        
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilTimeout = 100; // Timeout after 5 seconds (100 ticks) of not finding a path
        this.path = this.companion.getNavigation().createPath(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1);
        
        if (this.path != null) {
            this.companion.getNavigation().moveTo(this.path, this.speedModifier);
        } else if (this.companion.getOwner() != null) {
            this.companion.getOwner().sendMessage(
                    new StringTextComponent("AI Companion couldn't find a path to the destination"), UUID.randomUUID());
            this.companion.setCurrentTask("idle");
        }
    }

    @Override
    public void stop() {
        this.companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        BlockPos targetPos = this.companion.getTargetPos();
        this.companion.getLookControl().setLookAt(
                targetPos.getX(), targetPos.getY(), targetPos.getZ(), 
                10.0F, (float)this.companion.getMaxHeadXRot());
        
        if (--this.ticksUntilNextPathRecalculation <= 0) {
            this.ticksUntilNextPathRecalculation = 10;
            
            BlockPos companionPos = this.companion.blockPosition();
            
            if (targetPos.closerThan(companionPos, 2.0)) {
                // Reached destination
                this.companion.getNavigation().stop();
                this.companion.setCurrentTask("idle");
                
                if (this.companion.getOwner() != null) {
                    this.companion.getOwner().sendMessage(
                            new StringTextComponent("AI Companion reached the destination"), UUID.randomUUID());
                }
                return;
            }
            
            if (this.path == null || this.companion.getNavigation().isDone()) {
                Vector3d companionPosition = this.companion.position();
                
                double dx = targetPos.getX() - companionPosition.x;
                double dy = targetPos.getY() - companionPosition.y;
                double dz = targetPos.getZ() - companionPosition.z;
                double distanceSq = dx * dx + dy * dy + dz * dz;
                
                if (distanceSq > 256.0D) { // If too far (16 blocks), teleport closer
                    this.ticksUntilTimeout--;
                    
                    if (this.ticksUntilTimeout <= 0) {
                        // Give up after timeout
                        if (this.companion.getOwner() != null) {
                            this.companion.getOwner().sendMessage(
                                    new StringTextComponent("AI Companion couldn't reach the destination"), UUID.randomUUID());
                        }
                        this.companion.setCurrentTask("idle");
                        return;
                    }
                } else {
                    this.path = this.companion.getNavigation().createPath(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1);
                    if (this.path != null) {
                        this.companion.getNavigation().moveTo(this.path, this.speedModifier);
                    } else {
                        this.ticksUntilTimeout--;
                    }
                }
            }
        }
    }
}
