package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClientSettingsStoreTest {

  @TempDir Path dir;

  @ParameterizedTest(name = "{0}% {1}% markers {2}")
  @CsvSource({"44,100,true", "10,30,false", "100,50,true"})
  void settingsRoundTrip(int through, int surface, boolean markers) {
    Path file = dir.resolve("settings.json");
    ClientWireSettings settings = new ClientWireSettings(through, surface, markers);
    ClientSettingsStore.write(file, settings);
    assertEquals(settings, ClientSettingsStore.read(file));
  }

  @ParameterizedTest(name = "broken file: {0}")
  @ValueSource(strings = {"{not json", "[1,2]", "7"})
  void brokenFileGivesDefaultsAndIsKept(String body) throws IOException {
    Path file = dir.resolve("settings.json");
    Files.writeString(file, body);
    assertEquals(ClientWireSettings.DEFAULTS, ClientSettingsStore.read(file));
    assertTrue(Files.exists(dir.resolve("hardwired-client.broken.json")));
  }

  @ParameterizedTest(name = "missing {0}")
  @ValueSource(strings = {"absent.json"})
  void missingFileGivesDefaults(String name) {
    assertEquals(ClientWireSettings.DEFAULTS, ClientSettingsStore.read(dir.resolve(name)));
  }
}
