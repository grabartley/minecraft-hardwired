package com.grahambartley.hardwired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AdvancementAssetsTest {

  private static final Path ADVANCEMENTS = Path.of("src/main/resources/data/hardwired/advancement");
  private static final Set<String> HIDDEN =
      Set.of("short_circuit", "vintage", "lights_out", "blown_fuse");

  private static JsonObject read(String name) throws IOException {
    return JsonParser.parseString(Files.readString(ADVANCEMENTS.resolve(name + ".json")))
        .getAsJsonObject();
  }

  @ParameterizedTest(name = "the root keeps {0} off")
  @ValueSource(strings = {"show_toast", "announce_to_chat"})
  void theRootIsSilent(String flag) throws IOException {
    assertFalse(read("hardwired").getAsJsonObject("display").get(flag).getAsBoolean());
  }

  @Test
  void theRootIsEarnedByCopperWire() throws IOException {
    JsonObject wire = read("hardwired").getAsJsonObject("criteria").getAsJsonObject("wire");
    assertEquals("minecraft:inventory_changed", wire.get("trigger").getAsString());
    assertEquals(
        "hardwired:copper_wire",
        wire.getAsJsonObject("conditions")
            .getAsJsonArray("items")
            .get(0)
            .getAsJsonObject()
            .get("items")
            .getAsString());
  }

  @ParameterizedTest(name = "{0} hangs from {1}")
  @CsvSource({
    "live_wire,hardwired",
    "let_there_be_light,live_wire",
    "mood_lighting,let_there_be_light",
    "vintage,let_there_be_light",
    "street_smart,live_wire",
    "scene_stealer,live_wire",
    "full_spectrum,live_wire",
    "now_you_see_me,live_wire",
    "going_up,live_wire",
    "smart_home,live_wire",
    "logic_gate,live_wire",
    "charged_up,live_wire",
    "sun_power,live_wire",
    "caught_lightning,live_wire",
    "tuned_in,live_wire",
    "on_the_air,live_wire",
    "live_mic,live_wire",
    "powers_on,charged_up"
  })
  void everyBranchHangsWhereThePrdSays(String name, String parent) throws IOException {
    assertEquals("hardwired:" + parent, read(name).get("parent").getAsString());
  }

  @Test
  void onlyTheFourSecretsAreHidden() throws IOException {
    try (Stream<Path> files = Files.list(ADVANCEMENTS)) {
      for (Path file : files.toList()) {
        String name = file.getFileName().toString().replace(".json", "");
        assertEquals(
            HIDDEN.contains(name),
            read(name).getAsJsonObject("display").get("hidden").getAsBoolean(),
            name);
      }
    }
  }
}
