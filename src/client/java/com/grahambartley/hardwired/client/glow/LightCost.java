package com.grahambartley.hardwired.client.glow;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@FunctionalInterface
interface LightCost {

  int cost(BlockPos from, Direction side);
}
