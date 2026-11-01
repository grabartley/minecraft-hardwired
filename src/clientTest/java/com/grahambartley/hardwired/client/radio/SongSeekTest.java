package com.grahambartley.hardwired.client.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sound.BufferedAudioStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

  @ParameterizedTest(name = "a moving target of {0} per check")
  @CsvSource({"10,100", "50,100"})
  void aTargetThatMovesIsChasedUntilCaught(int perCheck, long samples) throws Exception {
    FakeStream stream = new FakeStream(1, 4000);
    long[] now = {0};
    long skipped = SongSeek.skipTo(stream, () -> now[0] += perCheck, () -> false);
    assertEquals(samples, skipped);
  }
}
