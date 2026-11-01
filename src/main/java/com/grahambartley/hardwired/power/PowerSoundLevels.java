package com.grahambartley.hardwired.power;

public final class PowerSoundLevels {

  private static final float LOWEST_PITCH = 0.7F;
  private static final float PITCH_RISE = 0.9F;
  private static final float QUIETEST_LOOP = 0.25F;
  private static final float LOOP_RISE = 0.55F;
  private static final float PERCENT = 100F;

  private PowerSoundLevels() {}

  public static float hum(int load, int startLoad, int fullLoad, int settingPercent) {
    if (load <= startLoad || fullLoad <= startLoad) {
      return 0F;
    }
    float level = Math.min(1F, (load - startLoad) / (float) (fullLoad - startLoad));
    return level * settingPercent / PERCENT;
  }

  public static float loopPitch(int workload) {
    return LOWEST_PITCH + PITCH_RISE * PowerDials.gauge(workload);
  }

  public static float loopVolume(int workload) {
    return QUIETEST_LOOP + LOOP_RISE * PowerDials.gauge(workload);
  }
}
