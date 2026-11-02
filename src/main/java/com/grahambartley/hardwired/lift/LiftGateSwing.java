package com.grahambartley.hardwired.lift;

public final class LiftGateSwing {

  public static final int TICKS = 10;

  private static final float FOLDED_WIDTH = 0.16F;

  private boolean known;
  private boolean open;
  private double changedAt;

  public float width(boolean nowOpen, double time) {
    if (!known) {
      known = true;
      open = nowOpen;
      changedAt = time - TICKS;
    } else if (open != nowOpen) {
      open = nowOpen;
      changedAt = time;
    }
    double progress = Math.min(1, Math.max(0, (time - changedAt) / TICKS));
    double eased = progress * progress * (3 - 2 * progress);
    double opened = open ? eased : 1 - eased;
    return (float) (1 - (1 - FOLDED_WIDTH) * opened);
  }
}
