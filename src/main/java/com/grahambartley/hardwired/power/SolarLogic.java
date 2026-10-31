package com.grahambartley.hardwired.power;

public final class SolarLogic {

  public static final int CHECK_TICKS = 40;

  private static final int TICKS_PER_MINUTE = 1200;
  private static final int MAX_POWER = 15;
  private static final int DAY_POWER = 8;
  private static final float EASE_TO_NOON = 0.2F;
  private static final float FULL_TURN = (float) (Math.PI * 2);

  private SolarLogic() {}

  public static int daylight(int skyLight, int ambientDarkness, float skyAngleRadians) {
    int power = skyLight - ambientDarkness;
    if (power > 0) {
      float noon = skyAngleRadians < Math.PI ? 0F : FULL_TURN;
      float angle = skyAngleRadians + (noon - skyAngleRadians) * EASE_TO_NOON;
      power = Math.round(power * (float) Math.cos(angle));
    }
    return Math.clamp(power, 0, MAX_POWER);
  }

  public static boolean isDay(int power) {
    return power >= DAY_POWER;
  }

  public static int earned(int chargePerMinute, long worldTime, int ticks) {
    long phase = Math.floorMod(worldTime, (long) TICKS_PER_MINUTE);
    long before = chargePerMinute * phase / TICKS_PER_MINUTE;
    long after = chargePerMinute * (phase + ticks) / TICKS_PER_MINUTE;
    return (int) (after - before);
  }
}
