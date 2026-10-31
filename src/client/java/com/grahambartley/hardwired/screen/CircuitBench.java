package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.Evaluator;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Side;

public final class CircuitBench {

  private final Design design;
  private final Evaluator evaluator;
  private int inputs;
  private int ticks;

  public CircuitBench(Design design) {
    this.design = design;
    this.evaluator = new Evaluator(design);
    this.evaluator.step(inputs);
  }

  public void tick() {
    ticks++;
    if (ticks % Evaluator.TICKS_PER_STEP == 0) {
      evaluator.step(inputs);
    }
  }

  public void toggle(Side side) {
    if (design.mode(side) == PortMode.INPUT) {
      inputs ^= side.bit();
    }
  }

  public boolean isOn(int node) {
    return evaluator.isOn(node);
  }

  public boolean isSideOn(Side side) {
    return design.mode(side) == PortMode.INPUT
        ? (inputs & side.bit()) != 0
        : (evaluator.outputs() & side.bit()) != 0;
  }
}
