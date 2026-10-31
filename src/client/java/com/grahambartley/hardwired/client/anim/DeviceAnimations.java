package com.grahambartley.hardwired.client.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grahambartley.hardwired.Hardwired;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class DeviceAnimations {

  public record Part(String bone, Identifier model, float[] pivot, boolean glow) {

    public boolean drawnWhile(boolean animating) {
      return animating || !glow;
    }

    public int light(int worldLight) {
      return glow ? LightmapTextureManager.MAX_LIGHT_COORDINATE : worldLight;
    }
  }

  public record Animation(DeviceClip clip, List<Part> parts) {}

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
    return new Animation(DeviceClip.parse(clipJson), List.copyOf(parts));
  }

  static Part part(JsonObject part) {
    JsonArray pivot = part.getAsJsonArray("pivot");
    return new Part(
        part.get("bone").getAsString(),
        Identifier.of(part.get("model").getAsString()),
        new float[] {
          pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat()
        },
        part.has("glow") && part.get("glow").getAsBoolean());
  }
}
