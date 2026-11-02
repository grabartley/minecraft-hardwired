package com.grahambartley.hardwired.lift;

import net.minecraft.util.math.Direction;

public final class LiftCarTurn {

  private static final int WHOLE_TURN = 4;

  private LiftCarTurn() {}

  public static int quarterTurns(Direction gateFacing) {
    return (WHOLE_TURN - gateFacing.getHorizontal()) % WHOLE_TURN;
  }
}
