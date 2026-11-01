package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PowerSettingsTest {

  @ParameterizedTest(name = "{0} clamps to {1}")
  @CsvSource({"-1000000,low", "2000000,high"})
  void everyValueIsClamped(int value, String end) {
    int[] v = new int[9];
    java.util.Arrays.fill(v, value);
    PowerSettings s = new PowerSettings(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
    boolean low = end.equals("low");
    assertEquals(
        low ? PowerSettings.CELL_CAPACITY_MIN : PowerSettings.CELL_CAPACITY_MAX, s.cellCapacity());
    assertEquals(
        low ? PowerSettings.DUST_CHARGE_MIN : PowerSettings.DUST_CHARGE_MAX, s.dustCharge());
    assertEquals(
        low ? PowerSettings.SOLAR_CHARGE_MIN : PowerSettings.SOLAR_CHARGE_MAX,
        s.solarChargePerMinute());
    assertEquals(
        low ? PowerSettings.LIGHTNING_CHARGE_MIN : PowerSettings.LIGHTNING_CHARGE_MAX,
        s.lightningCharge());
    assertEquals(
        low ? PowerSettings.ALARM_CHARGE_MIN : PowerSettings.ALARM_CHARGE_MAX,
        s.alarmChargePerSecond());
    assertEquals(
        low ? PowerSettings.RADIO_CHARGE_MIN : PowerSettings.RADIO_CHARGE_MAX,
        s.radioChargePerSecond());
    assertEquals(
        low ? PowerSettings.SPEAKER_CHARGE_MIN : PowerSettings.SPEAKER_CHARGE_MAX,
        s.speakerChargePerSecond());
    assertEquals(
        low ? PowerSettings.INTERCOM_CHARGE_MIN : PowerSettings.INTERCOM_CHARGE_MAX,
        s.intercomChargePerLine());
    assertEquals(
        low ? PowerSettings.BROADCAST_CHARGE_MIN : PowerSettings.BROADCAST_CHARGE_MAX,
        s.broadcastChargePerSecond());
  }

  @ParameterizedTest(name = "default {0}")
  @CsvSource({"1000"})
  void defaultsSitInsideTheirBounds(int first) {
    assertEquals(first, PowerSettings.DEFAULTS.cellCapacity());
    assertEquals(
        PowerSettings.DEFAULTS,
        new PowerSettings(
            PowerSettings.DEFAULTS.cellCapacity(),
            PowerSettings.DEFAULTS.dustCharge(),
            PowerSettings.DEFAULTS.solarChargePerMinute(),
            PowerSettings.DEFAULTS.lightningCharge(),
            PowerSettings.DEFAULTS.alarmChargePerSecond(),
            PowerSettings.DEFAULTS.radioChargePerSecond(),
            PowerSettings.DEFAULTS.speakerChargePerSecond(),
            PowerSettings.DEFAULTS.intercomChargePerLine(),
            PowerSettings.DEFAULTS.broadcastChargePerSecond()));
  }
}
