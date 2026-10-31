package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.circuit.PortMode;
import java.util.Locale;
import net.minecraft.util.StringIdentifiable;

public enum CircuitPort implements StringIdentifiable {
  UNUSED,
  INPUT,
  OUTPUT;

  public static CircuitPort of(PortMode mode) {
    return values()[mode.ordinal()];
  }

  @Override
  public String asString() {
    return name().toLowerCase(Locale.ROOT);
  }
}
