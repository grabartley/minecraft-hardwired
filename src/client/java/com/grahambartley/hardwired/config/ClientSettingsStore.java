package com.grahambartley.hardwired.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

  private static volatile ClientDisplaySettings current = ClientDisplaySettings.DEFAULTS;
  private static volatile ClientRadioSettings currentRadio = ClientRadioSettings.DEFAULTS;
  private static volatile Path path;

  private ClientSettingsStore() {}

  public static ClientDisplaySettings get() {
    return current;
  }

  public static ClientRadioSettings radio() {
    return currentRadio;
  }

  public static void load(final Path configDir) {
    path = configDir.resolve(FILE);
    currentRadio = readRadio(path);
    current = read(path);
  }

  public static void update(final ClientDisplaySettings settings) {
    update(settings, currentRadio);
  }

  public static void update(final ClientRadioSettings settings) {
    update(current, settings);
  }

  public static void update(
      final ClientDisplaySettings settings, final ClientRadioSettings radioSettings) {
    current = settings;
    currentRadio = radioSettings;
    if (path != null) {
      write(path, settings, radioSettings);
    }
  }

  static ClientDisplaySettings read(final Path file) {
    if (!Files.exists(file)) {
      return ClientDisplaySettings.DEFAULTS;
    }
    try {
      final JsonElement root = JsonParser.parseString(Files.readString(file));
      if (!root.isJsonObject()) {
        throw new JsonParseException("top level is not a JSON object");
      }
      return ClientDisplaySettings.fromJson(root.getAsJsonObject());
    } catch (IOException | JsonParseException ex) {
      LOGGER.warn("Bad Hardwired client settings at {}, using defaults: {}", file, ex.getMessage());
      keepBrokenFile(file);
      return ClientDisplaySettings.DEFAULTS;
    }
  }

  static ClientRadioSettings readRadio(final Path file) {
    if (!Files.exists(file)) {
      return ClientRadioSettings.DEFAULTS;
    }
    try {
      final JsonElement root = JsonParser.parseString(Files.readString(file));
      return root.isJsonObject()
          ? ClientRadioSettings.fromJson(root.getAsJsonObject())
          : ClientRadioSettings.DEFAULTS;
    } catch (IOException | JsonParseException ex) {
      return ClientRadioSettings.DEFAULTS;
    }
  }

  static void write(
      final Path file,
      final ClientDisplaySettings settings,
      final ClientRadioSettings radioSettings) {
    try {
      Files.createDirectories(file.getParent());
      final Path temp = file.resolveSibling(file.getFileName() + ".tmp");
      final JsonObject root = settings.toJson();
      radioSettings.writeJson(root);
      Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
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
