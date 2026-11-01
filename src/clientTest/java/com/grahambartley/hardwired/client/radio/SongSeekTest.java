package com.grahambartley.hardwired.client.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CancellationException;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sound.BufferedAudioStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SongSeekTest {

  private static final int RATE = 1000;
  private static final int PACKET_SAMPLES = 100;

  private static final class FakeStream implements BufferedAudioStream {

    private final AudioFormat format;
    private long samplesLeft;
    private int packets;

    FakeStream(int channels, long totalSamples) {
      format = new AudioFormat(RATE, 16, channels, true, false);
      samplesLeft = totalSamples;
    }

    @Override
    public AudioFormat getFormat() {
      return format;
    }

    @Override
    public boolean read(FloatConsumer consumer) {
      if (samplesLeft <= 0) {
        return false;
      }
      packets++;
      for (int i = 0; i < PACKET_SAMPLES; i++) {
        consumer.accept(0F);
      }
      samplesLeft -= PACKET_SAMPLES;
      return true;
    }

    @Override
    public void close() {}
  }

  private static final class ClosableStream implements BufferedAudioStream {

    private final boolean failsToRead;
    private int closes;

    ClosableStream(boolean failsToRead) {
      this.failsToRead = failsToRead;
    }

    @Override
    public AudioFormat getFormat() {
      return new AudioFormat(RATE, 16, 1, true, false);
    }

    @Override
    public boolean read(FloatConsumer consumer) throws IOException {
      if (failsToRead) {
        throw new IOException("bad stream");
      }
      return false;
    }

    @Override
    public void close() {
      closes++;
    }
  }

  @ParameterizedTest(name = "{0} channels to {1} ms")
  @CsvSource({"1,0,0", "1,500,500", "1,510,600", "2,500,1000", "1,99999,2000"})
  void skippingStopsAtTheFirstPacketPastTheTarget(int channels, long target, long samples)
      throws Exception {
    FakeStream stream = new FakeStream(channels, 2000);
    assertEquals(samples, SongSeek.skipTo(stream, () -> target, () -> false));
  }

  @ParameterizedTest(name = "cancelled after {0} checks")
  @CsvSource({"1,0", "3,2"})
  void aStoppedSoundStopsTheSkip(int checks, int packets) throws Exception {
    FakeStream stream = new FakeStream(1, 2000);
    int[] asked = new int[1];
    SongSeek.skipTo(stream, () -> 1000, () -> ++asked[0] >= checks);
    assertEquals(packets, stream.packets);
  }

  @ParameterizedTest(name = "cancelled {0}")
  @ValueSource(booleans = {true, false})
  void aSeekClosesTheStreamOnlyWhenItWillNotBeUsed(boolean cancelled) {
    ClosableStream stream = new ClosableStream(false);
    if (cancelled) {
      assertThrows(CancellationException.class, () -> SongSeek.seek(stream, () -> 10, () -> true));
    } else {
      assertSame(stream, SongSeek.seek(stream, () -> 10, () -> false));
    }
    assertEquals(cancelled ? 1 : 0, stream.closes);
  }

  @ParameterizedTest(name = "target {0}")
  @ValueSource(longs = {1, 1000})
  void aFailedSeekClosesTheStream(long target) {
    ClosableStream stream = new ClosableStream(true);
    assertThrows(
        UncheckedIOException.class, () -> SongSeek.seek(stream, () -> target, () -> false));
    assertEquals(1, stream.closes);
  }

  @ParameterizedTest(name = "a moving target of {0} per check")
  @CsvSource({"10,100", "50,100"})
  void aTargetThatMovesIsChasedUntilCaught(int perCheck, long samples) throws Exception {
    FakeStream stream = new FakeStream(1, 4000);
    long[] now = {0};
    long skipped = SongSeek.skipTo(stream, () -> now[0] += perCheck, () -> false);
    assertEquals(samples, skipped);
  }
}
