package com.grahambartley.hardwired.circuit;

public enum PartType {
  AND("and", 2, false, 0, 0, 0),
  OR("or", 2, false, 0, 0, 0),
  NOT("not", 1, false, 0, 0, 0),
  XOR("xor", 2, false, 0, 0, 0),
  NAND("nand", 2, false, 0, 0, 0),
  NOR("nor", 2, false, 0, 0, 0),
  SR_LATCH("sr", 2, true, 0, 0, 0),
  D_LATCH("d", 2, true, 0, 0, 0),
  TIMER("timer", 1, true, 1, 1200, 10),
  COUNTER("counter", 2, true, 2, 256, 2);

  private final String id;
  private final int inputs;
  private final boolean delay;
  private final int minSetting;
  private final int maxSetting;
  private final int defaultSetting;

  PartType(
      String id, int inputs, boolean delay, int minSetting, int maxSetting, int defaultSetting) {
    this.id = id;
    this.inputs = inputs;
    this.delay = delay;
    this.minSetting = minSetting;
    this.maxSetting = maxSetting;
    this.defaultSetting = defaultSetting;
  }

  public String id() {
    return id;
  }

  public int inputs() {
    return inputs;
  }

  public boolean isDelay() {
    return delay;
  }

  public boolean hasSetting() {
    return maxSetting > 0;
  }

  public int minSetting() {
    return minSetting;
  }

  public int maxSetting() {
    return maxSetting;
  }

  public int defaultSetting() {
    return defaultSetting;
  }

  public static PartType byId(String id) {
    for (PartType type : values()) {
      if (type.id.equals(id)) {
        return type;
      }
    }
    return null;
  }

  public boolean gate(boolean a, boolean b) {
    return switch (this) {
      case AND -> a && b;
      case OR -> a || b;
      case NOT -> !a;
      case XOR -> a != b;
      case NAND -> !(a && b);
      case NOR -> !(a || b);
      default -> throw new IllegalStateException(id + " is not a gate");
    };
  }
}
