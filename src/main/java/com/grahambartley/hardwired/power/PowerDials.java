package com.grahambartley.hardwired.power;

import net.minecraft.util.math.MathHelper;

public final class PowerDials {

  private static final float IDLE_SPEED = 0.35F;
  private static final float EXTRA_SPEED = 1.65F;
  private static final float NEEDLE_REST = 0.15F;
  private static final float NEEDLE_RANGE = 0.75F;
  private static final float NEEDLE_FLICKER = 0.03F;
  private static final float NEEDLE_SPEED = 0.7F;
  private static final float THROW_TICKS = StartOrder.START_TICKS;
  private static final float DAWN_ANGLE = 0.75F;
  private static final float QUARTER = 0.25F;
  private static final float SKY_ARC = 2F;
  private static final float SUN_EASE = 0.01F;

  private PowerDials() {}

  public static float gauge(int workload) {
    return workload / (float) PowerWear.MAX_WORKLOAD;
  }

  public static float clipSpeed(boolean running, int workload) {
    return running ? IDLE_SPEED + EXTRA_SPEED * gauge(workload) : 0F;
  }

  public static float sunPhase(float skyAngle) {
    float climbed = skyAngle > DAWN_ANGLE ? skyAngle - DAWN_ANGLE : skyAngle + QUARTER;
    return Math.clamp(climbed * SKY_ARC, 0F, 1F);
  }

  public static float easeSun(float current, float target, boolean running, float ticks) {
    if (!running) {
      return current;
    }
    float step = Math.max(ticks, 0F) * SUN_EASE;
    return current + Math.clamp(target - current, -step, step);
  }

  public static float needle(boolean live, int workload, float time) {
    if (!live) {
      return 0F;
    }
    float flicker = MathHelper.sin(time * NEEDLE_SPEED) * NEEDLE_FLICKER;
    return Math.clamp(NEEDLE_REST + gauge(workload) * NEEDLE_RANGE + flicker, 0F, 1F);
  }

  public static float thrown(boolean closed, float ticksSinceThrow) {
    float travelled = Math.clamp(ticksSinceThrow / THROW_TICKS, 0F, 1F);
    return closed ? travelled : 1F - travelled;
  }
}
