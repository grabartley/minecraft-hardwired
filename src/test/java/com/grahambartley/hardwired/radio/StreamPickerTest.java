package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.radio.StreamPicker.Candidate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StreamPickerTest {

  private static List<Candidate<String>> candidates(String spec) {
    return Arrays.stream(spec.split(" "))
        .filter(part -> !part.isEmpty())
        .map(
            part -> {
              String[] fields = part.split(":");
              return new Candidate<>(
                  fields[0], Double.parseDouble(fields[1]), Double.parseDouble(fields[2]));
            })
        .toList();
  }

  @ParameterizedTest(name = "{0} limit {1}")
  @CsvSource({
    "'a:5:24 b:1:24',2,b a",
    "'a:30:24',2,''",
    "'a:30:64 b:30:24',2,a",
    "'a:1:24 b:2:24 c:3:24',2,a b",
    "'',2,''"
  })
  void onlyTheNearestSourcesInHearingPlay(String spec, int limit, String expected) {
    List<String> wanted = expected.isEmpty() ? List.of() : Arrays.asList(expected.split(" "));
    assertEquals(wanted, StreamPicker.nearest(candidates(spec), limit));
  }
}
