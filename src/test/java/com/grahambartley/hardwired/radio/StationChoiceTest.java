package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StationChoiceTest {

  private static Reception heard(int x, long since, double strength) {
    Station station =
        new Station(new BlockPos(x, 0, 0), BlockPos.ORIGIN, 1000, 100, since, null, -1);
    return new Reception(station, strength);
  }

  private static final Reception STRONG = heard(1, 500, 0.9);
  private static final Reception WEAK_OLD = heard(2, 10, 0.3);
  private static final Reception TIE_OLD = heard(3, 100, 0.5);
  private static final Reception TIE_NEW = heard(4, 200, 0.5);
  private static final Reception TIE_NEW_SAME = heard(5, 200, 0.5);
  private static final Reception NOTHING = heard(6, 1, 0);

  private static Stream<Arguments> cases() {
    return Stream.of(
        Arguments.of("strongest wins", List.of(WEAK_OLD, STRONG), STRONG),
        Arguments.of("a tie goes to the longest on air", List.of(TIE_NEW, TIE_OLD), TIE_OLD),
        Arguments.of("a full tie is stable", List.of(TIE_NEW_SAME, TIE_NEW), TIE_NEW),
        Arguments.of("no signal is not heard", List.of(NOTHING), null),
        Arguments.of("nothing on the air", List.of(), null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  void aRadioPlaysTheStrongestStationItFinds(
      String name, List<Reception> heard, Reception expected) {
    assertEquals(Optional.ofNullable(expected), StationChoice.best(heard));
  }
}
