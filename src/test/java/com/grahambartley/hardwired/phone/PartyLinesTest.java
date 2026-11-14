package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PartyLinesTest {

  private static Map<Long, Integer> numbers(String list) {
    Map<Long, Integer> numbers = new LinkedHashMap<>();
    String[] values = list.split(" ");
    for (int i = 0; i < values.length; i++) {
      numbers.put((long) i + 1, Integer.parseInt(values[i]));
    }
    return numbers;
  }

  private static List<Long> phones(String list) {
    return list.isEmpty() ? List.of() : Arrays.stream(list.split(" ")).map(Long::valueOf).toList();
  }

  @ParameterizedTest(name = "numbers [{0}] share [{1}] changes [{2}]")
  @CsvSource({
    "'100 101 102','1 2','2=100'",
    "'100 101 102','3 2','3=101'",
    "'100 101 102','1','-'",
    "'100 101','1 9','-'",
    "'100 100 100 101','1 4','4=100'",
    "'100 100 100 100 101','1 5','-'",
    "'100 101 102 103 104','1 2 3 4 5','-'",
    "'100 100','1 2','-'"
  })
  void upToFourPhonesTakeTheLowestChosenNumber(String numbers, String chosen, String changes) {
    assertEquals(expected(changes), PartyLines.share(numbers(numbers), phones(chosen)));
  }

  @ParameterizedTest(name = "numbers [{0}] part [{1}] changes [{2}]")
  @CsvSource({
    "'100 100 101','2','2=-1'",
    "'100 100 101','3','-'",
    "'100 100 100','1 2','1=-1 2=-1'",
    "'100','7','-'"
  })
  void partingGivesASharedPhoneItsOwnNumberAgain(String numbers, String chosen, String changes) {
    assertEquals(expected(changes), PartyLines.part(numbers(numbers), phones(chosen)));
  }

  private static Map<Long, Integer> expected(String changes) {
    Map<Long, Integer> expected = new LinkedHashMap<>();
    if (!changes.equals("-")) {
      for (String change : changes.split(" ")) {
        String[] parts = change.split("=");
        expected.put(Long.valueOf(parts[0]), Integer.valueOf(parts[1]));
      }
    }
    return expected;
  }
}
