package com.grahambartley.hardwired.controller;

import net.minecraft.nbt.NbtCompound;

public final class TimelineRunNbt {

  private static final String POSITION = "position";
  private static final String TRAVEL = "travel";
  private static final String TICKS_LEFT = "ticks_left";
  private static final String PENDING = "pending";
  private static final String REVERSE_LEFT = "reverse_left";

  private TimelineRunNbt() {}

  public static NbtCompound write(TimelineRun run) {
    NbtCompound nbt = new NbtCompound();
    nbt.putInt(POSITION, run.position());
    nbt.putInt(TICKS_LEFT, run.ticksLeft());
    nbt.putInt(REVERSE_LEFT, run.reverseLeft());
    if (run.travel() != null) {
      nbt.putString(TRAVEL, run.travel().name());
    }
    if (run.pending() != null) {
      nbt.putString(PENDING, run.pending().name());
    }
    return nbt;
  }

  public static TimelineRun read(NbtCompound nbt, Timeline timeline) {
    return TimelineRun.restored(
        timeline,
        nbt.getInt(POSITION),
        orderOf(nbt.getString(TRAVEL)),
        nbt.getInt(TICKS_LEFT),
        orderOf(nbt.getString(PENDING)),
        nbt.getInt(REVERSE_LEFT));
  }

  private static TimelineOrder orderOf(String name) {
    for (TimelineOrder order : TimelineOrder.values()) {
      if (order.name().equals(name)) {
        return order;
      }
    }
    return null;
  }
}
