package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.light.Glow;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

final class GlowMesh {

  static final GlowMesh EMPTY = new GlowMesh(BlockPos.ORIGIN);
  private static final int POSITION = 0;
  private static final int HUE = 3;
  private static final int STRENGTH = 6;
  private static final int FLOATS_PER_VERTEX = 7;
  private static final int VERTICES_PER_FACE = 4;
  private static final float SURFACE_OFFSET = 0.01F;
  private static final float SAMPLE_REACH = 0.5F;
  private static final float INVISIBLE = 0.002F;
  private static final Box FULL_BLOCK = new Box(0, 0, 0, 1, 1, 1);
  private static final boolean[][] FACE_CORNERS = {
    {false, false}, {false, true}, {true, true}, {true, false}
  };

  private final BlockPos anchor;
  private final FloatArrayList vertices = new FloatArrayList();

  private GlowMesh(BlockPos anchor) {
    this.anchor = anchor;
  }

  static GlowMesh build(List<Glow> glows, GlowSurfaces surfaces) {
    if (glows.isEmpty()) {
      return EMPTY;
    }
    GlowField field = new GlowField(surfaces::isSolid, surfaces);
    glows.forEach(field::add);
    GlowMesh mesh = new GlowMesh(glows.get(0).pos());
    for (long key : field.cells()) {
      mesh.addCell(field, surfaces, BlockPos.fromLong(key));
    }
    return mesh;
  }

  boolean isEmpty() {
    return vertices.isEmpty();
  }

  BlockPos anchor() {
    return anchor;
  }

  void forEachVertex(GlowVertexSink sink) {
    for (int start = 0; start < vertices.size(); start += FLOATS_PER_VERTEX) {
      sink.vertex(
          vertices.getFloat(start + POSITION),
          vertices.getFloat(start + POSITION + 1),
          vertices.getFloat(start + POSITION + 2),
          vertices.getFloat(start + HUE),
          vertices.getFloat(start + HUE + 1),
          vertices.getFloat(start + HUE + 2),
          vertices.getFloat(start + STRENGTH));
    }
  }

  private void addCell(GlowField field, GlowSurfaces surfaces, BlockPos cell) {
    float daylight = surfaces.daylight(cell);
    for (Direction side : Direction.values()) {
      BlockPos neighbour = cell.offset(side);
      if (field.blocksLight(neighbour)) {
        addFace(field, FULL_BLOCK.offset(neighbour), side.getOpposite(), daylight);
      }
    }
    for (Box box : surfaces.partBoxes(cell)) {
      for (Direction side : Direction.values()) {
        if (surfaces.showsSide(cell, side)) {
          addFace(field, box.offset(cell), side, daylight);
        }
      }
    }
  }

  private void addFace(GlowField field, Box box, Direction side, float daylight) {
    float[] face = new float[FLOATS_PER_VERTEX * VERTICES_PER_FACE];
    boolean visible = false;
    for (int vertex = 0; vertex < VERTICES_PER_FACE; vertex++) {
      int start = vertex * FLOATS_PER_VERTEX;
      double[] corner = corner(box, side, FACE_CORNERS[vertex]);
      writeVertex(field, side, corner, daylight, face, start);
      visible |= face[start + STRENGTH] > INVISIBLE;
    }
    if (visible) {
      vertices.addElements(vertices.size(), face);
    }
  }

  private static double[] corner(Box box, Direction side, boolean[] highs) {
    Direction.Axis first = side.getAxis() == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
    double[] point = new double[Direction.Axis.values().length];
    for (Direction.Axis axis : Direction.Axis.values()) {
      boolean high =
          axis == side.getAxis()
              ? side.getDirection() == Direction.AxisDirection.POSITIVE
              : highs[axis == first ? 0 : 1];
      point[axis.ordinal()] = high ? box.getMax(axis) : box.getMin(axis);
    }
    return point;
  }

  private void writeVertex(
      GlowField field, Direction side, double[] point, float daylight, float[] out, int start) {
    float[] light =
        field.sample(
            point[0] + side.getOffsetX() * SAMPLE_REACH,
            point[1] + side.getOffsetY() * SAMPLE_REACH,
            point[2] + side.getOffsetZ() * SAMPLE_REACH);
    for (Direction.Axis axis : Direction.Axis.values()) {
      double push = side.getAxis() == axis ? side.getDirection().offset() * SURFACE_OFFSET : 0D;
      out[start + POSITION + axis.ordinal()] =
          (float) (point[axis.ordinal()] + push - anchor.getComponentAlongAxis(axis));
    }
    float weight = light[GlowField.WEIGHT];
    float total = Math.max(weight, Float.MIN_NORMAL);
    out[start + HUE] = light[GlowField.RED] / total;
    out[start + HUE + 1] = light[GlowField.GREEN] / total;
    out[start + HUE + 2] = light[GlowField.BLUE] / total;
    out[start + STRENGTH] = GlowColor.strength(weight, daylight);
  }
}
