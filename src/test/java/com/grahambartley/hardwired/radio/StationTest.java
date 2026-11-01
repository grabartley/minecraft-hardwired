package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StationTest {

  @ParameterizedTest(name = "({0},{1},{2}) is {3} away")
  @CsvSource({"0,64,0,0", "3,64,4,5", "3,200,4,5", "-3,-20,-4,5"})
  void distanceIsFlatFromTheTower(int x, int y, int z, double expected) {
    Station station = new Station(BlockPos.ORIGIN, new BlockPos(0, 64, 0), 1000, 100, 0, null, -1);
    assertEquals(expected, station.flatDistanceTo(new BlockPos(x, y, z)), 1e-9);
  }
}
