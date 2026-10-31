package com.grahambartley.hardwired.device;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface Electronic {

  DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state);

  int chargePerSecond();

  void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status);

  default void beat(ServerWorld world, BlockPos pos) {}

  default boolean keepsChecking(DeviceStatus status) {
    return status.wantsCharge();
  }
}
