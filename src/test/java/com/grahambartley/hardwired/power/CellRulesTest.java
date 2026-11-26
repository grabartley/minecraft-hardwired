package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CellRulesTest {

  @ParameterizedTest(name = "{0} of {1} shows {2}")
  @CsvSource({
    "0,1000,0",
    "1,1000,1",
    "250,1000,1",
    "251,1000,2",
    "500,1000,2",
    "750,1000,3",
    "751,1000,4",
    "1000,1000,4",
    "5000,1000,4",
    "99999,100000,4",
    "2147483647,100,4"
  })
  void levelIsEmptyOnlyAtZeroAndFullBeforeTheLastCharge(int charge, int capacity, int level) {
    assertEquals(level, CellRules.level(charge, capacity));
  }

  @ParameterizedTest(name = "{0} of {1} worth {2}")
  @CsvSource({
    "0,1000,10,true",
    "990,1000,10,true",
    "991,1000,10,false",
    "1000,1000,10,false",
    "0,100,1000,false",
    "1500,1000,10,false",
    "0,1000,0,false",
    "2147483647,2147483647,1,false"
  })
  void aCellTakesAnItemOnlyWhenAllOfItFits(int charge, int capacity, int worth, boolean takes) {
    assertEquals(takes, CellRules.takes(charge, capacity, worth));
  }

  @ParameterizedTest(name = "dust {0} block {1}")
  @CsvSource({"10,false,10", "10,true,90", "1000,true,9000"})
  void aRedstoneBlockIsWorthNineDust(int dust, boolean block, int worth) {
    assertEquals(worth, CellRules.worth(dust, block));
  }

  @ParameterizedTest(name = "charge {0} of {1} at worth {2} with {3} held takes {4}")
  @CsvSource({
    "0,1000,10,64,64",
    "0,1000,10,200,100",
    "965,1000,10,64,3",
    "991,1000,10,64,0",
    "1000,1000,10,64,0",
    "1200,1000,10,64,0",
    "0,1000,90,64,11",
    "950,1000,90,3,0",
    "0,1000,0,64,0"
  })
  void aHeldStackGivesAsManyItemsAsFit(int charge, int capacity, int worth, int held, int taken) {
    assertEquals(taken, CellRules.itemsThatFit(charge, capacity, worth, held));
  }

  @ParameterizedTest(name = "{0} to {1} crosses empty: {2}")
  @CsvSource({
    "1,0,true",
    "16,0,true",
    "0,1,true",
    "0,64,true",
    "2,1,false",
    "5,9,false",
    "0,0,false",
    "1,1,false"
  })
  void aSourceIsToldToStopOrStartOnlyWhenItsChargeCrossesEmpty(
      int before, int after, boolean crosses) {
    assertEquals(crosses, CellRules.crossesEmpty(before, after));
  }

  @ParameterizedTest(name = "charge {0}, part {1}, step {2} leaves part {3}")
  @CsvSource({
    "0,-0.06,0.53,-0.06",
    "0,-0.59,0.53,-0.53",
    "0,-2.2,3.2,-2.2",
    "0,-5.0,3.2,-3.2",
    "0,0.4,0.53,0.4",
    "1,-0.59,0.53,-0.59",
    "3,-0.99,0.1,-0.99"
  })
  void anEmptySourceNeverRunsMoreThanOneBurnStepBelowZero(
      int charge, double part, double step, double expected) {
    assertEquals(expected, CellRules.partAfterBurn(charge, part, step), 1e-9);
  }
}
