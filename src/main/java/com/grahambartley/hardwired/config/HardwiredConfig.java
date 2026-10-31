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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record HardwiredConfig(
    WireSettings wire,
    LampSettings lamp,
    DeviceSettings device,
    PowerSettings power,
    LiftSettings lift,
    CircuitSettings circuit) {

  private static final Logger LOGGER = LoggerFactory.getLogger(HardwiredConfig.class);
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public HardwiredConfig {
    wire = wire == null ? WireSettings.DEFAULTS : wire;
    lamp = lamp == null ? LampSettings.DEFAULTS : lamp;
    device = device == null ? DeviceSettings.DEFAULTS : device;
    power = power == null ? PowerSettings.DEFAULTS : power;
    lift = lift == null ? LiftSettings.DEFAULTS : lift;
    circuit = circuit == null ? CircuitSettings.DEFAULTS : circuit;
  }

  public static HardwiredConfig defaults() {
    return new HardwiredConfig(
        WireSettings.DEFAULTS,
        LampSettings.DEFAULTS,
        DeviceSettings.DEFAULTS,
        PowerSettings.DEFAULTS,
        LiftSettings.DEFAULTS,
        CircuitSettings.DEFAULTS);
  }

  public HardwiredConfig withWire(final WireSettings value) {
    return new HardwiredConfig(value, lamp, device, power, lift, circuit);
  }

  public HardwiredConfig withLamp(final LampSettings value) {
    return new HardwiredConfig(wire, value, device, power, lift, circuit);
  }

  public HardwiredConfig withDevice(final DeviceSettings value) {
    return new HardwiredConfig(wire, lamp, value, power, lift, circuit);
  }

  public HardwiredConfig withPower(final PowerSettings value) {
    return new HardwiredConfig(wire, lamp, device, value, lift, circuit);
  }

  public HardwiredConfig withLift(final LiftSettings value) {
    return new HardwiredConfig(wire, lamp, device, power, value, circuit);
  }

  public HardwiredConfig withCircuit(final CircuitSettings value) {
    return new HardwiredConfig(wire, lamp, device, power, lift, value);
  }

  public static HardwiredConfig load(Path path) {
    if (path == null || !Files.exists(path)) {
      return defaults();
    }
    final String json;
    try {
      json = Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      LOGGER.warn(
          "Failed to read Hardwired config at {}, using defaults: {}", path, ex.getMessage());
      return defaults();
    }
    try {
      final JsonElement root = JsonParser.parseString(json);
      if (root.isJsonNull()) {
        return defaults();
      }
      if (!root.isJsonObject()) {
        throw new JsonParseException("top level is not a JSON object");
      }
      return fromJson(root.getAsJsonObject());
    } catch (JsonParseException ex) {
      backupBrokenFile(path);
      LOGGER.warn(
          "Malformed Hardwired config at {} backed up and defaults loaded: {}",
          path,
          ex.getMessage());
      return defaults();
    }
  }

  public static HardwiredConfig loadOrCreate(Path path) {
    if (path != null && !Files.exists(path)) {
      final HardwiredConfig defaults = defaults();
      save(path, defaults);
      return defaults;
    }
    return load(path);
  }

  public static boolean save(Path path, HardwiredConfig config) {
    if (path == null || config == null) {
      return false;
    }
    try {
      Files.createDirectories(path.getParent());
      final JsonObject root = toJson(config);
      final Path tempFile = path.resolveSibling(path.getFileName().toString() + ".tmp");
      Files.writeString(tempFile, GSON.toJson(root), StandardCharsets.UTF_8);
      Files.move(
          tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException ex) {
      LOGGER.error("Failed to save Hardwired config at {}: {}", path, ex.getMessage());
      return false;
    }
  }

  static HardwiredConfig fromJson(JsonObject root) {
    final ConfigJsonFields fields = new ConfigJsonFields(root);
    return new HardwiredConfig(
        WireSettings.fromJson(fields),
        LampSettings.fromJson(fields),
        DeviceSettings.fromJson(fields),
        PowerSettings.fromJson(fields),
        LiftSettings.fromJson(fields),
        CircuitSettings.fromJson(fields));
  }

  static JsonObject toJson(HardwiredConfig config) {
    final JsonObject root = new JsonObject();
    config.wire.writeJson(root);
    config.lamp.writeJson(root);
    config.device.writeJson(root);
    config.power.writeJson(root);
    config.lift.writeJson(root);
    config.circuit.writeJson(root);
    return root;
  }

  private static void backupBrokenFile(Path path) {
    try {
      final String fileName = path.getFileName().toString();
      final String baseName =
          fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
      final String timestamp =
          DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");
      final Path backup = path.resolveSibling(baseName + ".broken." + timestamp + ".json");
      Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      LOGGER.warn("Failed to back up malformed Hardwired config: {}", ex.getMessage());
    }
  }
}
