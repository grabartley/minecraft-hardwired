package com.grahambartley.hardwired.client.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class MicSessionTest {

  private static final class FakeMic implements MicSession.Mic {
    int starts;
    int stops;
    Consumer<byte[]> frames;
    Consumer<MicProblem> problems;

    @Override
    public Runnable start(Consumer<byte[]> frames, Consumer<MicProblem> problems) {
      starts++;
      this.frames = frames;
      this.problems = problems;
      return () -> stops++;
    }
  }

  @ParameterizedTest(name = "{0} opens")
  @ValueSource(ints = {1, 2, 5})
  void openingAnOpenMicStartsItOnce(int opens) {
    FakeMic fake = new FakeMic();
    MicSession session = new MicSession(fake, Runnable::run);
    for (int open = 0; open < opens; open++) {
      session.open(frame -> {}, () -> {});
    }
    assertEquals(1, fake.starts);
    assertTrue(session.isOpen());
    session.close();
    session.close();
    assertEquals(1, fake.stops);
    assertFalse(session.isOpen());
  }

  @ParameterizedTest(name = "{0} frames")
  @ValueSource(ints = {0, 1, 3})
  void framesGoOutOnlyWhileOpen(int frames) {
    FakeMic fake = new FakeMic();
    MicSession session = new MicSession(fake, Runnable::run);
    List<byte[]> sent = new ArrayList<>();
    session.open(sent::add, () -> {});
    for (int frame = 0; frame < frames; frame++) {
      fake.frames.accept(new byte[] {(byte) frame});
    }
    session.close();
    fake.frames.accept(new byte[] {9});
    assertEquals(frames, sent.size());
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(MicProblem.class)
  void aFailureClosesTheMicAndIsRememberedUntilTheNextOpen(MicProblem failure) {
    FakeMic fake = new FakeMic();
    MicSession session = new MicSession(fake, Runnable::run);
    int[] failures = {0};
    session.open(frame -> {}, () -> failures[0]++);
    fake.problems.accept(failure);
    assertEquals(failure, session.problem());
    assertFalse(session.isOpen());
    assertEquals(1, failures[0]);
    session.open(frame -> {}, () -> {});
    assertNull(session.problem());
    assertEquals(2, fake.starts);
  }
}
