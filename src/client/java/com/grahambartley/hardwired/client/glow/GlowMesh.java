package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.light.Glow;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public final class GlowMesh {

  public static final GlowMesh EMPTY = new GlowMesh(BlockPos.ORIGIN, new FloatArrayList());
  public static final int FLOATS_PER_VERTEX = 7;
  public static final int STRENGTH_OFFSET = 6;
  private static final int VERTICES_PER_FACE = 4;
  private static final float SURFACE_OFFSET = 0.002F;
  private static final float SAMPLE_REACH = 0.5F;
  private static final float INVISIBLE = 0.002F;
  private static final Box FULL_BLOCK = new Box(0, 0, 0, 1, 1, 1);
  private static final int[][] FACE_CORNERS = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};

  private final BlockPos anchor;
  private final FloatArrayList vertices;

  private GlowMesh(BlockPos anchor, FloatArrayList vertices) {
    this.anchor = anchor;
    this.vertices = vertices;
  }

  public static GlowMesh build(List<Glow> glows, GlowSurfaces surfaces) {
    if (glows.isEmpty()) {
      return EMPTY;
    }
    GlowField field = new GlowField(surfaces::isSolid);
    glows.forEach(field::add);
    GlowMesh mesh = new GlowMesh(glows.get(0).pos(), new FloatArrayList());
    for (long key : field.cells()) {
      mesh.addCell(field, surfaces, BlockPos.fromLong(key));
    }
    return mesh;
  }

  public BlockPos anchor() {
    return anchor;
  }

  public FloatArrayList vertices() {
    return vertices;
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
        addFace(field, box.offset(cell), side, daylight);
      }
    }
  }

  private void addFace(GlowField field, Box box, Direction side, float daylight) {
    float[] face = new float[FLOATS_PER_VERTEX * VERTICES_PER_FACE];
    boolean visible = false;
    for (int vertex = 0; vertex < VERTICES_PER_FACE; vertex++) {
      int start = vertex * FLOATS_PER_VERTEX;
      corner(field, box, side, FACE_CORNERS[vertex], daylight, face, start);
      visible |= face[start + STRENGTH_OFFSET] > INVISIBLE;
    }
    if (visible) {
      vertices.addElements(vertices.size(), face);
    }
  }

  private void corner(
      GlowField field,
      Box box,
      Direction side,
      int[] corner,
      float daylight,
      float[] out,
      int start) {
    Direction.Axis first = side.getAxis() == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
    Direction.Axis second =
        side.getAxis() == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z;
    double[] point = new double[3];
    for (Direction.Axis axis : Direction.Axis.values()) {
      boolean high =
          axis == side.getAxis()
              ? side.getDirection() == Direction.AxisDirection.POSITIVE
              : corner[axis == first ? 0 : 1] == 1;
      point[axis.ordinal()] = high ? box.getMax(axis) : box.getMin(axis);
    }
    float[] sample = sample(field, side, point);
    for (int axis = 0; axis < point.length; axis++) {
      double push = axis == side.getAxis().ordinal() ? side.getDirection().offset() : 0;
      out[start + axis] = (float) (point[axis] + push * SURFACE_OFFSET - anchorAxis(axis));
    }
    float total = Math.max(sample[GlowField.WEIGHT], Float.MIN_NORMAL);
    out[start + 3] = sample[GlowField.RED] / total;
    out[start + 4] = sample[GlowField.GREEN] / total;
    out[start + 5] = sample[GlowField.BLUE] / total;
    out[start + STRENGTH_OFFSET] = GlowColor.strength(sample[GlowField.WEIGHT], daylight);
  }

  private static float[] sample(GlowField field, Direction side, double[] point) {
    double x = point[0] + side.getOffsetX() * SAMPLE_REACH - SAMPLE_REACH;
    double y = point[1] + side.getOffsetY() * SAMPLE_REACH - SAMPLE_REACH;
    double z = point[2] + side.getOffsetZ() * SAMPLE_REACH - SAMPLE_REACH;
    int baseX = MathHelper.floor(x);
    int baseY = MathHelper.floor(y);
    int baseZ = MathHelper.floor(z);
    float[] sum = new float[4];
    float open = 0F;
    BlockPos.Mutable pos = new BlockPos.Mutable();
    for (int dx = 0; dx <= 1; dx++) {
      for (int dy = 0; dy <= 1; dy++) {
        for (int dz = 0; dz <= 1; dz++) {
          float share = share(x - baseX, dx) * share(y - baseY, dy) * share(z - baseZ, dz);
          pos.set(baseX + dx, baseY + dy, baseZ + dz);
          if (share == 0F || field.blocksLight(pos)) {
            continue;
          }
          open += share;
          float[] cell = field.at(pos.asLong());
          if (cell != null) {
            for (int channel = 0; channel < sum.length; channel++) {
              sum[channel] += cell[channel] * share;
            }
          }
        }
      }
    }
    if (open > 0F) {
      for (int channel = 0; channel < sum.length; channel++) {
        sum[channel] /= open;
      }
    }
    return sum;
  }

  private static float share(double fraction, int step) {
    return (float) (step == 1 ? fraction : 1D - fraction);
  }

  private int anchorAxis(int axis) {
    return axis == 0 ? anchor.getX() : axis == 1 ? anchor.getY() : anchor.getZ();
  }
}
