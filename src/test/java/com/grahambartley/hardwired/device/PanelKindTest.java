package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PanelKindTest {

  private static final Path ENGLISH =
      Path.of("src/main/resources/assets/hardwired/lang/en_us.json");

  @ParameterizedTest
  @EnumSource(PanelKind.class)
  void everyKindHasAName(PanelKind kind) throws IOException {
    assertTrue(Files.readString(ENGLISH).contains('"' + kind.translationKey() + '"'));
  }

  @ParameterizedTest
  @EnumSource(PanelKind.class)
  void indexesRoundTrip(PanelKind kind) {
    assertEquals(kind, PanelKind.byIndex(kind.ordinal()));
  }
}
