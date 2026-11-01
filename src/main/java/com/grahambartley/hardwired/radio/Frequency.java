package com.grahambartley.hardwired.radio;

public final class Frequency {

  public static final int MIN = 530;
  public static final int MAX = 1600;
  public static final int STEP = 10;
  public static final int DEFAULT = 1000;

  private Frequency() {}

  public static int snap(int kilohertz) {
    int clamped = Math.clamp(kilohertz, MIN, MAX);
    return MIN + Math.round((clamped - MIN) / (float) STEP) * STEP;
  }

  public static int stepsApart(int first, int second) {
    return Math.abs(first - second) / STEP;
  }

  public static int stepsAcross() {
    return (MAX - MIN) / STEP;
  }
}
