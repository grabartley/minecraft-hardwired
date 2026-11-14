package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class LineStatusTest {

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "DIAL_TONE,screen.hardwired.telephone.status.dial_tone,false",
    "RINGING_OUT,screen.hardwired.telephone.status.ringing_out,true",
    "CONNECTED,screen.hardwired.telephone.status.connected,true",
    "BUSY,screen.hardwired.telephone.status.busy,false",
    "TOO_FAST,device.hardwired.too_fast,false"
  })
  void eachStatusNamesItsTextAndWhetherACallIsUp(LineStatus status, String key, boolean onCall) {
    assertEquals(key, status.translationKey());
    assertEquals(onCall, status.isOnCall());
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(LineStatus.class)
  void everyStatusSurvivesItsIndex(LineStatus status) {
    assertEquals(status, LineStatus.byIndex(status.ordinal()));
  }
}
