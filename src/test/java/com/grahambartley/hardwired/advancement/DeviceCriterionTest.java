package com.grahambartley.hardwired.advancement;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.wire.Hue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DeviceCriterionTest {

  private static final Path FULL_SPECTRUM =
      Path.of("src/main/resources/data/hardwired/advancement/full_spectrum.json");

  @ParameterizedTest
  @EnumSource(value = Hue.class, mode = EnumSource.Mode.EXCLUDE, names = "PLAIN")
  void everyDyeColourEventIsOneTheAdvancementListensFor(Hue hue) throws IOException {
    String advancement = Files.readString(FULL_SPECTRUM, StandardCharsets.UTF_8);
    assertTrue(advancement.contains('"' + DeviceCriterion.colorEvent(hue) + '"'));
  }
}
