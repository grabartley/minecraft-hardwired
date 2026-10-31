package com.grahambartley.hardwired.lift;

public record LiftTrip(double fromLevel, double toLevel, int ticksPerBlock) {

  private static final int EASE_TICKS = 8;
  private static final double WHOLE_BLOCK_MARGIN = 1.0E-9;

  public double distance() {
    return Math.abs(toLevel - fromLevel);
  }

  public int blocks() {
    return (int) Math.ceil(distance() - WHOLE_BLOCK_MARGIN);
  }

  public int totalTicks() {
    return Math.max(1, (int) Math.ceil(distance() * ticksPerBlock));
  }

  public int direction() {
    return (int) Math.signum(toLevel - fromLevel);
  }

  public boolean arrivedBy(int ticks) {
    return ticks >= totalTicks();
  }

  public double levelAt(int ticks) {
    return fromLevel + direction() * travelled(ticks);
  }

  public int blocksStartedBy(int ticks) {
    if (ticks <= 0) {
      return 0;
    }
    return Math.min(blocks(), (int) Math.floor(travelled(ticks) + WHOLE_BLOCK_MARGIN) + 1);
  }

  public double endOfBlock(int block) {
    double reach = Math.min(distance(), block);
    return fromLevel + direction() * reach;
  }

  private double travelled(int ticks) {
    int total = totalTicks();
    if (ticks >= total) {
      return distance();
    }
    int ease = Math.min(EASE_TICKS, total / 2);
    if (ticks <= 0) {
      return 0;
    }
    if (ease == 0) {
      return distance() * ticks / total;
    }
    double cruise = distance() / (total - ease);
    if (ticks < ease) {
      return cruise * ticks * ticks / (2.0 * ease);
    }
    if (ticks <= total - ease) {
      return cruise * (ticks - ease / 2.0);
    }
    int left = total - ticks;
    return distance() - cruise * left * left / (2.0 * ease);
  }
}
