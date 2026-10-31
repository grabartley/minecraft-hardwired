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
}
