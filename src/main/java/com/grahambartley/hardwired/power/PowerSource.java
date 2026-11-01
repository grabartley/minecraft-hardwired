package com.grahambartley.hardwired.power;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface PowerSource {

  SourceKind sourceKind();

  boolean isFuelled(ServerWorld world, BlockPos pos, BlockState state);
}
