package com.aicompanion.mod.entity;

import com.aicompanion.mod.entity.ai.goal.BreakBlockGoal;
import com.aicompanion.mod.entity.ai.goal.FollowOwnerGoal;
import com.aicompanion.mod.entity.ai.goal.MoveToBlockGoal;
import com.aicompanion.mod.entity.ai.goal.PlaceBlockGoal;

import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.UUID;

public class AICompanionEntity extends TameableEntity {
    private static final DataParameter<String> CURRENT_TASK = EntityDataManager.defineId(AICompanionEntity.class, DataSerializers.STRING);
    private static final DataParameter<Integer> TARGET_X = EntityDataManager.defineId(AICompanionEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> TARGET_Y = EntityDataManager.defineId(AICompanionEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> TARGET_Z = EntityDataManager.defineId(AICompanionEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> IS_ACTIVE = EntityDataManager.defineId(AICompanionEntity.class, DataSerializers.BOOLEAN);
    
    private ItemStack heldItem = ItemStack.EMPTY;
    
    public AICompanionEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(3, new MoveToBlockGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new BreakBlockGoal(this));
        this.goalSelector.addGoal(5, new PlaceBlockGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CURRENT_TASK, "idle");
        this.entityData.define(TARGET_X, 0);
        this.entityData.define(TARGET_Y, 0);
        this.entityData.define(TARGET_Z, 0);
        this.entityData.define(IS_ACTIVE, true);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("CurrentTask", this.getCurrentTask());
        compound.putInt("TargetX", this.getTargetX());
        compound.putInt("TargetY", this.getTargetY());
        compound.putInt("TargetZ", this.getTargetZ());
        compound.putBoolean("IsActive", this.isActive());
        
        if (!this.heldItem.isEmpty()) {
            CompoundNBT itemTag = new CompoundNBT();
            this.heldItem.save(itemTag);
            compound.put("HeldItem", itemTag);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setCurrentTask(compound.getString("CurrentTask"));
        this.setTargetX(compound.getInt("TargetX"));
        this.setTargetY(compound.getInt("TargetY"));
        this.setTargetZ(compound.getInt("TargetZ"));
        this.setActive(compound.getBoolean("IsActive"));
        
        if (compound.contains("HeldItem")) {
            CompoundNBT itemTag = compound.getCompound("HeldItem");
            this.heldItem = ItemStack.of(itemTag);
        }
    }

    @Override
    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        if (!this.level.isClientSide && player.equals(this.getOwner())) {
            if (player.isShiftKeyDown()) {
                // Toggle active state
                this.setActive(!this.isActive());
                player.sendMessage(new StringTextComponent("AI Companion is now " + 
                        (this.isActive() ? "active" : "inactive")), UUID.randomUUID());
            } else {
                // Display current state and commands
                player.sendMessage(new StringTextComponent("AI Companion Status: " + 
                        (this.isActive() ? "Active" : "Inactive")), UUID.randomUUID());
                player.sendMessage(new StringTextComponent("Current Task: " + this.getCurrentTask()), UUID.randomUUID());
                player.sendMessage(new StringTextComponent("Use commands: /aicompanion <follow|stay|move|break|place>"), UUID.randomUUID());
            }
            return ActionResultType.SUCCESS;
        }
        
        return super.mobInteract(player, hand);
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld world, AgeableEntity entity) {
        return null; // Not breedable
    }

    // Task management methods
    public String getCurrentTask() {
        return this.entityData.get(CURRENT_TASK);
    }

    public void setCurrentTask(String task) {
        this.entityData.set(CURRENT_TASK, task);
    }

    public int getTargetX() {
        return this.entityData.get(TARGET_X);
    }

    public void setTargetX(int x) {
        this.entityData.set(TARGET_X, x);
    }

    public int getTargetY() {
        return this.entityData.get(TARGET_Y);
    }

    public void setTargetY(int y) {
        this.entityData.set(TARGET_Y, y);
    }

    public int getTargetZ() {
        return this.entityData.get(TARGET_Z);
    }

    public void setTargetZ(int z) {
        this.entityData.set(TARGET_Z, z);
    }

    public BlockPos getTargetPos() {
        return new BlockPos(getTargetX(), getTargetY(), getTargetZ());
    }

    public void setTargetPos(BlockPos pos) {
        setTargetX(pos.getX());
        setTargetY(pos.getY());
        setTargetZ(pos.getZ());
    }

    public boolean isActive() {
        return this.entityData.get(IS_ACTIVE);
    }

    public void setActive(boolean active) {
        this.entityData.set(IS_ACTIVE, active);
    }

    public ItemStack getHeldItem() {
        return this.heldItem;
    }

    public void setHeldItem(ItemStack stack) {
        this.heldItem = stack;
    }

    // Command processing methods
    public void processCommand(String command, BlockPos targetPos, ItemStack item) {
        if (!this.isActive() || this.getOwner() == null) {
            return;
        }

        switch (command) {
            case "follow":
                this.setCurrentTask("follow");
                if (this.getOwner() instanceof PlayerEntity) {
                    ((PlayerEntity) this.getOwner()).sendMessage(
                            new StringTextComponent("AI Companion will now follow you"), UUID.randomUUID());
                }
                break;
                
            case "stay":
                this.setCurrentTask("stay");
                if (this.getOwner() instanceof PlayerEntity) {
                    ((PlayerEntity) this.getOwner()).sendMessage(
                            new StringTextComponent("AI Companion will stay at its position"), UUID.randomUUID());
                }
                break;
                
            case "move":
                if (targetPos != null) {
                    this.setCurrentTask("move");
                    this.setTargetPos(targetPos);
                    if (this.getOwner() instanceof PlayerEntity) {
                        ((PlayerEntity) this.getOwner()).sendMessage(
                                new StringTextComponent("AI Companion will move to " + 
                                        targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()), 
                                UUID.randomUUID());
                    }
                }
                break;
                
            case "break":
                if (targetPos != null) {
                    this.setCurrentTask("break");
                    this.setTargetPos(targetPos);
                    if (this.getOwner() instanceof PlayerEntity) {
                        ((PlayerEntity) this.getOwner()).sendMessage(
                                new StringTextComponent("AI Companion will break block at " + 
                                        targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()), 
                                UUID.randomUUID());
                    }
                }
                break;
                
            case "place":
                if (targetPos != null && !item.isEmpty()) {
                    this.setCurrentTask("place");
                    this.setTargetPos(targetPos);
                    this.setHeldItem(item.copy());
                    if (this.getOwner() instanceof PlayerEntity) {
                        ((PlayerEntity) this.getOwner()).sendMessage(
                                new StringTextComponent("AI Companion will place " + item.getDisplayName().getString() + 
                                        " at " + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()), 
                                UUID.randomUUID());
                    }
                } else if (this.getOwner() instanceof PlayerEntity) {
                    ((PlayerEntity) this.getOwner()).sendMessage(
                            new StringTextComponent("Cannot place: No valid item specified"), UUID.randomUUID());
                }
                break;
                
            default:
                if (this.getOwner() instanceof PlayerEntity) {
                    ((PlayerEntity) this.getOwner()).sendMessage(
                            new StringTextComponent("Unknown command: " + command), UUID.randomUUID());
                }
                break;
        }
    }
}
