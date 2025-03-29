package com.aicompanion.mod.entity.ai.goal;

import com.aicompanion.mod.entity.AICompanionEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.UUID;

public class PlaceBlockGoal extends Goal {
    private final AICompanionEntity companion;
    private BlockPos targetPos;
    private boolean reachedPos = false;
    private int timeoutCounter = 0;
    private int placingAttempts = 0;
    
    public PlaceBlockGoal(AICompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.companion.isActive() || !this.companion.getCurrentTask().equals("place")) {
            return false;
        }
        
        // Check if we have a valid target position
        this.targetPos = this.companion.getTargetPos();
        if (this.targetPos == null || this.targetPos.equals(BlockPos.ZERO)) {
            return false;
        }
        
        // Check if we have a valid item to place
        ItemStack heldItem = this.companion.getHeldItem();
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof BlockItem)) {
            if (this.companion.getOwner() != null) {
                this.companion.getOwner().sendMessage(
                        new StringTextComponent("AI Companion doesn't have a valid block to place"), UUID.randomUUID());
            }
            this.companion.setCurrentTask("idle");
            return false;
        }
        
        // Check if the space is valid for placing a block
        BlockState existingBlock = this.companion.level.getBlockState(this.targetPos);
        if (!existingBlock.isAir(this.companion.level, this.targetPos)) {
            if (this.companion.getOwner() != null) {
                this.companion.getOwner().sendMessage(
                        new StringTextComponent("Cannot place block: space is occupied"), UUID.randomUUID());
            }
            this.companion.setCurrentTask("idle");
            return false;
        }
        
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.companion.isActive() || !this.companion.getCurrentTask().equals("place")) {
            return false;
        }
        
        // Check if the block has been placed
        BlockState existingBlock = this.companion.level.getBlockState(this.targetPos);
        ItemStack heldItem = this.companion.getHeldItem();
        
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof BlockItem)) {
            this.companion.setCurrentTask("idle");
            return false;
        }
        
        BlockItem blockItem = (BlockItem) heldItem.getItem();
        Block block = blockItem.getBlock();
        
        if (existingBlock.getBlock() == block) {
            // Block has been successfully placed
            if (this.companion.getOwner() != null) {
                this.companion.getOwner().sendMessage(
                        new StringTextComponent("AI Companion has placed the block"), UUID.randomUUID());
            }
            this.companion.setCurrentTask("idle");
            return false;
        }
        
        // Timeout after 200 ticks (10 seconds) if we can't reach the position
        if (!this.reachedPos && this.timeoutCounter++ > 200) {
            if (this.companion.getOwner() != null) {
                this.companion.getOwner().sendMessage(
                        new StringTextComponent("AI Companion couldn't reach the position to place the block"), UUID.randomUUID());
            }
            this.companion.setCurrentTask("idle");
            return false;
        }
        
        // Give up after 5 attempts to place the block
        if (this.reachedPos && this.placingAttempts >= 5) {
            if (this.companion.getOwner() != null) {
                this.companion.getOwner().sendMessage(
                        new StringTextComponent("AI Companion failed to place the block after multiple attempts"), UUID.randomUUID());
            }
            this.companion.setCurrentTask("idle");
            return false;
        }
        
        return true;
    }

    @Override
    public void start() {
        // Move to adjacent position where we can place the block
        BlockPos adjacentPos = findAdjacentPosition();
        if (adjacentPos != null) {
            this.companion.getNavigation().moveTo(
                    adjacentPos.getX() + 0.5, 
                    adjacentPos.getY(), 
                    adjacentPos.getZ() + 0.5, 
                    1.0);
        } else {
            // No valid adjacent position found
            if (this.companion.getOwner() != null) {
                this.companion.getOwner().sendMessage(
                        new StringTextComponent("AI Companion can't find a position to place the block from"), UUID.randomUUID());
            }
            this.companion.setCurrentTask("idle");
        }
        
        this.reachedPos = false;
        this.timeoutCounter = 0;
        this.placingAttempts = 0;
    }

    @Override
    public void stop() {
        this.companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        // Look at the target position
        this.companion.getLookControl().setLookAt(
                this.targetPos.getX() + 0.5, 
                this.targetPos.getY() + 0.5, 
                this.targetPos.getZ() + 0.5, 
                10.0F, (float)this.companion.getMaxHeadXRot());
        
        BlockPos adjacentPos = findAdjacentPosition();
        if (adjacentPos == null) {
            // No valid adjacent position
            if (this.companion.getOwner() != null) {
                this.companion.getOwner().sendMessage(
                        new StringTextComponent("AI Companion can't find a position to place the block from"), UUID.randomUUID());
            }
            this.companion.setCurrentTask("idle");
            return;
        }
        
        // Check if we're close enough to the position
        double distanceSq = this.companion.distanceToSqr(
                adjacentPos.getX() + 0.5, 
                adjacentPos.getY() + 0.5, 
                adjacentPos.getZ() + 0.5);
        
        if (distanceSq > 4.0) { // Need to be within 2 blocks
            // Not close enough, keep moving
            if (this.companion.getNavigation().isDone()) {
                // Try to pathfind again
                this.companion.getNavigation().moveTo(
                        adjacentPos.getX() + 0.5, 
                        adjacentPos.getY(), 
                        adjacentPos.getZ() + 0.5, 
                        1.0);
            }
            return;
        }
        
        // We're close enough to place the block
        this.reachedPos = true;
        
        // Try to place the block
        ItemStack heldItem = this.companion.getHeldItem();
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof BlockItem)) {
            this.companion.setCurrentTask("idle");
            return;
        }
        
        Direction direction = getPlacementDirection(adjacentPos);
        if (direction == null) {
            this.companion.setCurrentTask("idle");
            return;
        }
        
        // Place the block
        World world = this.companion.level;
        BlockItem blockItem = (BlockItem) heldItem.getItem();
        
        // Simulate a right-click on the adjacent block
        Vector3d hitVec = new Vector3d(
                adjacentPos.getX() + 0.5 + direction.getStepX() * 0.5,
                adjacentPos.getY() + 0.5 + direction.getStepY() * 0.5,
                adjacentPos.getZ() + 0.5 + direction.getStepZ() * 0.5);
        
        BlockRayTraceResult rayTraceResult = new BlockRayTraceResult(
                hitVec, direction.getOpposite(), adjacentPos, false);
        
        // Try to place the block
        if (!world.isClientSide) {
            this.placingAttempts++;
            this.companion.swing(Hand.MAIN_HAND);
            
            ItemUseContext context = new ItemUseContext(
                    world, null, Hand.MAIN_HAND, heldItem, rayTraceResult);
            
            ActionResultType result = blockItem.useOn(context);
            
            if (result == ActionResultType.SUCCESS) {
                // Block placed successfully
                this.companion.setCurrentTask("idle");
                
                if (this.companion.getOwner() != null) {
                    this.companion.getOwner().sendMessage(
                            new StringTextComponent("AI Companion has placed the block"), UUID.randomUUID());
                }
            }
        }
    }
    
    private BlockPos findAdjacentPosition() {
        // Try all adjacent positions to find a valid one
        Direction[] directions = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, 
            Direction.UP, Direction.DOWN
        };
        
        for (Direction dir : directions) {
            BlockPos checkPos = this.targetPos.relative(dir);
            BlockState state = this.companion.level.getBlockState(checkPos);
            
            // Check if we can stand at this position
            if (this.companion.level.loadedAndEntityCanStandOn(checkPos, this.companion) || dir == Direction.UP || dir == Direction.DOWN) {
                // If this is a valid position, check if we have a solid face to place against
                if (state.isFaceSturdy(this.companion.level, checkPos, dir.getOpposite())) {
                    return checkPos;
                }
            }
        }
        
        return null;
    }
    
    private Direction getPlacementDirection(BlockPos adjacentPos) {
        // Determine which direction to place the block based on the adjacent position
        if (adjacentPos.getX() > this.targetPos.getX()) return Direction.WEST;
        if (adjacentPos.getX() < this.targetPos.getX()) return Direction.EAST;
        if (adjacentPos.getY() > this.targetPos.getY()) return Direction.DOWN;
        if (adjacentPos.getY() < this.targetPos.getY()) return Direction.UP;
        if (adjacentPos.getZ() > this.targetPos.getZ()) return Direction.NORTH;
        if (adjacentPos.getZ() < this.targetPos.getZ()) return Direction.SOUTH;
        
        return null;
    }
}
