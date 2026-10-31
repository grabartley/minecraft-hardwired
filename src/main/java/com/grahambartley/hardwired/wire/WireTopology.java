package com.grahambartley.hardwired.wire;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class WireTopology {

  private WireTopology() {}

  public static List<WireFace> targets(BlockPos hit, Direction side) {
    return List.of(new WireFace(hit, side), new WireFace(hit.offset(side.getOpposite()), side));
  }

  public static List<Direction> edges(Direction side) {
    List<Direction> edges = new ArrayList<>(4);
    for (Direction direction : Direction.values()) {
      if (direction.getAxis() != side.getAxis()) {
        edges.add(direction);
      }
    }
    return edges;
  }

  public static List<WireFace> acrossEdge(WireFace face, Direction edge) {
    return List.of(
        new WireFace(face.pos().offset(edge), face.side()),
        new WireFace(face.pos(), edge),
        new WireFace(face.occupant().offset(edge), edge.getOpposite()));
  }

  public static List<WireFace> neighbours(WireFace face) {
    List<WireFace> neighbours = new ArrayList<>(12);
    for (Direction edge : edges(face.side())) {
      neighbours.addAll(acrossEdge(face, edge));
    }
    return neighbours;
  }

  public static Set<Direction> arms(WireFace face, Predicate<WireFace> present) {
    Set<Direction> arms = EnumSet.noneOf(Direction.class);
    for (Direction edge : edges(face.side())) {
      if (acrossEdge(face, edge).stream().anyMatch(present)) {
        arms.add(edge);
      }
    }
    return arms;
  }

  public static List<TouchPoint> touches(WireFace face, Set<Direction> arms) {
    List<TouchPoint> touches = new ArrayList<>(6);
    touches.add(new TouchPoint(face.pos(), face.side().getOpposite()));
    touches.add(new TouchPoint(face.occupant(), face.side()));
    if (arms.size() <= 1) {
      for (Direction edge : edges(face.side())) {
        if (!arms.contains(edge)) {
          touches.add(new TouchPoint(face.occupant().offset(edge), edge));
        }
      }
    }
    return touches;
  }
}
