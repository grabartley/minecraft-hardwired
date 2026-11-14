package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.grahambartley.hardwired.config.PhoneSettings;
import com.grahambartley.hardwired.wire.TripDetector;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.LongStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BellRingTest {

  private static final long CALLER = 1L;
  private static final long PHONE = 2L;
  private static final long OTHER_PHONE = 3L;
  private static final long PHONE_BOX = 4L;
  private static final long PHONE_BOX_UPPER = 5L;
  private static final long STARTED = 1000L;
  private static final Function<Long, Collection<Long>> CELLS =
      phone ->
          Map.<Long, Collection<Long>>of(PHONE_BOX, List.of(PHONE_BOX, PHONE_BOX_UPPER))
              .getOrDefault(phone, List.of(phone));

  private static Call call(List<Long> called) {
    return new Call(CALLER, UUID.randomUUID(), called, Set.of(), STARTED);
  }

  @ParameterizedTest(name = "phone {0} {1} ticks in, touching {2}: rings {3}")
  @CsvSource({
    "2,0,2,true",
    "2,39,2,true",
    "2,40,2,false",
    "2,119,2,false",
    "2,120,2,true",
    "2,0,3,false",
    "2,0,1,false",
    "4,0,5,true",
    "4,60,5,false"
  })
  void ringsOnTheStrokesOfACalledPhoneItTouches(
      long called, long ticks, long touched, boolean rings) {
    assertEquals(
        rings,
        BellRing.ringing(List.of(call(List.of(called))), Set.of(touched), CELLS, STARTED + ticks)
            .isPresent());
  }

  @ParameterizedTest(name = "answered after {0} ticks")
  @ValueSource(longs = {0, 10, 39})
  void anAnsweredCallStopsTheBell(long ticks) {
    Call call = call(List.of(PHONE));
    call.answer(PHONE, UUID.randomUUID());
    assertFalse(BellRing.ringing(List.of(call), Set.of(PHONE), CELLS, STARTED + ticks).isPresent());
  }

  @ParameterizedTest(name = "party line {0}")
  @ValueSource(booleans = {false, true})
  void theRingPatternNeverTripsANetwork(boolean party) {
    Call call = call(party ? List.of(PHONE, OTHER_PHONE) : List.of(PHONE));
    TripDetector detector = new TripDetector();
    long ringTicks = PhoneSettings.RING_SECONDS_MAX * 20L;
    boolean[] last = {false};
    boolean tripped =
        LongStream.range(0, ringTicks)
            .anyMatch(
                tick -> {
                  boolean on =
                      BellRing.ringing(List.of(call), Set.of(PHONE), CELLS, STARTED + tick)
                          .isPresent();
                  boolean changed = on != last[0];
                  last[0] = on;
                  return changed && detector.recordChange(STARTED + tick);
                });
    assertFalse(tripped);
  }
}
