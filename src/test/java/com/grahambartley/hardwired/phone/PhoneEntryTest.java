package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.Hue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneEntryTest {

  @ParameterizedTest(name = "exchange {0} number {1} home [{2}] trusted {4}")
  @CsvSource({"false,105,RED,Kitchen,true", "true,3,'','',true", "false,-1,'',Hall,false"})
  void anEntrySurvivesASave(
      boolean exchange, int number, String home, String name, boolean trusted) {
    PhoneEntry entry =
        new PhoneEntry(
            exchange,
            List.of(11L, 12L),
            7L,
            number,
            42L,
            home.isEmpty() ? Optional.empty() : Optional.of(Hue.valueOf(home)),
            name,
            new UUID(1, 2),
            trusted);
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
    UUID placer = new UUID(5, 6);
    PhoneEntry entry =
        PhoneEntry.added(false, List.of(1L), 0L).numbered(104, 2L).placedBy(name, placer);
    assertEquals(104, entry.number());
    assertEquals(name, entry.name());
    assertEquals(placer, entry.placer());
  }

  @ParameterizedTest(name = "{0} characters")
  @CsvSource({"10,10", "50,50", "51,50", "400,50"})
  void aNameIsCutToFitTheDirectory(int length, int kept) {
    assertEquals(kept, PhoneEntry.fitName("a".repeat(length)).length());
  }

  @ParameterizedTest(name = "pair at {0}")
  @CsvSource({"49,49", "48,50"})
  void aNameIsNeverCutThroughAPairedCharacter(int before, int kept) {
    String name = "a".repeat(before) + "📞" + "b".repeat(10);
    assertEquals(kept, PhoneEntry.fitName(name).length());
  }

  @ParameterizedTest(name = "numbers {0}")
  @CsvSource({"'-1',false", "'104',true", "'104,-1',true", "'-1,-1',false"})
  void aPhoneKeepsItsTrustOnceItHasHadANumber(String numbers, boolean trusted) {
    PhoneEntry entry = PhoneEntry.added(false, List.of(1L), 0L);
    for (String number : numbers.split(",")) {
      entry = entry.numbered(Integer.parseInt(number), 2L);
    }
    assertEquals(trusted, entry.trusted());
  }
}
