package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.wire.Hue;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface WireDevice {

  default boolean isDriving(ServerWorld world, BlockPos pos, BlockState state, Hue color) {
    return false;
  }

  default boolean connectsFrom(BlockState state, Direction side) {
    return true;
  }

  default boolean isDrivingFrom(
      ServerWorld world, BlockPos pos, BlockState state, Hue color, Direction side) {
    return connectsFrom(state, side) && isDriving(world, pos, state, color);
  }

  default boolean holdsLook() {
    return false;
  }

  default void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {}

  default int load(ServerWorld world, BlockPos pos, BlockState state) {
    return 0;
  }

  default BlockPos loadAnchor(BlockPos pos, BlockState state) {
    return pos;
  }
}
