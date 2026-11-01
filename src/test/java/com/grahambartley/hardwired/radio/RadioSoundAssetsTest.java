package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RadioSoundAssetsTest {

  private static final Path ASSETS = Path.of("src/main/resources/assets/hardwired");

  private static JsonObject event(String name) throws IOException {
    return JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json")))
        .getAsJsonObject()
        .getAsJsonObject(name);
  }

  private static JsonObject lang() throws IOException {
    return JsonParser.parseString(Files.readString(ASSETS.resolve("lang/en_us.json")))
        .getAsJsonObject();
  }

  @ParameterizedTest(name = "{0} has {1} files")
  @CsvSource({"radio.hiss,1", "radio.crackle,6", "radio.sweep,3"})
  void everyNoiseFileExists(String name, int files) throws IOException {
    JsonObject event = event(name);
    assertEquals(files, event.getAsJsonArray("sounds").size());
    for (JsonElement sound : event.getAsJsonArray("sounds")) {
      String path = sound.getAsString().substring("hardwired:".length());
      assertTrue(Files.exists(ASSETS.resolve("sounds/" + path + ".ogg")), path);
    }
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({"radio.voice", "radio.hiss", "radio.crackle", "radio.sweep"})
  void everySoundHasASubtitle(String name) throws IOException {
    assertTrue(lang().has(event(name).get("subtitle").getAsString()));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({"radio.voice"})
  void aVoiceIsAStreamWithNoFileOfItsOwn(String name) throws IOException {
    JsonObject sound = event(name).getAsJsonArray("sounds").get(0).getAsJsonObject();
    assertEquals("fabric-sound-api-v1:empty", sound.get("name").getAsString());
    assertTrue(sound.get("stream").getAsBoolean());
  }
}
