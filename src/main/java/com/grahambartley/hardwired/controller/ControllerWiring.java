package com.grahambartley.hardwired.controller;

import com.grahambartley.hardwired.wire.Hue;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record ControllerWiring(Hue open, Hue close, List<Optional<Hue>> outputs) {

  public static final ControllerWiring DEFAULT =
      new ControllerWiring(
          Hue.PLAIN,
          Hue.PLAIN,
          List.of(
              Optional.of(Hue.RED),
              Optional.of(Hue.BLUE),
              Optional.of(Hue.LIME),
              Optional.of(Hue.YELLOW)));

  public ControllerWiring {
    Set<Hue> taken = EnumSet.of(open, close);
    List<Optional<Hue>> slots = new ArrayList<>(TimelineStep.OUTPUT_SLOTS);
    for (int slot = 0; slot < TimelineStep.OUTPUT_SLOTS; slot++) {
      Optional<Hue> hue = slot < outputs.size() ? outputs.get(slot) : Optional.empty();
      slots.add(hue.filter(taken::add));
    }
    outputs = List.copyOf(slots);
  }

  public boolean readsOneColour() {
    return open == close;
  }

  public boolean drives(int outputMask, Hue color) {
    int slot = outputs.indexOf(Optional.of(color));
    return slot >= 0 && (outputMask & (1 << slot)) != 0;
  }
}
