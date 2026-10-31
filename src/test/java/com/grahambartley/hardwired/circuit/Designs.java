package com.grahambartley.hardwired.circuit;

import java.util.List;

final class Designs {

  static final int NORTH = Design.sideNode(Side.NORTH);
  static final int EAST = Design.sideNode(Side.EAST);
  static final int SOUTH = Design.sideNode(Side.SOUTH);
  static final int WEST = Design.sideNode(Side.WEST);
  static final List<PortMode> TWO_IN_ONE_OUT =
      List.of(PortMode.INPUT, PortMode.OUTPUT, PortMode.UNUSED, PortMode.INPUT);

  private Designs() {}

  static int part(int index) {
    return Design.partNode(index);
  }

  static Design twoInputPart(PartType type, int setting) {
    return new Design(
        TWO_IN_ONE_OUT,
        List.of(new Part(type, 5, 5, setting)),
        List.of(
            new Line(NORTH, part(0), 0), new Line(WEST, part(0), 1), new Line(part(0), EAST, 0)));
  }

  static Design twoInputPartAt(int x, int y) {
    return new Design(
        List.of(PortMode.INPUT, PortMode.OUTPUT, PortMode.UNUSED, PortMode.INPUT),
        List.of(new Part(PartType.AND, x, y, 0)),
        List.of(
            new Line(NORTH, part(0), 0), new Line(WEST, part(0), 1), new Line(part(0), EAST, 0)));
  }

  static Design oneInputPart(PartType type, int setting) {
    return new Design(
        TWO_IN_ONE_OUT,
        List.of(new Part(type, 5, 5, setting)),
        List.of(new Line(NORTH, part(0), 0), new Line(part(0), EAST, 0)));
  }

  static Design notThroughTimerOscillator(int delay) {
    return new Design(
        List.of(PortMode.UNUSED, PortMode.OUTPUT, PortMode.UNUSED, PortMode.UNUSED),
        List.of(new Part(PartType.NOT, 1, 1, 0), new Part(PartType.TIMER, 2, 1, delay)),
        List.of(
            new Line(part(1), part(0), 0),
            new Line(part(0), part(1), 0),
            new Line(part(1), EAST, 0)));
  }
}
