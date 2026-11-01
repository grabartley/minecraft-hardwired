package com.grahambartley.hardwired.client.glow;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

interface GlowSurfaces extends LightCost {

  boolean isSolid(BlockPos pos);

  List<Box> partBoxes(BlockPos pos);

  boolean showsSide(BlockPos pos, Direction side);

  float daylight(BlockPos pos);
}
