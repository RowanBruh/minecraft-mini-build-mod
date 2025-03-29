package com.aicompanion.mod.entity.ai.goal;

import com.aicompanion.mod.entity.AICompanionEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

public class FollowOwnerGoal extends Goal {
    private final AICompanionEntity companion;
    private LivingEntity owner;
    private final World world;
    private final double speedModifier;
    private final PathNavigator navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;
    private float oldWaterCost;
    private final boolean canFly;

    public FollowOwnerGoal(AICompanionEntity companion, double speed, float startDist, float stopDist, boolean canFly) {
        this.companion = companion;
        this.world = companion.level;
        this.speedModifier = speed;
        this.navigation = companion.getNavigation();
        this.startDistance = startDist;
        this.stopDistance = stopDist;
        this.canFly = canFly;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.companion.getOwner();
        if (livingentity == null) {
            return false;
        } else if (livingentity.isSpectator()) {
            return false;
        } else if (!this.companion.isActive()) {
            return false;
        } else if (!this.companion.getCurrentTask().equals("follow")) {
            return false;
        } else if (this.companion.distanceToSqr(livingentity) < (double)(this.startDistance * this.startDistance)) {
            return false;
        } else {
            this.owner = livingentity;
            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.navigation.isDone()) {
            return false;
        } else if (!this.companion.isActive()) {
            return false;
        } else if (!this.companion.getCurrentTask().equals("follow")) {
            return false;
        } else {
            return !(this.companion.distanceToSqr(this.owner) <= (double)(this.stopDistance * this.stopDistance));
        }
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.companion.getPathfindingMalus(PathNodeType.WATER);
        this.companion.setPathfindingMalus(PathNodeType.WATER, 0.0F);
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.companion.setPathfindingMalus(PathNodeType.WATER, this.oldWaterCost);
    }

    @Override
    public void tick() {
        this.companion.getLookControl().setLookAt(this.owner, 10.0F, (float)this.companion.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.companion.isLeashed() && !this.companion.isPassenger()) {
                if (this.companion.distanceToSqr(this.owner) >= 144.0D) {
                    this.teleportToOwner();
                } else {
                    this.navigation.moveTo(this.owner, this.speedModifier);
                }
            }
        }
    }

    private void teleportToOwner() {
        BlockPos blockpos = this.owner.blockPosition();

        for(int i = 0; i < 10; ++i) {
            int j = this.randomIntInclusive(-3, 3);
            int k = this.randomIntInclusive(-1, 1);
            int l = this.randomIntInclusive(-3, 3);
            boolean flag = this.tryToTeleportToLocation(blockpos.getX() + j, blockpos.getY() + k, blockpos.getZ() + l);
            if (flag) {
                return;
            }
        }
    }

    private boolean tryToTeleportToLocation(int x, int y, int z) {
        if (Math.abs(x - this.owner.getX()) < 2.0D && Math.abs(z - this.owner.getZ()) < 2.0D) {
            return false;
        } else if (!this.isValidTeleportSpot(new BlockPos(x, y, z))) {
            return false;
        } else {
            this.companion.moveTo((double)x + 0.5D, (double)y, (double)z + 0.5D, this.companion.yRot, this.companion.xRot);
            this.navigation.stop();
            return true;
        }
    }

    private boolean isValidTeleportSpot(BlockPos pos) {
        PathNodeType pathnodetype = this.navigation.getNodeEvaluator().getBlockPathType(this.world, pos.getX(), pos.getY(), pos.getZ());
        if (pathnodetype != PathNodeType.WALKABLE) {
            return false;
        } else {
            BlockPos blockpos = pos.subtract(this.companion.blockPosition());
            return this.world.noCollision(this.companion, this.companion.getBoundingBox().move(blockpos));
        }
    }

    private int randomIntInclusive(int min, int max) {
        return this.companion.getRandom().nextInt(max - min + 1) + min;
    }
}
