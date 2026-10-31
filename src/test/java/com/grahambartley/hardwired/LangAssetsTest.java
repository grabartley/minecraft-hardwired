package com.grahambartley.hardwired;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LangAssetsTest {

  private static final Path LANG = Path.of("src/main/resources/assets/hardwired/lang");
  private static final Pattern PLACEHOLDER = Pattern.compile("%[sd%]");

  private static JsonObject read(String code) throws IOException {
    return JsonParser.parseString(Files.readString(LANG.resolve(code + ".json"))).getAsJsonObject();
  }

  private static long placeholders(String text) {
    Matcher matcher = PLACEHOLDER.matcher(text);
    return matcher.results().count();
  }

  @ParameterizedTest(name = "{0} has every key")
  @ValueSource(strings = {"de_de", "es_es", "fr_fr", "pt_br", "ru_ru", "zh_cn"})
  void everyLanguageHasEveryKey(String code) throws IOException {
    Set<String> english = read("en_us").keySet();
    assertEquals(english, read(code).keySet());
  }

  @ParameterizedTest(name = "{0} keeps its placeholders")
  @ValueSource(strings = {"de_de", "es_es", "fr_fr", "pt_br", "ru_ru", "zh_cn"})
  void everyLanguageKeepsPlaceholders(String code) throws IOException {
    JsonObject english = read("en_us");
    JsonObject other = read(code);
    for (String key : english.keySet()) {
      assertEquals(
          placeholders(english.get(key).getAsString()),
          placeholders(other.get(key).getAsString()),
          key);
    }
  }
}
