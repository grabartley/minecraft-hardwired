package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WirePathTest {

  private static WireFace floor(int x) {
    return new WireFace(new BlockPos(x, 64, 0), Direction.UP);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void bridgesASkippedFace(int distance) {
    List<WireFace> expected =
        IntStream.rangeClosed(1, distance).mapToObj(WirePathTest::floor).toList();
    assertEquals(expected, WirePath.between(floor(0), floor(distance), face -> true, 400));
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 5})
  void fallsBackToTheTargetWhenNoRouteExists(int distance) {
    assertEquals(
        List.of(floor(distance)), WirePath.between(floor(0), floor(distance), face -> false, 400));
  }
}
