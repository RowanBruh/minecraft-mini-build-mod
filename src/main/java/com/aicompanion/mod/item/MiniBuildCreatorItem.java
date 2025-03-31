package com.aicompanion.mod.item;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.entity.MiniBuildEntity;
import com.aicompanion.mod.init.ModEntities;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;

public class MiniBuildCreatorItem extends Item {
    
    public MiniBuildCreatorItem(Item.Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(new StringTextComponent("Creates a miniature replica of a structure").withStyle(TextFormatting.GRAY));
        tooltip.add(new StringTextComponent("Right-click to set the first corner").withStyle(TextFormatting.GRAY));
        tooltip.add(new StringTextComponent("Shift-right-click to set the second corner").withStyle(TextFormatting.GRAY));
        
        CompoundNBT nbt = stack.getOrCreateTag();
        if (nbt.contains("FirstCorner")) {
            CompoundNBT corner = nbt.getCompound("FirstCorner");
            tooltip.add(new StringTextComponent("First corner: " + 
                    corner.getInt("X") + ", " + 
                    corner.getInt("Y") + ", " + 
                    corner.getInt("Z")).withStyle(TextFormatting.GREEN));
        }
    }
    
    @Override
    public ActionResultType useOn(ItemUseContext context) {
        World world = context.getLevel();
        if (world.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResultType.FAIL;
        }
        
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        
        // If the clicked face is not the top face, adjust the position
        if (context.getClickedFace() != Direction.UP) {
            clickedPos = clickedPos.relative(context.getClickedFace());
        }
        
        CompoundNBT nbt = stack.getOrCreateTag();
        
        if (player.isShiftKeyDown()) {
            // Set second corner and create the replica
            if (!nbt.contains("FirstCorner")) {
                player.sendMessage(new StringTextComponent("You need to set the first corner first!"), player.getUUID());
                return ActionResultType.FAIL;
            }
            
            CompoundNBT firstCorner = nbt.getCompound("FirstCorner");
            BlockPos firstPos = new BlockPos(
                    firstCorner.getInt("X"),
                    firstCorner.getInt("Y"),
                    firstCorner.getInt("Z"));
            
            // Calculate structure dimensions
            BlockPos dimensions = calculateDimensions(firstPos, clickedPos);
            int width = dimensions.getX();
            int height = dimensions.getY();
            int depth = dimensions.getZ();
            
            // Check if the structure is too large
            final int MAX_SIZE = 32;
            if (width > MAX_SIZE || height > MAX_SIZE || depth > MAX_SIZE) {
                player.sendMessage(new StringTextComponent(
                        "Structure is too large! Maximum size is " + MAX_SIZE + " blocks in any dimension."), 
                        player.getUUID());
                return ActionResultType.FAIL;
            }
            
            // Calculate the origin (minimum corner)
            BlockPos origin = new BlockPos(
                    Math.min(firstPos.getX(), clickedPos.getX()),
                    Math.min(firstPos.getY(), clickedPos.getY()),
                    Math.min(firstPos.getZ(), clickedPos.getZ()));
            
            // Create the mini build entity
            EntityType<MiniBuildEntity> entityType = ModEntities.MINI_BUILD.get();
            MiniBuildEntity miniBuild = entityType.create((ServerWorld)world, null, null, player, origin, SpawnReason.SPAWN_EGG);
            
            if (miniBuild != null) {
                // Position the entity on top of the block
                miniBuild.setPos(clickedPos.getX() + 0.5, clickedPos.getY() + 0.5, clickedPos.getZ() + 0.5);
                
                // Capture the structure details in the entity
                miniBuild.captureStructure(player, origin, width, height, depth);
                
                // Add the entity to the world
                world.addFreshEntity(miniBuild);
                
                player.sendMessage(new StringTextComponent(
                        "Created a miniature replica of structure: " + width + "x" + height + "x" + depth), 
                        player.getUUID());
                
                // Clear the first corner
                nbt.remove("FirstCorner");
            } else {
                player.sendMessage(new StringTextComponent("Failed to create mini build entity!"), player.getUUID());
                return ActionResultType.FAIL;
            }
        } else {
            // Set first corner
            CompoundNBT firstCorner = new CompoundNBT();
            firstCorner.putInt("X", clickedPos.getX());
            firstCorner.putInt("Y", clickedPos.getY());
            firstCorner.putInt("Z", clickedPos.getZ());
            nbt.put("FirstCorner", firstCorner);
            
            player.sendMessage(new StringTextComponent(
                    "First corner set at: " + clickedPos.getX() + ", " + clickedPos.getY() + ", " + clickedPos.getZ() + 
                    ". Now shift-right-click to set the second corner."), 
                    player.getUUID());
        }
        
        return ActionResultType.SUCCESS;
    }
    
    /**
     * Calculate the dimensions of a structure from two corners
     */
    private BlockPos calculateDimensions(BlockPos pos1, BlockPos pos2) {
        return new BlockPos(
                Math.abs(pos1.getX() - pos2.getX()) + 1,
                Math.abs(pos1.getY() - pos2.getY()) + 1,
                Math.abs(pos1.getZ() - pos2.getZ()) + 1);
    }
}