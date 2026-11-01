package com.grahambartley.hardwired.client.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.AnimatedDevice;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class DeviceAnimations {

  public record Dial(String value, char axis, float from, float to, boolean world) {

    float degrees(float amount) {
      return from + (to - from) * amount;
    }
  }

  public record Part(
      String bone,
      Identifier model,
      float[] pivot,
      boolean glow,
      String when,
      String movesWhen,
      Dial dial) {

    public boolean drawnWhile(boolean animating) {
      return animating || !glow;
    }

    public boolean shownIn(BlockState state) {
      return when == null || AnimatedDevice.flagged(state, when);
    }

    public boolean movesIn(BlockState state) {
      return movesWhen == null || AnimatedDevice.flagged(state, movesWhen);
    }

    public int light(int worldLight) {
      return glow ? LightmapTextureManager.MAX_LIGHT_COORDINATE : worldLight;
    }
  }

  public record Animation(DeviceClip clip, List<Part> parts, int yaw, boolean wide) {}

  private static final String FOLDER = "animated";
  private static final String SUFFIX = ".json";

  private static volatile Map<Identifier, Animation> byBlock = Map.of();

  private DeviceAnimations() {}

  public static Animation of(Identifier block) {
    return byBlock.get(block);
  }

  public static void register() {
    PreparableModelLoadingPlugin.register(
        (manager, executor) -> CompletableFuture.supplyAsync(() -> load(manager), executor),
        (data, context) -> {
          byBlock = data;
          data.values()
              .forEach(
                  animation -> animation.parts().forEach(part -> context.addModels(part.model())));
        });
  }

  private static Map<Identifier, Animation> load(ResourceManager manager) {
    Map<Identifier, Animation> loaded = new HashMap<>();
    manager
        .findResources(FOLDER, id -> id.getPath().endsWith(SUFFIX))
        .forEach(
            (id, resource) -> {
              try {
                loaded.put(blockId(id), read(manager, resource));
              } catch (IOException | RuntimeException ex) {
                Hardwired.LOGGER.error("Bad device animation {}: {}", id, ex.getMessage());
              }
            });
    return Map.copyOf(loaded);
  }

  private static Identifier blockId(Identifier file) {
    String path = file.getPath();
    return Identifier.of(
        file.getNamespace(), path.substring(FOLDER.length() + 1, path.length() - SUFFIX.length()));
  }

  private static Animation read(ResourceManager manager, Resource resource) throws IOException {
    JsonObject root;
    try (Reader reader = resource.getReader()) {
      root = JsonParser.parseReader(reader).getAsJsonObject();
    }
    Identifier clipId = Identifier.of(root.get("clip").getAsString());
    Resource clipFile = manager.getResourceOrThrow(clipId);
    String clipJson;
    try (var stream = clipFile.getInputStream()) {
      clipJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    List<Part> parts = new ArrayList<>();
    for (JsonElement element : root.getAsJsonArray("parts")) {
      parts.add(part(element.getAsJsonObject()));
    }
    int yaw = root.has("yaw") ? root.get("yaw").getAsInt() : 0;
    boolean wide = root.has("wide") && root.get("wide").getAsBoolean();
    return new Animation(DeviceClip.parse(clipJson), List.copyOf(parts), yaw, wide);
  }

  static Part part(JsonObject part) {
    JsonArray pivot = part.getAsJsonArray("pivot");
    return new Part(
        part.get("bone").getAsString(),
        Identifier.of(part.get("model").getAsString()),
        new float[] {
          pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat()
        },
        part.has("glow") && part.get("glow").getAsBoolean(),
        part.has("when") ? part.get("when").getAsString() : null,
        part.has("moves_when") ? part.get("moves_when").getAsString() : null,
        part.has("dial") ? dial(part.getAsJsonObject("dial")) : null);
  }

  private static Dial dial(JsonObject dial) {
    return new Dial(
        dial.get("value").getAsString(),
        dial.get("axis").getAsString().charAt(0),
        dial.get("from").getAsFloat(),
        dial.get("to").getAsFloat(),
        dial.has("world") && dial.get("world").getAsBoolean());
  }
}
