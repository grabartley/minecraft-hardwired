package com.grahambartley.hardwired.radio;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public record Station(
    BlockPos desk,
    BlockPos towerFoot,
    int frequency,
    int reach,
    long onAirSince,
    @Nullable Identifier song,
    long songStart) {

  public double flatDistanceTo(BlockPos pos) {
    return Math.hypot(pos.getX() - towerFoot.getX(), pos.getZ() - towerFoot.getZ());
  }
}
