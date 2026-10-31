package com.grahambartley.hardwired.block;

import net.minecraft.util.StringIdentifiable;

public enum JunctionRule implements StringIdentifiable {
  AND("and"),
  OR("or"),
  NOT("not");

  private final String id;

  JunctionRule(String id) {
    this.id = id;
  }

  @Override
  public String asString() {
    return id;
  }

  public String translationKey() {
    return "junction_rule.hardwired." + id;
  }

  public boolean apply(int inputsOn, int inputs) {
    if (inputs == 0) {
      return false;
    }
    return switch (this) {
      case AND -> inputsOn == inputs;
      case OR -> inputsOn > 0;
      case NOT -> inputsOn == 0;
    };
  }
}
