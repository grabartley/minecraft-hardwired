package com.grahambartley.hardwired.block;

import net.minecraft.util.math.MathHelper;

public final class DoorSwing {

  public static final int SWING_TICKS = 10;

  private DoorSwing() {}

  public static float openness(boolean open, float ticksSinceSwing) {
    float travelled = MathHelper.clamp(ticksSinceSwing / SWING_TICKS, 0F, 1F);
    float eased = travelled * travelled * (3F - 2F * travelled);
    return open ? eased : 1F - eased;
  }
}
