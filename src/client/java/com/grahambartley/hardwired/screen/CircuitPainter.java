package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.Line;
import com.grahambartley.hardwired.circuit.Part;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Side;
import com.grahambartley.hardwired.screen.CircuitLayout.Point;
import com.grahambartley.hardwired.screen.CircuitLayout.Rect;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.client.gui.DrawContext;

public final class CircuitPainter {

  private static final int BOX_INSET = 2;
  private static final int BOX_SIZE = 10;
  private static final int GLYPH_INSET = 4;
  private static final int ARROW_HALF = 3;

  private final DrawContext context;
  private final CircuitLayout layout;

  public CircuitPainter(DrawContext context, CircuitLayout layout) {
    this.context = context;
    this.layout = layout;
  }

  public void grid() {
    for (int y = 0; y <= Design.GRID_HEIGHT; y++) {
      for (int x = 0; x <= Design.GRID_WIDTH; x++) {
        int dotX = Math.min(layout.cellX(x), layout.gridRight() - 1);
        int dotY = Math.min(layout.cellY(y), layout.gridBottom() - 1);
        context.fill(dotX, dotY, dotX + 1, dotY + 1, PhosphorScreen.GRID_DOT);
      }
    }
  }

  public void lines(Design design, IntPredicate on) {
    int lane = 0;
    for (Line line : design.lines()) {
      Point from = layout.sourcePoint(design, line.from());
      Point to = layout.sinkPoint(design, line.to(), line.pin());
      route(LineRoute.between(from, to, lane++), ink(on.test(line.from())));
    }
  }

  public void route(List<Point> points, int color) {
    for (int index = 1; index < points.size(); index++) {
      Point a = points.get(index - 1);
      Point b = points.get(index);
      context.fill(
          Math.min(a.x(), b.x()),
          Math.min(a.y(), b.y()),
          Math.max(a.x(), b.x()) + 1,
          Math.max(a.y(), b.y()) + 1,
          color);
    }
  }

  public void parts(Design design, IntPredicate on, int selectedNode) {
    for (int index = 0; index < design.parts().size(); index++) {
      int node = Design.partNode(index);
      part(design.parts().get(index), on.test(node), node == selectedNode);
    }
  }

  public void part(Part part, boolean on, boolean selected) {
    int x = layout.cellX(part.x());
    int y = layout.cellY(part.y());
    int boxX = x + BOX_INSET;
    int boxY = y + BOX_INSET;
    int outline = selected || on ? PhosphorScreen.BRIGHT : PhosphorScreen.DIM;
    context.fill(boxX, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, outline);
    context.fill(
        boxX + 1, boxY + 1, boxX + BOX_SIZE - 1, boxY + BOX_SIZE - 1, PhosphorScreen.GLASS);
    glyph(part, x + GLYPH_INSET - 1, y + GLYPH_INSET - 1, ink(on || selected));
    for (int pin = 0; pin < part.type().inputs(); pin++) {
      Point point = layout.inputPoint(part, pin);
      context.fill(
          point.x(),
          point.y(),
          point.x() + CircuitLayout.PIN_STUB,
          point.y() + 1,
          PhosphorScreen.DIM);
    }
    Point out = layout.outputPoint(part);
    context.fill(out.x() - CircuitLayout.PIN_STUB + 1, out.y(), out.x() + 1, out.y() + 1, ink(on));
  }

  private void glyph(Part part, int x, int y, int color) {
    List<String> rows = PartGlyph.rows(part.type());
    glyphRows(rows, x, y, color);
  }

  public void glyphRows(List<String> rows, int x, int y, int color) {
    for (int row = 0; row < rows.size(); row++) {
      String bits = rows.get(row);
      for (int column = 0; column < bits.length(); column++) {
        if (bits.charAt(column) == '#') {
          context.fill(x + column, y + row, x + column + 1, y + row + 1, color);
        }
      }
    }
  }

  public void pad(Side side, PortMode mode, boolean on) {
    Rect pad = layout.pad(side);
    int edge = mode == PortMode.UNUSED ? PhosphorScreen.GRID_DOT : ink(on);
    context.fill(pad.x(), pad.y(), pad.x() + pad.width(), pad.y() + pad.height(), edge);
    context.fill(
        pad.x() + 1,
        pad.y() + 1,
        pad.x() + pad.width() - 1,
        pad.y() + pad.height() - 1,
        PhosphorScreen.GLASS);
    if (mode != PortMode.UNUSED) {
      arrow(side, mode == PortMode.INPUT, pad, edge);
    }
  }

  private void arrow(Side side, boolean inward, Rect pad, int color) {
    int centreX = pad.x() + pad.width() / 2;
    int centreY = pad.y() + pad.height() / 2;
    boolean pointsDownOrRight = (side == Side.NORTH || side == Side.WEST) == inward;
    for (int step = 0; step < ARROW_HALF; step++) {
      int reach = pointsDownOrRight ? ARROW_HALF - 1 - step : step;
      int along = step - 1;
      if (side == Side.NORTH || side == Side.SOUTH) {
        context.fill(
            centreX - reach, centreY + along, centreX + reach + 1, centreY + along + 1, color);
      } else {
        context.fill(
            centreX + along, centreY - reach, centreX + along + 1, centreY + reach + 1, color);
      }
    }
  }

  private static int ink(boolean on) {
    return on ? PhosphorScreen.BRIGHT : PhosphorScreen.DIM;
  }
}
