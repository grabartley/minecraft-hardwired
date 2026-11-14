package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireViewer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneEntryTest {

  @ParameterizedTest(name = "exchange {0} number {1} home {2}")
  @CsvSource({"false,105,RED,Kitchen,false", "true,3,'',,true", "false,-1,'',Hall,false"})
  void anEntrySurvivesASave(
      boolean exchange, int number, String home, String name, boolean override) {
    PhoneEntry entry =
        new PhoneEntry(
            exchange,
            List.of(11L, 12L),
            7L,
            number,
            42L,
            home.isEmpty() ? Optional.empty() : Optional.of(Hue.valueOf(home)),
            name == null ? "" : name,
            new WireViewer(new UUID(1, 2), override));
    assertEquals(entry, PhoneEntry.read(entry.write()));
  }

  @ParameterizedTest(name = "number {0}")
  @CsvSource({"-1,false", "100,true"})
  void aNewEntryHasNoNumberUntilNumbered(int number, boolean numbered) {
    PhoneEntry entry = PhoneEntry.added(false, List.of(1L), 0L).numbered(number, 9L);
    assertEquals(numbered, entry.hasNumber());
    assertEquals(9L, entry.numberedBy());
  }

  @ParameterizedTest(name = "name [{0}]")
  @CsvSource({"Kitchen", "''"})
  void placingKeepsTheNumberAndRecordsThePlacer(String name) {
    WireViewer placer = new WireViewer(new UUID(5, 6), false);
    PhoneEntry entry =
        PhoneEntry.added(false, List.of(1L), 0L).numbered(104, 2L).placedBy(name, placer);
    assertEquals(104, entry.number());
    assertEquals(name, entry.name());
    assertEquals(placer, entry.placer());
  }
}
