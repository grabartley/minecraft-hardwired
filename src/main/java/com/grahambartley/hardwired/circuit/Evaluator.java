package com.grahambartley.hardwired.circuit;

import java.util.Arrays;

public final class Evaluator {

  public static final int TICKS_PER_STEP = 2;

  private static final int UNCONNECTED = -1;
  private static final int MAX_PINS = 2;

  private final Design design;
  private final int[] gateOrder;
  private final int[][] sources;
  private final boolean[] levels;
  private final int[] memory;
  private int outputs;
  private boolean settled = true;

  public Evaluator(Design design) {
    this.design = design;
    this.gateOrder =
        LoopCheck.gateOrder(design)
            .orElseThrow(() -> new IllegalArgumentException("design has a loop without delay"));
    int nodes = design.nodes();
    this.sources = new int[nodes][MAX_PINS];
    for (int[] pins : sources) {
      Arrays.fill(pins, UNCONNECTED);
    }
    for (Line line : design.lines()) {
      sources[line.to()][line.pin()] = line.from();
    }
    this.levels = new boolean[nodes];
    this.memory = new int[design.parts().size()];
  }

  public static int delayToNextStep(long worldTime) {
    return TICKS_PER_STEP - (int) Math.floorMod(worldTime, (long) TICKS_PER_STEP);
  }

  public void step(int inputs) {
    readSources(inputs);
    for (int node : gateOrder) {
      Part part = design.partAt(node);
      levels[node] = part.type().gate(input(node, 0), input(node, 1));
    }
    settled = !advanceMemory();
    outputs = readOutputs();
  }

  private void readSources(int inputs) {
    for (Side side : Side.values()) {
      levels[Design.sideNode(side)] =
          design.mode(side) == PortMode.INPUT && (inputs & side.bit()) != 0;
    }
    for (int index = 0; index < memory.length; index++) {
      int node = Design.partNode(index);
      Part part = design.parts().get(index);
      if (part.type().isDelay()) {
        levels[node] = delayOutput(part, memory[index]);
      }
    }
  }

  private boolean input(int node, int pin) {
    int source = sources[node][pin];
    return source != UNCONNECTED && levels[source];
  }

  private boolean advanceMemory() {
    boolean changed = false;
    for (int index = 0; index < memory.length; index++) {
      Part part = design.parts().get(index);
      if (part.type().isDelay()) {
        int node = Design.partNode(index);
        int next = nextMemory(part, memory[index], input(node, 0), input(node, 1));
        changed |= next != memory[index];
        memory[index] = next;
      }
    }
    return changed;
  }

  private int readOutputs() {
    int mask = 0;
    for (Side side : Side.values()) {
      if (design.mode(side) == PortMode.OUTPUT && input(Design.sideNode(side), 0)) {
        mask |= side.bit();
      }
    }
    return mask;
  }

  private static boolean delayOutput(Part part, int stored) {
    return switch (part.type()) {
      case SR_LATCH, D_LATCH -> stored != 0;
      case TIMER -> stored >= part.setting();
      case COUNTER -> count(stored) == part.setting() - 1;
      default -> throw new IllegalStateException(part.type() + " holds no memory");
    };
  }

  private static int nextMemory(Part part, int stored, boolean first, boolean second) {
    return switch (part.type()) {
      case SR_LATCH -> nextSrLatch(stored, first, second);
      case D_LATCH -> second ? (first ? 1 : 0) : stored;
      case TIMER -> first ? Math.min(stored + 1, part.setting()) : 0;
      case COUNTER -> nextCounter(part, stored, first, second);
      default -> throw new IllegalStateException(part.type() + " holds no memory");
    };
  }

  private static int nextSrLatch(int stored, boolean set, boolean reset) {
    if (reset) {
      return 0;
    }
    return set ? 1 : stored;
  }

  private static int nextCounter(Part part, int stored, boolean clock, boolean reset) {
    int count = count(stored);
    boolean rose = clock && !clockWasOn(stored);
    if (reset) {
      count = 0;
    } else if (rose) {
      count = (count + 1) % part.setting();
    }
    return count << 1 | (clock ? 1 : 0);
  }

  private static int count(int stored) {
    return stored >>> 1;
  }

  private static boolean clockWasOn(int stored) {
    return (stored & 1) != 0;
  }

  public int outputs() {
    return outputs;
  }

  public boolean isSettled() {
    return settled;
  }

  public boolean isOn(int node) {
    return levels[node];
  }

  public int[] memory() {
    return memory.clone();
  }

  public void restore(int[] saved, int savedOutputs) {
    if (saved.length != memory.length) {
      return;
    }
    for (int index = 0; index < memory.length; index++) {
      memory[index] = clamped(design.parts().get(index), saved[index]);
    }
    outputs = savedOutputs & allowedOutputs();
  }

  private int allowedOutputs() {
    int mask = 0;
    for (Side side : Side.values()) {
      if (design.mode(side) == PortMode.OUTPUT) {
        mask |= side.bit();
      }
    }
    return mask;
  }

  private static int clamped(Part part, int stored) {
    return switch (part.type()) {
      case SR_LATCH, D_LATCH -> stored == 0 ? 0 : 1;
      case TIMER -> Math.clamp(stored, 0, part.setting());
      case COUNTER ->
          Math.clamp(count(Math.max(stored, 0)), 0, part.setting() - 1) << 1 | (stored & 1);
      default -> 0;
    };
  }
}
