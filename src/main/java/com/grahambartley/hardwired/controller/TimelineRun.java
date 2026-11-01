package com.grahambartley.hardwired.controller;

public final class TimelineRun {

  private int position;
  private TimelineOrder travel;
  private int ticksLeft;
  private TimelineOrder pending;
  private int reverseLeft;

  public TimelineRun() {}

  private TimelineRun(
      int position, TimelineOrder travel, int ticksLeft, TimelineOrder pending, int reverseLeft) {
    this.position = position;
    this.travel = travel;
    this.ticksLeft = ticksLeft;
    this.pending = pending;
    this.reverseLeft = reverseLeft;
  }

  public static TimelineRun restored(
      Timeline timeline,
      int position,
      TimelineOrder travel,
      int ticksLeft,
      TimelineOrder pending,
      int reverseLeft) {
    boolean travelling = travel != null && travel != TimelineOrder.STOP && ticksLeft > 0;
    TimelineRun run =
        new TimelineRun(
            Math.clamp(position, 0, timeline.size()),
            travelling ? travel : null,
            travelling ? Math.min(ticksLeft, TimelineStep.MAX_WAIT_TICKS) : 0,
            pending,
            travelling ? 0 : Math.clamp(reverseLeft, 0, timeline.autoReverseTicks()));
    run.dropReverseUnlessAtEnd(timeline);
    return run;
  }

  public int position() {
    return position;
  }

  public TimelineOrder travel() {
    return travel;
  }

  public int ticksLeft() {
    return ticksLeft;
  }

  public TimelineOrder pending() {
    return pending;
  }

  public int reverseLeft() {
    return reverseLeft;
  }

  public boolean isRunning() {
    return travel != null || reverseLeft > 0 || pending != null;
  }

  public int outputs(Timeline timeline) {
    return timeline.outputsAt(position);
  }

  public void order(Timeline timeline, TimelineOrder order) {
    if (travel != null) {
      pending = order == travel ? null : order;
    } else if (reverseLeft > 0) {
      orderWhileHolding(timeline, order);
    } else {
      begin(timeline, order);
    }
  }

  public void orderDark(Timeline timeline, TimelineOrder order) {
    if (travel != null) {
      order(timeline, order);
    } else {
      pending = order;
    }
  }

  private void orderWhileHolding(Timeline timeline, TimelineOrder order) {
    if (order == TimelineOrder.FORWARD) {
      reverseLeft = timeline.autoReverseTicks();
    } else {
      reverseLeft = 0;
      begin(timeline, order);
    }
  }

  public TimelineOrder toggleOrder(Timeline timeline) {
    if (travel != null && pending == TimelineOrder.STOP) {
      return travel;
    }
    if (isRunning()) {
      return TimelineOrder.STOP;
    }
    return position < timeline.size() ? TimelineOrder.FORWARD : TimelineOrder.BACKWARD;
  }

  public void tick(Timeline timeline) {
    if (travel == null && pending != null) {
      TimelineOrder waiting = pending;
      pending = null;
      order(timeline, waiting);
      return;
    }
    if (reverseLeft > 0) {
      reverseLeft--;
      if (reverseLeft == 0) {
        begin(timeline, TimelineOrder.BACKWARD);
      }
      return;
    }
    if (travel == null) {
      return;
    }
    ticksLeft--;
    if (ticksLeft <= 0) {
      TimelineOrder next = pending != null ? pending : travel;
      travel = null;
      pending = null;
      begin(timeline, next);
    }
  }

  public void retarget(Timeline next) {
    position = Math.min(position, next.size());
    if (next.size() == 0) {
      travel = null;
      ticksLeft = 0;
      pending = null;
    }
    reverseLeft = Math.min(reverseLeft, next.autoReverseTicks());
    dropReverseUnlessAtEnd(next);
  }

  private void dropReverseUnlessAtEnd(Timeline timeline) {
    if (position != timeline.size()) {
      reverseLeft = 0;
    }
  }

  private void begin(Timeline timeline, TimelineOrder order) {
    if (order == TimelineOrder.FORWARD) {
      beginForward(timeline);
    } else if (order == TimelineOrder.BACKWARD && position > 0) {
      ticksLeft = timeline.waitBefore(position);
      position--;
      travel = TimelineOrder.BACKWARD;
    }
  }

  private void beginForward(Timeline timeline) {
    if (position < timeline.size()) {
      position++;
      ticksLeft = timeline.waitBefore(position);
      travel = TimelineOrder.FORWARD;
    } else if (timeline.size() > 0) {
      reverseLeft = timeline.autoReverseTicks();
    }
  }
}
