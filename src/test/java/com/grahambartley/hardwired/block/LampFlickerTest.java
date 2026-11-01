package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LampFlickerTest {

  @ParameterizedTest(name = "level {0} wanting {1}, darkened {2}")
  @CsvSource({
    "15,0,true,3",
    "8,0,true,2",
    "4,0,true,0",
    "3,0,true,0",
    "0,0,true,0",
    "15,0,false,0",
    "0,15,false,15",
    "5,15,false,15"
  })
  void onlyADarkGridStepsALampDown(int current, int wanted, boolean darkened, int next) {
    assertEquals(next, LampFlicker.next(current, wanted, darkened));
  }
}
