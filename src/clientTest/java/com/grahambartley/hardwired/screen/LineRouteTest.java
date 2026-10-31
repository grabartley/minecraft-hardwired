package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.grahambartley.hardwired.screen.CircuitLayout.Point;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LineRouteTest {

  private static final Point FROM = new Point(0, 0);
  private static final Point TO = new Point(40, 20);

  @ParameterizedTest(name = "lane {0} bends at {1}")
  @CsvSource({"2, 20", "0, 16", "4, 24", "5, 16", "-3, 20"})
  void linesBendInTheirLane(int lane, int bendX) {
    assertEquals(
        List.of(FROM, new Point(bendX, 0), new Point(bendX, 20), TO),
        LineRoute.between(FROM, TO, lane));
  }

  @Test
  void neighbouringLanesBendApart() {
    assertNotEquals(LineRoute.between(FROM, TO, 1), LineRoute.between(FROM, TO, 2));
  }

  @Test
  void aRouteRunsOnlyAlongTheAxes() {
    List<Point> route = LineRoute.between(new Point(50, 3), new Point(7, 90), 1);
    for (int index = 1; index < route.size(); index++) {
      Point a = route.get(index - 1);
      Point b = route.get(index);
      assertEquals(true, a.x() == b.x() || a.y() == b.y());
    }
  }
}
