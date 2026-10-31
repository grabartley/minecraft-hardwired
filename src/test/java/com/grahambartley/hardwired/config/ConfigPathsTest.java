package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigPathsTest {

  @ParameterizedTest(name = "world {0}")
  @ValueSource(strings = {"saves/New World", "world"})
  void settingsLiveInTheWorldFolder(String world) {
    Path root = Path.of(world);
    assertEquals(
        root.resolve("hardwired").resolve("server-config.json"),
        new ConfigPaths(root).getServerConfigPath());
  }
}
