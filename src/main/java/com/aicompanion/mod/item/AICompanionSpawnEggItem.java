package com.aicompanion.mod.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.AbstractSpawner;

import java.util.Objects;
import java.util.function.Supplier;

public class AICompanionSpawnEggItem extends Item {
    private final Supplier<EntityType<?>> entityTypeSupplier;
    private final int primaryColor;
    private final int secondaryColor;

    public AICompanionSpawnEggItem(Supplier<EntityType<?>> entityTypeSupplier, int primaryColor, int secondaryColor, Properties properties) {
        super(properties);
        this.entityTypeSupplier = entityTypeSupplier;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        World world = context.getLevel();
        if (!(world instanceof ServerWorld)) {
            return ActionResultType.SUCCESS;
        }
        
        ItemStack itemstack = context.getItemInHand();
        BlockPos blockpos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockState blockstate = world.getBlockState(blockpos);

        if (blockstate.is(Blocks.SPAWNER)) {
            TileEntity tileentity = world.getBlockEntity(blockpos);
            if (tileentity instanceof MobSpawnerTileEntity) {
                AbstractSpawner abstractspawner = ((MobSpawnerTileEntity)tileentity).getSpawner();
                abstractspawner.setEntityId(this.entityTypeSupplier.get());
                tileentity.setChanged();
                world.sendBlockUpdated(blockpos, blockstate, blockstate, 3);
                itemstack.shrink(1);
                return ActionResultType.CONSUME;
            }
        }

        BlockPos spawnPos;
        if (blockstate.getCollisionShape(world, blockpos).isEmpty()) {
            spawnPos = blockpos;
        } else {
            spawnPos = blockpos.relative(direction);
        }

        EntityType<?> entitytype = this.entityTypeSupplier.get();
        if (entitytype.spawn((ServerWorld)world, itemstack, context.getPlayer(), spawnPos, SpawnReason.SPAWN_EGG, true, !Objects.equals(blockpos, spawnPos)) != null) {
            itemstack.shrink(1);
        }

        return ActionResultType.CONSUME;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (world.isClientSide) {
            return ActionResult.success(itemstack);
        } else {
            return ActionResult.consume(itemstack);
        }
    }

    public int getColor(int tintIndex) {
        return tintIndex == 0 ? this.primaryColor : this.secondaryColor;
    }
}
