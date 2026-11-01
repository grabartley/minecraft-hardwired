package com.grahambartley.hardwired.lift;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.NbtCompound;

public record LiftDial(
    List<Integer> levels, double fromLevel, double toLevel, int pace, long startTick) {

  public static final LiftDial NONE = at(List.of(), 0);

  public static final int MAX_MARKS = 9;

  private static final float SWEEP_DEGREES = 70F;
  private static final String LEVELS = "dial_levels";
  private static final String FROM = "dial_from";
  private static final String TO = "dial_to";
  private static final String PACE = "dial_pace";
  private static final String START = "dial_start";

  public static LiftDial at(List<Integer> levels, double level) {
    return new LiftDial(levels, level, level, 0, 0);
  }

  public static LiftDial riding(List<Integer> levels, LiftTrip trip, long startTick) {
    return new LiftDial(levels, trip.fromLevel(), trip.toLevel(), trip.ticksPerBlock(), startTick);
  }

  public double levelAt(double time) {
    if (pace == 0) {
      return toLevel;
    }
    LiftTrip trip = new LiftTrip(fromLevel, toLevel, pace);
    double elapsed = Math.max(0, time - startTick);
    int whole = (int) elapsed;
    double before = trip.levelAt(whole);
    return before + (trip.levelAt(whole + 1) - before) * (elapsed - whole);
  }

  public double reading(double level) {
    int last = levels.size() - 1;
    if (last < 1 || level <= levels.getFirst()) {
      return 0;
    }
    for (int floor = 0; floor < last; floor++) {
      int below = levels.get(floor);
      int above = levels.get(floor + 1);
      if (level < above) {
        return (floor + (level - below) / (above - below)) / last;
      }
    }
    return 1;
  }

  public List<Float> markDegrees() {
    List<Float> marks = new ArrayList<>();
    if (levels.size() > 1 && levels.size() <= MAX_MARKS) {
      levels.forEach(level -> marks.add(degrees(reading(level))));
    }
    return marks;
  }

  public static float degrees(double reading) {
    return SWEEP_DEGREES - 2 * SWEEP_DEGREES * (float) reading;
  }

  public void write(NbtCompound nbt) {
    nbt.putIntArray(LEVELS, levels);
    nbt.putDouble(FROM, fromLevel);
    nbt.putDouble(TO, toLevel);
    nbt.putInt(PACE, pace);
    nbt.putLong(START, startTick);
  }

  public static void clear(NbtCompound nbt) {
    List.of(LEVELS, FROM, TO, PACE, START).forEach(nbt::remove);
  }

  public static LiftDial read(NbtCompound nbt) {
    return new LiftDial(
        Arrays.stream(nbt.getIntArray(LEVELS)).boxed().toList(),
        nbt.getDouble(FROM),
        nbt.getDouble(TO),
        nbt.getInt(PACE),
        nbt.getLong(START));
  }
}
