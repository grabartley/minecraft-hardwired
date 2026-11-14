package com.grahambartley.hardwired.screen;

import java.util.OptionalInt;

public final class DialState {

  public static final int DIGITS = 3;
  private static final double HALF_STEP = RotaryWheel.STEP / 2;

  private String dialed = "";
  private int held;
  private double grabAngle;
  private double turned;
  private int returnSteps;
  private boolean clicking;

  public String dialed() {
    return dialed;
  }

  public double turned() {
    return returnSteps > 0 ? returnSteps * HALF_STEP : turned;
  }

  public boolean canGrab(boolean onCall) {
    return !onCall && held == 0 && returnSteps == 0 && dialed.length() < DIGITS;
  }

  public boolean grab(int pulses, double angle, boolean onCall) {
    if (pulses <= 0 || !canGrab(onCall)) {
      return false;
    }
    held = pulses;
    grabAngle = angle;
    turned = 0;
    return true;
  }

  public boolean holding() {
    return held > 0;
  }

  public void drag(double angle) {
    if (held > 0) {
      turned = RotaryWheel.turn(grabAngle, angle, held);
    }
  }

  public OptionalInt release() {
    if (held == 0) {
      return OptionalInt.empty();
    }
    boolean dialledIt = RotaryWheel.atStop(turned, held);
    clicking = dialledIt;
    returnSteps = dialledIt ? 2 * (held + 1) : (int) Math.ceil(turned / HALF_STEP);
    int pulses = held;
    held = 0;
    turned = 0;
    if (!dialledIt) {
      return OptionalInt.empty();
    }
    dialed += RotaryWheel.digitFor(pulses);
    return OptionalInt.of(RotaryWheel.digitFor(pulses));
  }

  public boolean tick() {
    if (returnSteps == 0) {
      return false;
    }
    returnSteps--;
    return clicking && returnSteps >= 2 && returnSteps % 2 == 0;
  }

  public OptionalInt number() {
    return dialed.length() == DIGITS
        ? OptionalInt.of(Integer.parseInt(dialed))
        : OptionalInt.empty();
  }

  public void clearDigits() {
    dialed = "";
  }
}
