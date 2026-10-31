package com.grahambartley.hardwired.circuit;

import java.util.ArrayList;
import java.util.List;

public record Design(List<PortMode> sides, List<Part> parts, List<Line> lines) {

  public static final int GRID_WIDTH = 20;
  public static final int GRID_HEIGHT = 13;

  public static final Design EMPTY =
      new Design(List.of(PortMode.UNUSED, PortMode.UNUSED, PortMode.UNUSED, PortMode.UNUSED));

  public Design(List<PortMode> sides) {
    this(sides, List.of(), List.of());
  }

  public Design {
    sides = List.copyOf(sides);
    parts = List.copyOf(parts);
    lines = List.copyOf(lines);
  }

  public static int sideNode(Side side) {
    return side.ordinal();
  }

  public static int partNode(int part) {
    return Side.COUNT + part;
  }

  public int nodes() {
    return Side.COUNT + parts.size();
  }

  public boolean isSide(int node) {
    return node >= 0 && node < Side.COUNT;
  }

  public PortMode mode(Side side) {
    return sides.get(side.ordinal());
  }

  public Part partAt(int node) {
    return parts.get(node - Side.COUNT);
  }

  public List<PortMode> sidesTurned(int quarterTurns) {
    List<PortMode> turned = new ArrayList<>(sides);
    for (Side side : Side.values()) {
      turned.set(side.turned(quarterTurns).ordinal(), mode(side));
    }
    return turned;
  }

  public boolean isEmpty() {
    return parts.isEmpty();
  }
}
