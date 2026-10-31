package com.grahambartley.hardwired.lift;

import net.minecraft.util.math.Direction;

public final class LiftCarTurn {

  private LiftCarTurn() {}

  public static int quarterTurns(Direction gateFacing) {
    return gateFacing.getAxis() == Direction.Axis.X ? 1 : 0;
  }
}
