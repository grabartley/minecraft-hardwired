package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.screen.CircuitLayout.Point;
import java.util.List;

public final class LineRoute {

  private static final int LANES = 5;
  private static final int LANE_GAP = 2;

  private LineRoute() {}

  public static List<Point> between(Point from, Point to, int lane) {
    int offset = (Math.floorMod(lane, LANES) - LANES / 2) * LANE_GAP;
    int bendX = (from.x() + to.x()) / 2 + offset;
    return List.of(from, new Point(bendX, from.y()), new Point(bendX, to.y()), to);
  }
}
