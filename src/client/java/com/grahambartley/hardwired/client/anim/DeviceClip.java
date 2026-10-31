package com.grahambartley.hardwired.client.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DeviceClip {

  public record Pose(float[] rotation, float[] position) {
    static final Pose REST = new Pose(new float[3], new float[3]);
  }

  private record Key(float time, float[] before, float[] after) {}

  private record Channels(List<Key> rotation, List<Key> position) {}

  private static final String LOOP = "loop";
  private static final float MIN_LENGTH = 0.05F;

  private final float length;
  private final Map<String, Channels> bones;

  private DeviceClip(float length, Map<String, Channels> bones) {
    this.length = length;
    this.bones = bones;
  }

  public static DeviceClip parse(String json) {
    JsonObject animations =
        JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("animations");
    JsonObject loop =
        animations.has(LOOP)
            ? animations.getAsJsonObject(LOOP)
            : animations.entrySet().iterator().next().getValue().getAsJsonObject();
    Map<String, Channels> bones = new HashMap<>();
    float longest = 0;
    if (loop.has("bones")) {
      for (Map.Entry<String, JsonElement> bone : loop.getAsJsonObject("bones").entrySet()) {
        JsonObject body = bone.getValue().getAsJsonObject();
        Channels channels = new Channels(keys(body.get("rotation")), keys(body.get("position")));
        bones.put(bone.getKey(), channels);
        longest = Math.max(longest, lastTime(channels.rotation));
        longest = Math.max(longest, lastTime(channels.position));
      }
    }
    float length =
        loop.has("animation_length") ? loop.get("animation_length").getAsFloat() : longest;
    return new DeviceClip(Math.max(length, MIN_LENGTH), bones);
  }

  public float length() {
    return length;
  }

  public Pose sample(String bone, float seconds) {
    Channels channels = bones.get(bone);
    if (channels == null) {
      return Pose.REST;
    }
    float time = seconds % length;
    return new Pose(
        DeviceClipAxes.rotation(sample(channels.rotation, time)),
        DeviceClipAxes.position(sample(channels.position, time)));
  }

  private static float lastTime(List<Key> keys) {
    return keys.isEmpty() ? 0 : keys.get(keys.size() - 1).time;
  }

  private static float[] sample(List<Key> keys, float time) {
    if (keys.isEmpty()) {
      return new float[3];
    }
    Key previous = keys.get(0);
    if (time <= previous.time) {
      return previous.before;
    }
    for (Key next : keys) {
      if (next.time >= time) {
        float span = next.time - previous.time;
        float progress = span <= 0 ? 1 : (time - previous.time) / span;
        return lerp(previous.after, next.before, progress);
      }
      previous = next;
    }
    return previous.after;
  }

  private static float[] lerp(float[] from, float[] to, float progress) {
    float[] mixed = new float[3];
    for (int axis = 0; axis < 3; axis++) {
      mixed[axis] = from[axis] + (to[axis] - from[axis]) * progress;
    }
    return mixed;
  }

  private static List<Key> keys(JsonElement channel) {
    List<Key> keys = new ArrayList<>();
    if (channel == null) {
      return keys;
    }
    if (!channel.isJsonObject()
        || channel.getAsJsonObject().has("pre")
        || channel.getAsJsonObject().has("post")) {
      keys.add(key(0, channel));
      return keys;
    }
    for (Map.Entry<String, JsonElement> entry : channel.getAsJsonObject().entrySet()) {
      keys.add(key(Float.parseFloat(entry.getKey()), entry.getValue()));
    }
    keys.sort(Comparator.comparingDouble(Key::time));
    return keys;
  }

  private static Key key(float time, JsonElement value) {
    if (value.isJsonObject()) {
      JsonObject split = value.getAsJsonObject();
      float[] after = vector(split.has("post") ? split.get("post") : split.get("pre"));
      float[] before = split.has("pre") ? vector(split.get("pre")) : after;
      return new Key(time, before, after);
    }
    float[] both = vector(value);
    return new Key(time, both, both);
  }

  private static float[] vector(JsonElement value) {
    float[] vector = new float[3];
    if (value.isJsonArray()) {
      JsonArray array = value.getAsJsonArray();
      for (int axis = 0; axis < Math.min(3, array.size()); axis++) {
        vector[axis] = number(array.get(axis));
      }
    } else {
      java.util.Arrays.fill(vector, number(value));
    }
    return vector;
  }

  private static float number(JsonElement value) {
    try {
      return value.getAsFloat();
    } catch (NumberFormatException ex) {
      return 0;
    }
  }
}
