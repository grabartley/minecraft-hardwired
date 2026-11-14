package com.grahambartley.hardwired.phone;

import java.util.List;

public final class RingCadence {

  public static final int CYCLE_TICKS = 120;
  public static final int STRIKE_TICKS = 3;
  private static final float[] GONG_PITCHES = {1F, 1.19F};

  private record Stroke(int start, int length) {

    boolean covers(int tick) {
      return tick >= start && tick < start + length;
    }
  }

  private static final List<Stroke> SINGLE = List.of(new Stroke(0, 40));
  private static final List<Stroke> PARTY =
      List.of(new Stroke(0, 24), new Stroke(32, 8), new Stroke(48, 8));

  private RingCadence() {}

  public static boolean ringing(long ticks, boolean party) {
    int tick = (int) Math.floorMod(ticks, CYCLE_TICKS);
    return strokes(party).stream().anyMatch(stroke -> stroke.covers(tick));
  }

  public static int strokeStarting(long ticks, boolean party) {
    int tick = (int) Math.floorMod(ticks, CYCLE_TICKS);
    return strokes(party).stream()
        .filter(stroke -> stroke.start() == tick)
        .mapToInt(Stroke::length)
        .findFirst()
        .orElse(0);
  }

  public static float strikePitch(long ticks, boolean party) {
    return ringing(ticks, party) ? gongPitch(ticks) : 0F;
  }

  private static float gongPitch(long ticks) {
    if (Math.floorMod(ticks, STRIKE_TICKS) != 0) {
      return 0F;
    }
    return GONG_PITCHES[(int) Math.floorMod(ticks / STRIKE_TICKS, GONG_PITCHES.length)];
  }

  private static List<Stroke> strokes(boolean party) {
    return party ? PARTY : SINGLE;
  }
}
