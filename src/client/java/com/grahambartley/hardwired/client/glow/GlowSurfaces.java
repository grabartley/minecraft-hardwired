package com.grahambartley.hardwired.client.glow;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

interface GlowSurfaces {

  boolean isSolid(BlockPos pos);

  List<Box> partBoxes(BlockPos pos);

  float daylight(BlockPos pos);
}
