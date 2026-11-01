package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.config.BurnRates;
import com.grahambartley.hardwired.config.PowerSettings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SourceKindTest {

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "CELL,16,true,7,0.5",
    "GENERATOR,64,true,5,1",
    "SOLAR,6,false,0,0.3",
    "DYNAMO,24,false,0,0.8"
  })
  void everySourceKeepsItsFacts(SourceKind kind, int rating, boolean fuel, int burn, float volume) {
    assertEquals(rating, kind.rating(PowerSettings.DEFAULTS));
    assertEquals(fuel, kind.burnsFuel());
    assertEquals(burn, kind.chargePerLoadMinute(new BurnRates(7, 5, 3)));
    assertEquals(volume, kind.soundVolume());
  }
}
