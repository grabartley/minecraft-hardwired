package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RadioSettingsTest {

  @ParameterizedTest(name = "{0} clamps to {1}")
  @CsvSource({"-1000000,low", "2000000,high"})
  void everyValueIsClamped(int value, String end) {
    RadioSettings s =
        new RadioSettings(value, value, value, value, value, value, value, value, true);
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

  @ParameterizedTest(name = "voice broadcasts {0}")
  @ValueSource(booleans = {true, false})
  void voiceBroadcastsSurviveEveryWither(boolean voice) {
    RadioSettings d = RadioSettings.DEFAULTS.withVoiceBroadcasts(voice);
    assertEquals(voice, d.withUndergroundLossDepth(9).voiceBroadcasts());
    assertEquals(voice, d.withUndergroundFreeDepth(9).voiceBroadcasts());
    assertEquals(voice, d.withTowerMinMasts(9).voiceBroadcasts());
    JsonObject root = new JsonObject();
    d.writeJson(root);
    assertEquals(voice, RadioSettings.fromJson(new ConfigJsonFields(root)).voiceBroadcasts());
  }

  @ParameterizedTest(name = "min {0} and max {1} in order: {2}")
  @CsvSource({"5,32,true", "32,32,true", "1,1,true", "33,32,false", "40,33,false", "64,1,false"})
  void towerMastsAreInOrderWhenTheMinimumIsNotAboveTheMaximum(int min, int max, boolean inOrder) {
    RadioSettings s = RadioSettings.DEFAULTS.withTowerMaxMasts(max).withTowerMinMasts(min);
    assertEquals(inOrder, s.towerMastsInOrder());
  }

  @ParameterizedTest(name = "a file with min {0} and max {1} loads as {2}..{1}")
  @CsvSource({"40,33,33", "64,1,1", "33,32,32", "32,32,32", "5,32,5"})
  void aFileWithTheMinimumAboveTheMaximumLoadsInOrder(int min, int max, int loadedMin) {
    JsonObject root = new JsonObject();
    root.addProperty(RadioSettings.KEY_TOWER_MIN_MASTS, min);
    root.addProperty(RadioSettings.KEY_TOWER_MAX_MASTS, max);
    RadioSettings loaded = RadioSettings.fromJson(new ConfigJsonFields(root));
    assertEquals(loadedMin, loaded.towerMinMasts());
    assertEquals(max, loaded.towerMaxMasts());
    assertTrue(loaded.towerMastsInOrder());
  }
}
