package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PowerWearTest {

  @ParameterizedTest(name = "{0}")
  @CsvSource({"0,0", "1.5,1", "-0.25,-1", "3,3"})
  void anAmountRoundsDownToWhole(double amount, int whole) {
    assertEquals(whole, PowerWear.whole(amount));
  }

  @ParameterizedTest(name = "load {0} supply {1}")
  @CsvSource({"0,16,0", "1,0,0", "1,64,1", "16,16,15", "8,16,7", "99,16,15"})
  void workloadRunsFromOneToFifteen(int load, int supply, int workload) {
    assertEquals(workload, PowerWear.workload(load, supply));
  }
}
