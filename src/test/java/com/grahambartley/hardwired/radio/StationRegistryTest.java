package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StationRegistryTest {

  private static final BlockPos DESK = new BlockPos(1, 0, 0);
  private static final BlockPos TOWER = new BlockPos(9, 0, 0);

  private static Station on(BlockPos desk, int frequency) {
    return new Station(desk, TOWER, frequency, 100, 0, null, -1);
  }

  @ParameterizedTest(name = "reach {0} then {1}")
  @CsvSource({"100,90,true", "100,100,false", "100,120,false"})
  void publishingSaysWhenReachFell(int first, int second, boolean fell) {
    StationRegistry registry = new StationRegistry();
    registry.publish(new Station(DESK, TOWER, 1000, first, 0, null, -1));
    assertEquals(fell, registry.publish(new Station(DESK, TOWER, 1000, second, 0, null, -1)));
  }

  @ParameterizedTest(name = "on the air {0}")
  @CsvSource({"true", "false"})
  void withdrawingSaysWhetherAStationWent(boolean onAir) {
    StationRegistry registry = new StationRegistry();
    if (onAir) {
      registry.publish(on(DESK, 1000));
    }
    assertEquals(onAir, registry.withdraw(DESK));
  }

  @ParameterizedTest(name = "frequency {0} then {1}")
  @CsvSource({"1000,1010", "530,1600"})
  void publishingAgainReplacesTheDesksStation(int first, int second) {
    StationRegistry registry = new StationRegistry();
    registry.publish(on(DESK, first));
    registry.publish(on(DESK, second));
    assertEquals(List.of(on(DESK, second)), List.copyOf(registry.stations()));
    registry.withdraw(DESK);
    assertEquals(List.of(), List.copyOf(registry.stations()));
  }

  @ParameterizedTest(name = "asked by desk {0}: {1}")
  @CsvSource({"1,false", "2,true"})
  void aTowerIsInUseOnlyByAnotherDesk(int deskX, boolean inUse) {
    StationRegistry registry = new StationRegistry();
    registry.publish(on(DESK, 1000));
    assertEquals(inUse, registry.towerUsedByAnother(TOWER, new BlockPos(deskX, 0, 0)));
  }
}
