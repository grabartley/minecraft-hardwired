package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.wire.Hue;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface WireDevice {

  default boolean isDriving(ServerWorld world, BlockPos pos, BlockState state, Hue color) {
    return false;
  }

  default boolean holdsLook() {
    return false;
  }

  default void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {}
}
