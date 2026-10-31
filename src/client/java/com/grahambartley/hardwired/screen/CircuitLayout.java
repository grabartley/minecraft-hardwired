package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.Part;
import com.grahambartley.hardwired.circuit.PartType;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Side;
import java.util.Optional;

public record CircuitLayout(int left, int top) {

  public static final int WIDTH = 404;
  public static final int HEIGHT = 232;
  public static final int CELL = 14;
  public static final int PAD_LENGTH = 26;
  public static final int PAD_DEPTH = 7;
  public static final int PIN_STUB = 2;
  public static final int PALETTE_ROW = 11;

  private static final int GRID_INSET_X = 16;
  private static final int GRID_INSET_Y = 26;
  private static final int COLUMN_GAP = 14;
  private static final int COLUMN_WIDTH = 86;
  private static final int UPPER_PIN = 4;
  private static final int LOWER_PIN = 10;
  private static final int MIDDLE_PIN = 7;
  private static final int PALETTE_TOP = 18;
  private static final int SETTING_TOP = 130;
  private static final int BUTTONS_TOP = 148;
  private static final int STATUS_TOP = 219;

  public record Rect(int x, int y, int width, int height) {
    public boolean contains(double px, double py) {
      return px >= x && py >= y && px < x + width && py < y + height;
    }
  }

  public record Point(int x, int y) {}

  public record Cell(int x, int y) {}

  public static CircuitLayout centredIn(int screenWidth, int screenHeight) {
    return new CircuitLayout((screenWidth - WIDTH) / 2, (screenHeight - HEIGHT) / 2);
  }

  public int gridX() {
    return left + GRID_INSET_X;
  }

  public int gridY() {
    return top + GRID_INSET_Y;
  }

  public int gridRight() {
    return gridX() + Design.GRID_WIDTH * CELL;
  }

  public int gridBottom() {
    return gridY() + Design.GRID_HEIGHT * CELL;
  }

  public int columnX() {
    return gridRight() + COLUMN_GAP;
  }

  public int columnWidth() {
    return COLUMN_WIDTH;
  }

  public int settingY() {
    return top + SETTING_TOP;
  }

  public int buttonsY() {
    return top + BUTTONS_TOP;
  }

  public int statusY() {
    return top + STATUS_TOP;
  }

  public Rect paletteRow(int row) {
    return new Rect(columnX(), top + PALETTE_TOP + row * PALETTE_ROW, COLUMN_WIDTH, PALETTE_ROW);
  }

  public Optional<PartType> paletteAt(double x, double y) {
    PartType[] types = PartType.values();
    for (int row = 0; row < types.length; row++) {
      if (paletteRow(row).contains(x, y)) {
        return Optional.of(types[row]);
      }
    }
    return Optional.empty();
  }

  public Optional<Cell> cellAt(double x, double y) {
    if (x < gridX() || y < gridY() || x >= gridRight() || y >= gridBottom()) {
      return Optional.empty();
    }
    return Optional.of(
        new Cell((int) Math.floor((x - gridX()) / CELL), (int) Math.floor((y - gridY()) / CELL)));
  }

  public int cellX(int x) {
    return gridX() + x * CELL;
  }

  public int cellY(int y) {
    return gridY() + y * CELL;
  }

  public Rect pad(Side side) {
    int middleX = (gridX() + gridRight() - PAD_LENGTH) / 2;
    int middleY = (gridY() + gridBottom() - PAD_LENGTH) / 2;
    return switch (side) {
      case NORTH -> new Rect(middleX, gridY() - PAD_DEPTH - 1, PAD_LENGTH, PAD_DEPTH);
      case SOUTH -> new Rect(middleX, gridBottom() + 1, PAD_LENGTH, PAD_DEPTH);
      case WEST -> new Rect(gridX() - PAD_DEPTH - 1, middleY, PAD_DEPTH, PAD_LENGTH);
      case EAST -> new Rect(gridRight() + 1, middleY, PAD_DEPTH, PAD_LENGTH);
    };
  }

  public Optional<Side> padAt(double x, double y) {
    for (Side side : Side.values()) {
      if (pad(side).contains(x, y)) {
        return Optional.of(side);
      }
    }
    return Optional.empty();
  }

  public Point sidePoint(Side side) {
    Rect pad = pad(side);
    return switch (side) {
      case NORTH -> new Point(pad.x() + PAD_LENGTH / 2, gridY());
      case SOUTH -> new Point(pad.x() + PAD_LENGTH / 2, gridBottom() - 1);
      case WEST -> new Point(gridX(), pad.y() + PAD_LENGTH / 2);
      case EAST -> new Point(gridRight() - 1, pad.y() + PAD_LENGTH / 2);
    };
  }

  public Point inputPoint(Part part, int pin) {
    int y = part.type().inputs() == 1 ? MIDDLE_PIN : pin == 0 ? UPPER_PIN : LOWER_PIN;
    return new Point(cellX(part.x()), cellY(part.y()) + y);
  }

  public Point outputPoint(Part part) {
    return new Point(cellX(part.x()) + CELL - 1, cellY(part.y()) + MIDDLE_PIN);
  }

  public Point sourcePoint(Design design, int node) {
    return design.isSide(node) ? sidePoint(Side.byIndex(node)) : outputPoint(design.partAt(node));
  }

  public Point sinkPoint(Design design, int node, int pin) {
    return design.isSide(node)
        ? sidePoint(Side.byIndex(node))
        : inputPoint(design.partAt(node), pin);
  }

  public Optional<CircuitPin> pinAt(Design design, double x, double y) {
    Optional<Side> side = padAt(x, y);
    if (side.isPresent()) {
      return sidePin(design, side.get());
    }
    Optional<Cell> cell = cellAt(x, y);
    if (cell.isEmpty()) {
      return Optional.empty();
    }
    for (int index = 0; index < design.parts().size(); index++) {
      Part part = design.parts().get(index);
      if (part.x() == cell.get().x() && part.y() == cell.get().y()) {
        return partPin(part, Design.partNode(index), x - cellX(part.x()), y - cellY(part.y()));
      }
    }
    return Optional.empty();
  }

  private static Optional<CircuitPin> sidePin(Design design, Side side) {
    PortMode mode = design.mode(side);
    return mode == PortMode.UNUSED
        ? Optional.empty()
        : Optional.of(new CircuitPin(Design.sideNode(side), 0, mode == PortMode.INPUT));
  }

  private static Optional<CircuitPin> partPin(Part part, int node, double insideX, double insideY) {
    if (insideX >= CELL - PIN_STUB - 2) {
      return Optional.of(new CircuitPin(node, 0, true));
    }
    if (insideX < PIN_STUB + 2) {
      int pin = part.type().inputs() == 2 && insideY >= (double) CELL / 2 ? 1 : 0;
      return Optional.of(new CircuitPin(node, pin, false));
    }
    return Optional.empty();
  }
}
