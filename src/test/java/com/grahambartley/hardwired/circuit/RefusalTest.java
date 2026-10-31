package com.grahambartley.hardwired.circuit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RefusalTest {

  @ParameterizedTest(name = "{0} has a message")
  @EnumSource(Refusal.class)
  void everyRefusalHasAMessage(Refusal refusal) throws IOException {
    try (var stream = getClass().getResourceAsStream("/assets/hardwired/lang/en_us.json");
        var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      JsonObject lang = JsonParser.parseReader(reader).getAsJsonObject();
      assertTrue(lang.has(refusal.key()), refusal.key());
    }
  }
}
