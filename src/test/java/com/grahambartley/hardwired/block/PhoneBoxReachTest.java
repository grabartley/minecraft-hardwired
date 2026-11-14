package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneBoxReachTest {

  private static final BlockPos FLOOR = new BlockPos(10, 64, -5);

  @ParameterizedTest(name = "feet at {0},{1},{2} inside {3}")
  @CsvSource({
    "10.5,64.0625,-4.5,true",
    "10.01,65.5,-4.99,true",
    "10.5,66,-4.5,false",
    "10.5,63.9,-4.5,false",
    "11.2,64.0625,-4.5,false",
    "10.5,64.0625,-3.6,false",
    "9.9,64.0625,-4.5,false"
  })
  void onlyAPlayerStandingInTheBoxIsInside(double x, double y, double z, boolean inside) {
    assertEquals(inside, PhoneBoxReach.inside(new Vec3d(x, y, z), FLOOR));
  }

  @ParameterizedTest(name = "inside {0} at depth {1} opens the door {2}")
  @CsvSource({
    "false,0.1,true",
    "false,0.9,true",
    "true,0.1,true",
    "true,0.49,true",
    "true,0.5,false",
    "true,0.9,false"
  })
  void theDoorTurnsFromEitherSideAndOnlyTheInsideReachesThePhone(
      boolean inside, double depth, boolean door) {
    assertEquals(door, PhoneBoxReach.opensDoor(inside, depth));
  }
}
