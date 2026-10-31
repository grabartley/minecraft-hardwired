package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import java.util.function.BiPredicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public final class SensorBlock extends MountedDeviceBlock {

  public static final int CHECK_INTERVAL = 10;

  private final BiPredicate<ServerWorld, BlockPos> sense;

  public SensorBlock(
      Settings settings, double[] floorBox, BiPredicate<ServerWorld, BlockPos> sense) {
    super(settings, floorBox);
    this.sense = sense;
  }

  public static boolean livingEntityNearby(ServerWorld world, BlockPos pos) {
    return !world
        .getEntitiesByClass(
            LivingEntity.class,
            new Box(pos).expand(Hardwired.SERVER_CONFIG.device().motionSensorRangeBlocks()),
            EntityPredicates.EXCEPT_SPECTATOR)
        .isEmpty();
  }

  @Override
  protected void onBlockAdded(
      BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (!world.isClient && !oldState.isOf(this)) {
      world.scheduleBlockTick(pos, this, CHECK_INTERVAL);
    }
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    boolean sensed = sense.test(world, pos);
    if (sensed != state.get(POWERED)) {
      world.setBlockState(pos, state.with(POWERED, sensed), NOTIFY_ALL);
    }
    world.scheduleBlockTick(pos, this, CHECK_INTERVAL);
  }
}
