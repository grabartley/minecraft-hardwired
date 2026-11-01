package com.grahambartley.hardwired.client.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class VoiceCaptureTest {

  private static final long WAIT_SECONDS = 10;

  private static final class FakeSource implements CaptureSource {

    private final short sample;
    private final MicProblem failsToOpen;
    private final MicProblem failsAfterFrames;
    private final int framesBeforeFailing;
    private final CountDownLatch blockedInRead = new CountDownLatch(1);
    private final CountDownLatch readReturned = new CountDownLatch(1);
    private final CountDownLatch closed = new CountDownLatch(1);
    private final Semaphore closes = new Semaphore(0);
    private final boolean blocks;
    private volatile int reads;

    private FakeSource(
        short sample,
        MicProblem failsToOpen,
        MicProblem failsAfterFrames,
        int framesBeforeFailing,
        boolean blocks) {
      this.sample = sample;
      this.failsToOpen = failsToOpen;
      this.failsAfterFrames = failsAfterFrames;
      this.framesBeforeFailing = framesBeforeFailing;
      this.blocks = blocks;
    }

    static FakeSource giving(short sample) {
      return new FakeSource(sample, null, null, 0, false);
    }

    static FakeSource blocking() {
      return new FakeSource((short) 1, null, null, 0, true);
    }

    static FakeSource refusingToOpen(MicProblem problem) {
      return new FakeSource((short) 1, problem, null, 0, false);
    }

    static FakeSource failingAfter(int frames, MicProblem problem) {
      return new FakeSource((short) 1, null, problem, frames, false);
    }

    @Override
    public void open() throws MicFailure {
      if (failsToOpen != null) {
        throw new MicFailure(failsToOpen);
      }
    }

    @Override
    public void read(short[] frame) throws MicFailure {
      if (blocks) {
        blockedInRead.countDown();
        await(closed);
        readReturned.countDown();
        throw new MicFailure(MicProblem.NO_INPUT);
      }
      if (failsAfterFrames != null && reads >= framesBeforeFailing) {
        throw new MicFailure(failsAfterFrames);
      }
      reads++;
      Arrays.fill(frame, sample);
    }

    @Override
    public void close() {
      closed.countDown();
      closes.release();
    }
  }

  private static boolean await(CountDownLatch latch) {
    try {
      return latch.await(WAIT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private final List<byte[]> frames = new CopyOnWriteArrayList<>();
  private final List<MicProblem> problems = new CopyOnWriteArrayList<>();
  private final CountDownLatch reported = new CountDownLatch(1);

  private VoiceCapture start(CaptureSource source) {
    return VoiceCapture.start(
        source,
        frames::add,
        problem -> {
          problems.add(problem);
          reported.countDown();
        });
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(MicProblem.class)
  void aMicThatWillNotOpenSaysWhy(MicProblem problem) {
    FakeSource source = FakeSource.refusingToOpen(problem);
    start(source);
    assertTrue(await(reported));
    assertEquals(List.of(problem), problems);
    assertTrue(await(source.closed));
    assertTrue(frames.isEmpty());
  }

  @ParameterizedTest(name = "fails after {0} frames")
  @CsvSource({"0,NO_INPUT", "3,NO_INPUT", "3,BUSY"})
  void aFailedReadSaysWhyAndClosesTheMic(int good, MicProblem problem) {
    FakeSource source = FakeSource.failingAfter(good, problem);
    start(source);
    assertTrue(await(reported));
    assertEquals(List.of(problem), problems);
    assertTrue(await(source.closed));
    assertEquals(good, frames.size());
  }

  @ParameterizedTest(name = "sample {0}")
  @ValueSource(shorts = {0})
  void aDeadMicCountsAsRefusedAfterASecond(short sample) {
    FakeSource source = FakeSource.giving(sample);
    start(source);
    assertTrue(await(reported));
    assertEquals(List.of(MicProblem.REFUSED), problems);
    assertEquals(9, frames.size());
    assertTrue(await(source.closed));
  }

  @ParameterizedTest(name = "{0} stops")
  @ValueSource(ints = {1, 2})
  void stoppingClosesTheSourceSoABlockedReadEndsWithNoProblem(int stops) throws Exception {
    FakeSource source = FakeSource.blocking();
    VoiceCapture capture = start(source);
    assertTrue(await(source.blockedInRead));
    assertFalse(
        source.readReturned.await(50, TimeUnit.MILLISECONDS),
        "the read must block until the source is closed");
    for (int i = 0; i < stops; i++) {
      capture.stop();
    }
    assertTrue(source.closes.tryAcquire(stops + 1, WAIT_SECONDS, TimeUnit.SECONDS));
    assertTrue(problems.isEmpty());
    assertTrue(frames.isEmpty());
  }

  @ParameterizedTest(name = "sample {0}")
  @ValueSource(shorts = {100, -100})
  void aLiveMicSendsEncodedFramesUntilStopped(short sample) throws Exception {
    FakeSource source = FakeSource.giving(sample);
    VoiceCapture capture = start(source);
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
    while (frames.isEmpty() && System.nanoTime() < deadline) {
      Thread.onSpinWait();
    }
    capture.stop();
    assertTrue(await(source.closed));
    assertFalse(frames.isEmpty());
    assertTrue(problems.isEmpty());
  }
}
