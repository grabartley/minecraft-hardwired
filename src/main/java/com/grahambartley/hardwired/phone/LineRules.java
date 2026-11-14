package com.grahambartley.hardwired.phone;

import java.util.Optional;

public final class LineRules {

  public enum PickUp {
    ANSWER,
    BUSY,
    RESUME,
    JOIN,
    LIFT
  }

  public record Dial(
      boolean holding,
      boolean rateAllows,
      boolean numberExists,
      boolean ownNumber,
      boolean targetBusy,
      boolean targetAnswerable,
      boolean exchangesLive,
      boolean linesFree) {}

  private LineRules() {}

  public static PickUp pickUp(
      boolean ringingHere, Optional<Boolean> heldByMe, boolean liveCallOnNumber) {
    if (ringingHere) {
      return PickUp.ANSWER;
    }
    if (heldByMe.isPresent()) {
      return heldByMe.get() ? PickUp.RESUME : PickUp.BUSY;
    }
    return liveCallOnNumber ? PickUp.JOIN : PickUp.LIFT;
  }

  public static LineStatus dial(Dial facts) {
    if (!facts.holding()) {
      return LineStatus.LINE_DEAD;
    }
    if (!facts.rateAllows()) {
      return LineStatus.TOO_FAST;
    }
    if (!facts.numberExists()) {
      return LineStatus.NO_SUCH_NUMBER;
    }
    if (facts.ownNumber() || facts.targetBusy()) {
      return LineStatus.BUSY;
    }
    if (!facts.targetAnswerable() || !facts.exchangesLive()) {
      return LineStatus.LINE_DEAD;
    }
    return facts.linesFree() ? LineStatus.RINGING_OUT : LineStatus.BUSY;
  }
}
