package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.Line;
import com.grahambartley.hardwired.circuit.Part;
import com.grahambartley.hardwired.circuit.PartType;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Side;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public final class CircuitDraft {

  private final List<PortMode> sides = new ArrayList<>();
  private final List<Part> parts = new ArrayList<>();
  private final List<Line> lines = new ArrayList<>();

  public CircuitDraft(Design design) {
    load(design);
  }

  public void load(Design design) {
    sides.clear();
    sides.addAll(design.sides());
    parts.clear();
    parts.addAll(design.parts());
    lines.clear();
    lines.addAll(design.lines());
  }

  public void clear() {
    load(Design.EMPTY);
  }

  public Design design() {
    return new Design(sides, parts, lines);
  }

  public OptionalInt nodeAt(int x, int y) {
    for (int index = 0; index < parts.size(); index++) {
      if (parts.get(index).x() == x && parts.get(index).y() == y) {
        return OptionalInt.of(Design.partNode(index));
      }
    }
    return OptionalInt.empty();
  }

  public boolean place(PartType type, int x, int y, int maxParts) {
    if (parts.size() >= maxParts || !fits(x, y)) {
      return false;
    }
    parts.add(Part.of(type, x, y));
    return true;
  }

  public boolean move(int node, int x, int y) {
    if (!fits(x, y)) {
      return false;
    }
    int index = node - Side.COUNT;
    parts.set(index, parts.get(index).movedTo(x, y));
    return true;
  }

  private boolean fits(int x, int y) {
    return x >= 0
        && y >= 0
        && x < Design.GRID_WIDTH
        && y < Design.GRID_HEIGHT
        && nodeAt(x, y).isEmpty();
  }

  public void remove(int node) {
    parts.remove(node - Side.COUNT);
    lines.removeIf(line -> line.from() == node || line.to() == node);
    lines.replaceAll(
        line ->
            new Line(
                line.from() > node ? line.from() - 1 : line.from(),
                line.to() > node ? line.to() - 1 : line.to(),
                line.pin()));
  }

  public void cycleSide(Side side) {
    sides.set(side.ordinal(), sides.get(side.ordinal()).next());
    int node = Design.sideNode(side);
    lines.removeIf(line -> line.from() == node || line.to() == node);
  }

  public boolean connect(CircuitPin first, CircuitPin second) {
    if (first.output() == second.output() || first.node() == second.node()) {
      return false;
    }
    CircuitPin source = first.output() ? first : second;
    CircuitPin sink = first.output() ? second : first;
    disconnect(sink);
    lines.add(new Line(source.node(), sink.node(), sink.pin()));
    return true;
  }

  public void disconnect(CircuitPin pin) {
    if (pin.output()) {
      lines.removeIf(line -> line.from() == pin.node());
    } else {
      lines.removeIf(line -> line.to() == pin.node() && line.pin() == pin.pin());
    }
  }

  public void adjustSetting(int node, int change) {
    int index = node - Side.COUNT;
    Part part = parts.get(index);
    PartType type = part.type();
    if (type.hasSetting()) {
      parts.set(
          index,
          part.withSetting(
              Math.clamp((long) part.setting() + change, type.minSetting(), type.maxSetting())));
    }
  }
}
