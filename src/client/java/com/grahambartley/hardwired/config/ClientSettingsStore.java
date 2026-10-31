package com.grahambartley.hardwired.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientSettingsStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientSettingsStore.class);
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final String FILE = "hardwired-client.json";
  private static final String BROKEN_FILE = "hardwired-client.broken.json";

  private static volatile ClientWireSettings current = ClientWireSettings.DEFAULTS;
  private static volatile Path path;

  private ClientSettingsStore() {}

  public static ClientWireSettings get() {
    return current;
  }

  public static void load(final Path configDir) {
    path = configDir.resolve(FILE);
    current = read(path);
  }

  public static void update(final ClientWireSettings settings) {
    current = settings;
    if (path != null) {
      write(path, settings);
    }
  }

  static ClientWireSettings read(final Path file) {
    if (!Files.exists(file)) {
      return ClientWireSettings.DEFAULTS;
    }
    try {
      final JsonElement root = JsonParser.parseString(Files.readString(file));
      if (!root.isJsonObject()) {
        throw new JsonParseException("top level is not a JSON object");
      }
      return ClientWireSettings.fromJson(root.getAsJsonObject());
    } catch (IOException | JsonParseException ex) {
      LOGGER.warn("Bad Hardwired client settings at {}, using defaults: {}", file, ex.getMessage());
      keepBrokenFile(file);
      return ClientWireSettings.DEFAULTS;
    }
  }

  static void write(final Path file, final ClientWireSettings settings) {
    try {
      Files.createDirectories(file.getParent());
      final Path temp = file.resolveSibling(file.getFileName() + ".tmp");
      Files.writeString(temp, GSON.toJson(settings.toJson()), StandardCharsets.UTF_8);
      Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      LOGGER.error("Failed to save Hardwired client settings: {}", ex.getMessage());
    }
  }

  private static void keepBrokenFile(final Path file) {
    try {
      Files.move(file, file.resolveSibling(BROKEN_FILE), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      LOGGER.warn("Failed to keep broken Hardwired client settings: {}", ex.getMessage());
    }
  }
}
