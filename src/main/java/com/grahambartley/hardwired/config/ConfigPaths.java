package com.grahambartley.hardwired.config;

import java.nio.file.Path;

public final class ConfigPaths {
  private static final String HARDWIRED_DIR = "hardwired";
  private static final String SERVER_CONFIG_FILE = "server-config.json";

  private final Path hardwiredDir;

  public ConfigPaths(Path worldDir) {
    this.hardwiredDir = worldDir.resolve(HARDWIRED_DIR);
  }

  public Path getServerConfigPath() {
    return hardwiredDir.resolve(SERVER_CONFIG_FILE);
  }
}
