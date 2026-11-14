package com.grahambartley.hardwired.line;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.CopperCare;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireSpan;
import java.util.List;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;

public final class SpanCare {

  static final int CHECK_TICKS = 20;
  private static final float BLOCKS_PER_SECTION = 4096F;

  private SpanCare() {}

  public static float ageChance(int randomTickSpeed, int ratePercent) {
    return randomTickSpeed * CHECK_TICKS / BLOCKS_PER_SECTION * CopperCare.ageChance(ratePercent);
  }

  public static void tick(ServerWorld world) {
    if (world.getTime() % CHECK_TICKS != 0) {
      return;
    }
    float chance =
        ageChance(
            world.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED),
            Hardwired.SERVER_CONFIG.lamp().ageRateMultiplierPercent());
    WireEngine engine = WireEngine.of(world);
    for (WireSpan span : List.copyOf(engine.spans())) {
      if (span.canAge() && world.random.nextFloat() < chance) {
        engine.restyleSpan(span, span.older());
      }
    }
  }
}
