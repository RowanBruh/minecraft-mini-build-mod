package com.aicompanion.mod.entity;

import com.aicompanion.mod.AICompanionMod;
import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The main entity class for Mini Build structures
 * This entity represents a miniature version of a structure
 */
public class MiniBuildEntity extends Entity {
    
    // Structure data
    private String ownerUUID = "";
    private BlockPos originPos = BlockPos.ZERO;
    private int width = 0;
    private int height = 0; 
    private int depth = 0;
    private List<MiniBlock> miniBlocks = new ArrayList<>();
    
    // Interaction flags
    private static final DataParameter<Boolean> WALLS_VISIBLE = EntityDataManager.defineId(MiniBuildEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> GIANT_PLAYER_VISIBLE = EntityDataManager.defineId(MiniBuildEntity.class, DataSerializers.BOOLEAN);
    
    // Interaction cooldowns
    private long lastInteractionTime = 0;
    private static final long INTERACTION_COOLDOWN = 250; // ms
    
    public MiniBuildEntity(EntityType<? extends MiniBuildEntity> entityType, World world) {
        super(entityType, world);
        this.noCulling = true; // Make sure the entity is always rendered
    }
    
    @Override
    protected void defineSynchedData() {
        this.entityData.define(WALLS_VISIBLE, true);
        this.entityData.define(GIANT_PLAYER_VISIBLE, false);
    }
    
    /**
     * Capture a structure in the world to create this mini build
     */
    public void captureStructure(PlayerEntity player, BlockPos origin, int width, int height, int depth) {
        this.ownerUUID = player.getUUID().toString();
        this.originPos = origin;
        this.width = width;
        this.height = height;
        this.depth = depth;
        
        // Clear existing blocks
        this.miniBlocks.clear();
        
        // Capture all blocks in the structure
        World world = player.level;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos worldPos = origin.offset(x, y, z);
                    BlockState blockState = world.getBlockState(worldPos);
                    
                    // Skip air blocks
                    if (blockState.isAir()) {
                        continue;
                    }
                    
                    MiniBlock miniBlock = new MiniBlock();
                    miniBlock.setRelativePos(new BlockPos(x, y, z));
                    miniBlock.setBlockState(blockState);
                    
                    this.miniBlocks.add(miniBlock);
                }
            }
        }
    }
    
    /**
     * Update the real structure based on changes to the mini structure
     */
    public void updateRealStructure(BlockPos pos, BlockState state) {
        if (this.level.isClientSide || this.ownerUUID.isEmpty()) {
            return;
        }
        
        PlayerEntity owner = getOwner();
        if (owner == null) {
            return;
        }
        
        // Calculate the real-world position
        BlockPos realPos = this.originPos.offset(pos.getX(), pos.getY(), pos.getZ());
        
        // Update the block in the real world
        this.level.setBlock(realPos, state, 3);
        
        // Update our internal list
        for (MiniBlock miniBlock : this.miniBlocks) {
            if (miniBlock.getRelativePos().equals(pos)) {
                miniBlock.setBlockState(state);
                break;
            }
        }
    }
    
    /**
     * Update the mini structure based on changes to the real structure
     */
    public void updateMiniStructure() {
        if (this.level.isClientSide || this.ownerUUID.isEmpty()) {
            return;
        }
        
        // Clear the existing blocks
        this.miniBlocks.clear();
        
        // Re-capture all blocks in the structure
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos worldPos = this.originPos.offset(x, y, z);
                    BlockState blockState = this.level.getBlockState(worldPos);
                    
                    // Skip air blocks
                    if (blockState.isAir()) {
                        continue;
                    }
                    
                    MiniBlock miniBlock = new MiniBlock();
                    miniBlock.setRelativePos(new BlockPos(x, y, z));
                    miniBlock.setBlockState(blockState);
                    
                    this.miniBlocks.add(miniBlock);
                }
            }
        }
    }
    
    /**
     * Handle player interactions with the mini build
     */
    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        if (this.level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        
        // Throttle interactions to prevent spam
        long currentTime = Util.getMillis();
        if (currentTime - this.lastInteractionTime < INTERACTION_COOLDOWN) {
            return ActionResultType.PASS;
        }
        this.lastInteractionTime = currentTime;
        
        // Check if the player is the owner of this structure
        if (!this.ownerUUID.isEmpty() && !this.ownerUUID.equals(player.getUUID().toString())) {
            player.sendMessage(new StringTextComponent("You can only interact with your own mini builds!"), player.getUUID());
            return ActionResultType.FAIL;
        }
        
        // Handle sneaking interactions (toggle special features)
        if (player.isShiftKeyDown()) {
            // Toggle between wall visibility modes with shift-right-click
            if (player.getItemInHand(hand).isEmpty()) {
                boolean newState = !isWallsVisible();
                setWallsVisible(newState);
                player.sendMessage(new StringTextComponent("Walls are now " + (newState ? "visible" : "invisible")), player.getUUID());
                return ActionResultType.SUCCESS;
            }
        }
        // Toggle giant player in the sky
        else if (player.getMainHandItem().isEmpty()) {
            boolean newState = !isGiantPlayerVisible();
            setGiantPlayerVisible(newState);
            player.sendMessage(new StringTextComponent("Giant player is now " + (newState ? "visible" : "invisible")), player.getUUID());
            return ActionResultType.SUCCESS;
        }
        
        // When holding an item, modify the structure
        if (!player.getMainHandItem().isEmpty()) {
            // TODO: Implement this to handle placing blocks in the mini structure
            return ActionResultType.SUCCESS;
        }
        
        return ActionResultType.PASS;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Prevent the entity from moving or being affected by gravity
        this.setDeltaMovement(0, 0, 0);
        
        // Synchronize with the real world structure periodically
        if (!this.level.isClientSide && this.tickCount % 100 == 0) {
            updateMiniStructure();
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundNBT compound) {
        // Read owner info
        if (compound.contains("OwnerUUID")) {
            this.ownerUUID = compound.getString("OwnerUUID");
        }
        
        // Read structure dimensions
        this.width = compound.getInt("Width");
        this.height = compound.getInt("Height");
        this.depth = compound.getInt("Depth");
        
        // Read origin position
        int originX = compound.getInt("OriginX");
        int originY = compound.getInt("OriginY");
        int originZ = compound.getInt("OriginZ");
        this.originPos = new BlockPos(originX, originY, originZ);
        
        // Read block data
        if (compound.contains("Blocks", Constants.NBT.TAG_LIST)) {
            this.miniBlocks.clear();
            
            ListNBT blocksList = compound.getList("Blocks", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < blocksList.size(); i++) {
                CompoundNBT blockNBT = blocksList.getCompound(i);
                MiniBlock miniBlock = new MiniBlock();
                miniBlock.readFromNBT(blockNBT);
                this.miniBlocks.add(miniBlock);
            }
        }
        
        // Read interaction flags
        this.entityData.set(WALLS_VISIBLE, compound.getBoolean("WallsVisible"));
        this.entityData.set(GIANT_PLAYER_VISIBLE, compound.getBoolean("GiantPlayerVisible"));
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundNBT compound) {
        // Write owner info
        compound.putString("OwnerUUID", this.ownerUUID);
        
        // Write structure dimensions
        compound.putInt("Width", this.width);
        compound.putInt("Height", this.height);
        compound.putInt("Depth", this.depth);
        
        // Write origin position
        compound.putInt("OriginX", this.originPos.getX());
        compound.putInt("OriginY", this.originPos.getY());
        compound.putInt("OriginZ", this.originPos.getZ());
        
        // Write block data
        ListNBT blocksList = new ListNBT();
        for (MiniBlock miniBlock : this.miniBlocks) {
            CompoundNBT blockNBT = new CompoundNBT();
            miniBlock.writeToNBT(blockNBT);
            blocksList.add(blockNBT);
        }
        compound.put("Blocks", blocksList);
        
        // Write interaction flags
        compound.putBoolean("WallsVisible", this.entityData.get(WALLS_VISIBLE));
        compound.putBoolean("GiantPlayerVisible", this.entityData.get(GIANT_PLAYER_VISIBLE));
    }
    
    public String getOwnerUUID() {
        return this.ownerUUID;
    }
    
    public PlayerEntity getOwner() {
        if (this.ownerUUID.isEmpty()) {
            return null;
        }
        
        try {
            UUID uuid = UUID.fromString(this.ownerUUID);
            return this.level.getPlayerByUUID(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public boolean isWallsVisible() {
        return this.entityData.get(WALLS_VISIBLE);
    }
    
    public void setWallsVisible(boolean visible) {
        this.entityData.set(WALLS_VISIBLE, visible);
    }
    
    public boolean isGiantPlayerVisible() {
        return this.entityData.get(GIANT_PLAYER_VISIBLE);
    }
    
    public void setGiantPlayerVisible(boolean visible) {
        this.entityData.set(GIANT_PLAYER_VISIBLE, visible);
    }
    
    /**
     * Get the list of mini blocks in this structure
     */
    public List<MiniBlock> getMiniBlocks() {
        return this.miniBlocks;
    }
    
    /**
     * Class to store information about a block in the mini structure
     */
    public static class MiniBlock {
        private BlockPos relativePos;
        private BlockState blockState;
        
        public BlockPos getRelativePos() {
            return relativePos;
        }
        
        public void setRelativePos(BlockPos pos) {
            this.relativePos = pos;
        }
        
        public BlockState getBlockState() {
            return blockState;
        }
        
        public void setBlockState(BlockState state) {
            this.blockState = state;
        }
        
        public void readFromNBT(CompoundNBT nbt) {
            int x = nbt.getInt("X");
            int y = nbt.getInt("Y");
            int z = nbt.getInt("Z");
            this.relativePos = new BlockPos(x, y, z);
            
            // Read block state (simplified for demonstration)
            String blockName = nbt.getString("Block");
            // In a full implementation, you would need to deserialize the full BlockState
        }
        
        public void writeToNBT(CompoundNBT nbt) {
            nbt.putInt("X", relativePos.getX());
            nbt.putInt("Y", relativePos.getY());
            nbt.putInt("Z", relativePos.getZ());
            
            // Write block state (simplified for demonstration)
            if (blockState != null) {
                nbt.putString("Block", blockState.getBlock().getRegistryName().toString());
                // In a full implementation, you would need to serialize the full BlockState
            }
        }
    }
}