package com.grahambartley.hardwired.client.anim;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.entity.BlockEntity;

final class DevicePhase {

  private static final float TICKS_PER_SECOND = 20F;
  private static final float SETTLE = 0.04F;

  private static final Map<BlockEntity, Phase> PHASES = new WeakHashMap<>();

  private static final class Phase {
    private float seconds;
    private float speed;
    private double lastTick = Double.NaN;
  }

  private DevicePhase() {}

  static float seconds(BlockEntity entity, float speed, float tickDelta) {
    Phase phase = PHASES.computeIfAbsent(entity, ignored -> new Phase());
    double now = entity.getWorld().getTime() + (double) tickDelta;
    if (!Double.isNaN(phase.lastTick) && now > phase.lastTick) {
      float ticks = (float) (now - phase.lastTick);
      phase.speed += (speed - phase.speed) * Math.min(1F, ticks * SETTLE);
      phase.seconds += ticks / TICKS_PER_SECOND * phase.speed;
    }
    phase.lastTick = now;
    return phase.seconds;
  }
}
