package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.wire.LampLook;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface PanelDevice {

  PanelRow read(ServerWorld world, BlockPos pos, BlockState state);

  boolean work(
      ServerWorld world, BlockPos pos, BlockState state, PanelAction action, LampLook look);

  default BlockPos anchor(BlockPos pos, BlockState state) {
    return pos;
  }
}
