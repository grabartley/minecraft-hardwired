package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RadioSettingsTest {

  @ParameterizedTest(name = "{0} clamps to {1}")
  @CsvSource({"-1000000,low", "2000000,high"})
  void everyValueIsClamped(int value, String end) {
    RadioSettings s = new RadioSettings(value, value, value, value, value, value, value, value);
    boolean low = end.equals("low");
    assertEquals(
        low ? RadioSettings.TOWER_MIN_MASTS_MIN : RadioSettings.TOWER_MIN_MASTS_MAX,
        s.towerMinMasts());
    assertEquals(
        low ? RadioSettings.TOWER_MAX_MASTS_MIN : RadioSettings.TOWER_MAX_MASTS_MAX,
        s.towerMaxMasts());
    assertEquals(
        low ? RadioSettings.TOWER_BASE_REACH_MIN : RadioSettings.TOWER_BASE_REACH_MAX,
        s.towerBaseReach());
    assertEquals(
        low ? RadioSettings.TOWER_REACH_PER_MAST_MIN : RadioSettings.TOWER_REACH_PER_MAST_MAX,
        s.towerReachPerMast());
    assertEquals(
        low ? RadioSettings.RADIO_HEARING_MIN : RadioSettings.RADIO_HEARING_MAX, s.radioHearing());
    assertEquals(
        low ? RadioSettings.SPEAKER_HEARING_MIN : RadioSettings.SPEAKER_HEARING_MAX,
        s.speakerHearing());
    assertEquals(
        low ? RadioSettings.UNDERGROUND_FREE_DEPTH_MIN : RadioSettings.UNDERGROUND_FREE_DEPTH_MAX,
        s.undergroundFreeDepth());
    assertEquals(
        low ? RadioSettings.UNDERGROUND_LOSS_DEPTH_MIN : RadioSettings.UNDERGROUND_LOSS_DEPTH_MAX,
        s.undergroundLossDepth());
  }

  @ParameterizedTest(name = "with changes only its own field to {0}")
  @CsvSource({"7", "9"})
  void eachWitherChangesOneField(int value) {
    RadioSettings d = RadioSettings.DEFAULTS;
    assertEquals(value, d.withTowerMinMasts(value).towerMinMasts());
    assertEquals(value, d.withTowerMaxMasts(value).towerMaxMasts());
    assertEquals(value, d.withTowerBaseReach(value).towerBaseReach());
    assertEquals(value, d.withTowerReachPerMast(value).towerReachPerMast());
    assertEquals(value, d.withRadioHearing(value).radioHearing());
    assertEquals(value, d.withSpeakerHearing(value).speakerHearing());
    assertEquals(value, d.withUndergroundFreeDepth(value).undergroundFreeDepth());
    assertEquals(value, d.withUndergroundLossDepth(value).undergroundLossDepth());
    assertEquals(d.towerBaseReach(), d.withTowerMinMasts(value).towerBaseReach());
  }
}
