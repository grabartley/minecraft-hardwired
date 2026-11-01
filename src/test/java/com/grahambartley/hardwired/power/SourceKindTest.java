package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.config.PowerSettings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SourceKindTest {

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "CELL,16,true,1,0.5",
    "GENERATOR,64,true,2,1",
    "SOLAR,6,false,0,0.3",
    "DYNAMO,24,false,0,0.8"
  })
  void everySourceKeepsItsFacts(
      SourceKind kind, int rating, boolean fuel, int divisor, float volume) {
    assertEquals(rating, kind.rating(PowerSettings.DEFAULTS));
    assertEquals(fuel, kind.burnsFuel());
    assertEquals(divisor, kind.burnDivisor());
    assertEquals(volume, kind.soundVolume());
  }
}
