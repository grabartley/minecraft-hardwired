package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.config.RadioSettings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TowerColumnTest {

  private static TowerColumn read(String column, int y) {
    return TowerColumn.read(
        y,
        0,
        column.length() - 1,
        index ->
            switch (column.charAt(index)) {
              case 'm' -> TowerColumn.Cell.MAST;
              case 'r' -> TowerColumn.Cell.ROD;
              default -> TowerColumn.Cell.OTHER;
            });
  }

  @ParameterizedTest(name = "{0} from {1}")
  @CsvSource({
    "mmmmmr,0,0,5,true",
    "mmmmmr,3,0,5,true",
    "ammmr,2,1,3,true",
    "ammma,2,1,3,false",
    "mmm,2,0,3,false",
    "mam,0,0,1,false",
    "mam,2,2,1,false"
  })
  void aColumnReadsTheSameFromAnyMast(String column, int y, int foot, int masts, boolean capped) {
    assertEquals(new TowerColumn(foot, masts, capped), read(column, y));
  }

  @ParameterizedTest(name = "{0} masts, capped {1}: stands {2}")
  @CsvSource({"5,true,true", "4,true,false", "5,false,false", "32,true,true"})
  void aTowerNeedsItsHeightAndItsRod(int masts, boolean capped, boolean stands) {
    assertEquals(stands, new TowerColumn(0, masts, capped).stands(RadioSettings.DEFAULTS));
  }

  @ParameterizedTest(name = "{0} masts reach {1}")
  @CsvSource({"5,224", "32,1088", "40,1088"})
  void reachGrowsWithMastsUpToTheLimit(int masts, int reach) {
    assertEquals(reach, new TowerColumn(0, masts, true).reach(RadioSettings.DEFAULTS));
  }
}
