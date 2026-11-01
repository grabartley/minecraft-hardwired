package com.grahambartley.hardwired.client.anim;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.entity.BlockEntity;

final class DevicePhase {

  private static final float TICKS_PER_SECOND = 20F;
  private static final float SETTLE = 0.04F;

  private static final Map<BlockEntity, Phase> PHASES = new WeakHashMap<>();

  static final class Phase {
    private float seconds;
    private float speed;
    private double lastTick = Double.NaN;

    float advance(float targetSpeed, double now) {
      if (!Double.isNaN(lastTick) && now > lastTick) {
        float ticks = (float) (now - lastTick);
        speed += (targetSpeed - speed) * Math.min(1F, ticks * SETTLE);
        seconds += ticks / TICKS_PER_SECOND * speed;
      }
      lastTick = now;
      return seconds;
    }
  }

  private DevicePhase() {}

  static float seconds(BlockEntity entity, float speed, float tickDelta) {
    return PHASES
        .computeIfAbsent(entity, ignored -> new Phase())
        .advance(speed, entity.getWorld().getTime() + (double) tickDelta);
  }
}
