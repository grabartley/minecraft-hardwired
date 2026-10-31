package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.circuit.PartType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PartGlyphTest {

  @ParameterizedTest(name = "{0} fills the glyph square")
  @EnumSource(PartType.class)
  void glyphsAreSquare(PartType type) {
    List<String> rows = PartGlyph.rows(type);
    assertEquals(PartGlyph.SIZE, rows.size());
    rows.forEach(row -> assertEquals(PartGlyph.SIZE, row.length(), row));
    assertTrue(rows.stream().anyMatch(row -> row.contains("#")));
  }

  @Test
  void noTwoPartsShareAGlyph() {
    long distinct = Arrays.stream(PartType.values()).map(PartGlyph::rows).distinct().count();
    assertEquals(PartType.values().length, distinct);
  }
}
