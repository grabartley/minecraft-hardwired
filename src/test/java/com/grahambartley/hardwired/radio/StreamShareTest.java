package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StreamShareTest {

  private static List<StreamShare.Wants<Integer>> devices(int count, boolean voice, boolean song) {
    List<StreamShare.Wants<Integer>> wants = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      wants.add(new StreamShare.Wants<>(i, voice, song));
    }
    return wants;
  }

  @ParameterizedTest(name = "{0} devices, voice {1}, song {2}")
  @CsvSource({
    "2,true,true,2,2",
    "3,true,true,3,3",
    "4,true,true,4,2",
    "6,true,true,6,0",
    "8,true,true,6,0",
    "8,false,true,0,6",
    "8,true,false,6,0",
    "0,true,true,0,0"
  })
  void voicesComeFirstAndSongsShareWhatIsLeft(
      int count, boolean voice, boolean song, int voices, int songs) {
    StreamShare.Granted<Integer> granted = StreamShare.share(devices(count, voice, song), 6);
    assertEquals(voices, granted.voices().size());
    assertEquals(songs, granted.songs().size());
  }

  @ParameterizedTest(name = "limit {0}")
  @CsvSource({"3,0,1,2", "4,0,1,3"})
  void theNearestAreServedFirst(int limit, int nearest, int next, int last) {
    StreamShare.Granted<Integer> granted =
        StreamShare.share(devices(limit + 2, false, true), limit);
    assertEquals(limit, granted.songs().size());
    assertEquals(true, granted.songs().contains(nearest));
    assertEquals(true, granted.songs().contains(next));
    assertEquals(true, granted.songs().contains(last));
  }
}
