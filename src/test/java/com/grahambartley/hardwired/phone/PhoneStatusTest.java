package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.device.DeviceStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneStatusTest {

  @ParameterizedTest(name = "listed {0} exchange {1} system {2} numbered {3} allowed {4}")
  @CsvSource({
    "false,false,false,false,false,NO_EXCHANGE",
    "false,true,true,true,true,NO_EXCHANGE",
    "true,false,false,false,true,NO_EXCHANGE",
    "true,true,false,false,true,NO_TELEPHONE",
    "true,false,true,true,false,READY",
    "true,true,true,true,true,READY",
    "true,false,true,false,true,NETWORK_FULL",
    "true,true,true,false,true,NETWORK_FULL",
    "true,false,true,false,false,SEALED_OUT"
  })
  void aDeviceSaysWhyItHasNoLine(
      boolean listed,
      boolean exchange,
      boolean onSystem,
      boolean numbered,
      boolean allowed,
      DeviceStatus expected) {
    assertEquals(expected, PhoneStatus.of(listed, exchange, onSystem, numbered, allowed));
  }
}
