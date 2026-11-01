package com.grahambartley.hardwired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class AdvancementAssetsTest {

  private static final Path ADVANCEMENTS = Path.of("src/main/resources/data/hardwired/advancement");
  private static final Path RECIPES = Path.of("src/main/resources/data/hardwired/recipe");
  private static final Set<String> HIDDEN =
      Set.of("short_circuit", "vintage", "lights_out", "blown_fuse");

  private static JsonObject read(String name) throws IOException {
    return JsonParser.parseString(Files.readString(ADVANCEMENTS.resolve(name + ".json")))
        .getAsJsonObject();
  }

  @ParameterizedTest(name = "the root does not turn {0} off")
  @ValueSource(strings = {"show_toast", "announce_to_chat"})
  void theRootAnnouncesTheFirstWire(String flag) throws IOException {
    assertFalse(read("hardwired").getAsJsonObject("display").has(flag));
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

  @ParameterizedTest(name = "{0} earns Juiced Up")
  @ValueSource(strings = {"redstone_cell", "redstone_generator", "solar_panel", "lodestone_dynamo"})
  void everySourceEarnsJuicedUp(String source) throws IOException {
    assertTrue(
        read("juiced_up")
            .getAsJsonObject("criteria")
            .getAsJsonObject("source")
            .toString()
            .contains("hardwired:" + source));
  }

  @ParameterizedTest(name = "{0} hangs from {1}")
  @CsvSource({
    "juiced_up,hardwired",
    "let_there_be_light,juiced_up",
    "mood_lighting,let_there_be_light",
    "vintage,let_there_be_light",
    "street_smart,hardwired",
    "scene_stealer,hardwired",
    "full_spectrum,hardwired",
    "now_you_see_me,hardwired",
    "going_up,hardwired",
    "smart_home,hardwired",
    "logic_gate,hardwired",
    "charged_up,juiced_up",
    "sun_power,juiced_up",
    "caught_lightning,juiced_up",
    "tuned_in,hardwired",
    "on_the_air,hardwired",
    "live_mic,hardwired",
    "short_circuit,hardwired",
    "lights_out,charged_up",
    "blown_fuse,charged_up",
    "pocket_circuit,logic_gate",
    "share_the_love,pocket_circuit",
    "powers_on,charged_up"
  })
  void everyBranchHangsWhereThePrdSays(String name, String parent) throws IOException {
    assertEquals("hardwired:" + parent, read(name).get("parent").getAsString());
  }

  @Test
  void onlyTheFourSecretsAreHidden() throws IOException {
    try (Stream<Path> files = Files.list(ADVANCEMENTS)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        String name = file.getFileName().toString().replace(".json", "");
        assertEquals(
            HIDDEN.contains(name),
            read(name).getAsJsonObject("display").get("hidden").getAsBoolean(),
            name);
      }
    }
  }

  static Stream<Path> recipes() throws IOException {
    try (Stream<Path> files = Files.list(RECIPES)) {
      return files.toList().stream();
    }
  }

  private static JsonObject parse(Path file) throws IOException {
    return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
  }

  private static Set<String> ingredientsOf(JsonObject recipe) {
    Set<String> ingredients = new HashSet<>();
    JsonArray slots = recipe.getAsJsonArray("ingredients");
    if (slots != null) {
      slots.forEach(slot -> ingredients.add(ingredientId(slot.getAsJsonObject())));
    } else {
      recipe
          .getAsJsonObject("key")
          .entrySet()
          .forEach(slot -> ingredients.add(ingredientId(slot.getValue().getAsJsonObject())));
    }
    return ingredients;
  }

  private static String ingredientId(JsonObject ingredient) {
    return ingredient.has("tag")
        ? "#" + ingredient.get("tag").getAsString()
        : ingredient.get("item").getAsString();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("recipes")
  void everyRecipeUnlocksFromAllOfItsIngredients(Path recipeFile) throws IOException {
    String name = recipeFile.getFileName().toString();
    String id = "hardwired:" + name.replace(".json", "");
    JsonObject recipe = parse(recipeFile);
    JsonObject unlock =
        parse(
            ADVANCEMENTS.resolve("recipes/" + recipe.get("category").getAsString()).resolve(name));
    assertEquals("minecraft:recipes/root", unlock.get("parent").getAsString());
    assertEquals(
        id, unlock.getAsJsonObject("rewards").getAsJsonArray("recipes").get(0).getAsString());
    JsonObject criteria = unlock.getAsJsonObject("criteria");
    assertEquals(
        id,
        criteria
            .getAsJsonObject("has_the_recipe")
            .getAsJsonObject("conditions")
            .get("recipe")
            .getAsString());
    Set<String> required = new HashSet<>();
    for (JsonElement group : unlock.getAsJsonArray("requirements")) {
      Set<String> members = new HashSet<>();
      group.getAsJsonArray().forEach(member -> members.add(member.getAsString()));
      assertTrue(members.remove("has_the_recipe"));
      assertEquals(1, members.size());
      JsonObject held = criteria.getAsJsonObject(members.iterator().next());
      assertEquals("minecraft:inventory_changed", held.get("trigger").getAsString());
      required.add(
          held.getAsJsonObject("conditions")
              .getAsJsonArray("items")
              .get(0)
              .getAsJsonObject()
              .get("items")
              .getAsString());
    }
    assertEquals(ingredientsOf(recipe), required);
  }

  @Test
  void everyUnlockHasARecipe() throws IOException {
    try (Stream<Path> files = Files.walk(ADVANCEMENTS.resolve("recipes"))) {
      files
          .filter(Files::isRegularFile)
          .forEach(file -> assertTrue(Files.exists(RECIPES.resolve(file.getFileName()))));
    }
  }
}
