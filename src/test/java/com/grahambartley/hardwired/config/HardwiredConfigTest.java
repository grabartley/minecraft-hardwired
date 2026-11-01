package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HardwiredConfigTest {

  @TempDir Path dir;

  private Path file() {
    return dir.resolve("server-config.json");
  }

  @Test
  void aMissingFileGivesDefaultsAndWritesThem() {
    assertEquals(HardwiredConfig.defaults(), HardwiredConfig.loadOrCreate(file()));
    assertTrue(Files.exists(file()));
  }

  @Test
  void savedValuesRoundTrip() {
    HardwiredConfig config =
        new HardwiredConfig(
            new WireSettings(64, 1000),
            new LampSettings(250),
            new DeviceSettings(9, 40, 32),
            PowerSettings.DEFAULTS,
            LiftSettings.DEFAULTS,
            CircuitSettings.DEFAULTS,
            new RadioSettings(6, 40, 100, 20, 30, 70, 10, 50, false));
    HardwiredConfig.save(file(), config);
    assertEquals(config, HardwiredConfig.load(file()));
  }

  @Test
  void aBrokenFileGivesDefaultsAndIsKept() throws IOException {
    Files.writeString(file(), "{not json");
    assertEquals(HardwiredConfig.defaults(), HardwiredConfig.load(file()));
    try (Stream<Path> files = Files.list(dir)) {
      assertTrue(files.anyMatch(path -> path.getFileName().toString().contains(".broken.")));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {-5, 0})
  void lowValuesAreClamped(int buttonHoldTicks) throws IOException {
    Files.writeString(file(), "{\"buttonHoldTicks\": " + buttonHoldTicks + "}");
    assertEquals(
        DeviceSettings.BUTTON_HOLD_MIN, HardwiredConfig.load(file()).device().buttonHoldTicks());
  }

  @ParameterizedTest
  @ValueSource(ints = {9999, 100000})
  void highValuesAreClamped(int viewRangeBlocks) throws IOException {
    Files.writeString(file(), "{\"wireViewRangeBlocks\": " + viewRangeBlocks + "}");
    assertEquals(
        WireSettings.VIEW_RANGE_MAX, HardwiredConfig.load(file()).wire().viewRangeBlocks());
  }

  @Test
  void missingFieldsKeepTheirDefaults() throws IOException {
    Files.writeString(file(), "{\"maxNetworkSize\": 512}");
    HardwiredConfig config = HardwiredConfig.load(file());
    assertEquals(512, config.wire().maxNetworkSize());
    assertEquals(WireSettings.DEFAULTS.viewRangeBlocks(), config.wire().viewRangeBlocks());
    assertEquals(DeviceSettings.DEFAULTS, config.device());
  }

  @Test
  void aWorldSavedWithTheOldLiftSpeedKeyMovesToTheNewDefault() throws IOException {
    Path file = dir.resolve("old-speed.json");
    Files.writeString(file, "{\"ticksPerBlock\": 6}");
    assertEquals(
        LiftSettings.DEFAULTS.ticksPerBlock(), HardwiredConfig.load(file).lift().ticksPerBlock());
  }
}
