package com.grahambartley.hardwired.client.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.radio.RadioTone;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sound.AudioStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RadioToneStreamTest {

  private static final class Feed implements AudioStream {

    private final AudioFormat format;
    private final ByteBuffer[] chunks;
    private int next;
    private boolean closed;

    Feed(AudioFormat format, ByteBuffer... chunks) {
      this.format = format;
      this.chunks = chunks;
    }

    @Override
    public AudioFormat getFormat() {
      return format;
    }

    @Override
    public ByteBuffer read(int size) {
      return next < chunks.length ? chunks[next++] : null;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  private static AudioFormat format(int channels, boolean bigEndian) {
    return new AudioFormat(44100, Short.SIZE, channels, true, bigEndian);
  }

  private static ByteBuffer pcm(boolean bigEndian, short... samples) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(samples.length * Short.BYTES);
    buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    for (short sample : samples) {
      buffer.putShort(sample);
    }
    return buffer.flip();
  }

  private static short[] wave(int length) {
    short[] samples = new short[length];
    for (int i = 0; i < length; i++) {
      samples[i] = (short) (20000 * Math.sin(i * 0.2));
    }
    return samples;
  }

  private static short[] read(ByteBuffer buffer, boolean bigEndian) {
    ByteBuffer view =
        buffer.duplicate().order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    short[] samples = new short[view.remaining() / Short.BYTES];
    view.asShortBuffer().get(samples);
    return samples;
  }

  @ParameterizedTest(name = "split at {0}, big endian {1}")
  @CsvSource({"1,false", "64,false", "199,false", "64,true"})
  void splittingAStreamIntoChunksChangesNothing(int split, boolean bigEndian) throws IOException {
    short[] whole = wave(200);
    short[] head = Arrays.copyOfRange(whole, 0, split);
    short[] tail = Arrays.copyOfRange(whole, split, whole.length);
    RadioToneStream stream =
        new RadioToneStream(
            new Feed(format(1, bigEndian), pcm(bigEndian, head), pcm(bigEndian, tail)),
            RadioTone.Band.RADIO);
    short[] first = read(stream.read(0), bigEndian);
    short[] second = read(stream.read(0), bigEndian);
    RadioTone tone = new RadioTone(44100, 1, RadioTone.Band.RADIO);
    for (int i = 0; i < whole.length; i++) {
      short got = i < split ? first[i] : second[i - split];
      short expected =
          (short) Math.round(tone.shape(0, whole[i] / (double) Short.MAX_VALUE) * Short.MAX_VALUE);
      assertEquals(expected, got);
    }
  }

  @Test
  void stereoChannelsStayApartAcrossAnOddChunk() throws IOException {
    short[] left = wave(101);
    short[] mixed = new short[202];
    for (int i = 0; i < left.length; i++) {
      mixed[2 * i] = left[i];
    }
    RadioToneStream stream =
        new RadioToneStream(
            new Feed(
                format(2, false),
                pcm(false, Arrays.copyOfRange(mixed, 0, 101)),
                pcm(false, Arrays.copyOfRange(mixed, 101, 202))),
            RadioTone.Band.RADIO);
    short[] first = read(stream.read(0), false);
    short[] second = read(stream.read(0), false);
    assertEquals(0, first[1]);
    assertEquals(0, second[0]);
    assertTrue(first[100] != 0);
  }

  @Test
  void theEndOfAStreamPassesThroughAndCloses() throws IOException {
    Feed feed = new Feed(format(1, false));
    RadioToneStream stream = new RadioToneStream(feed, RadioTone.Band.RADIO);
    assertNull(stream.read(16));
    stream.close();
    assertTrue(feed.closed);
    assertEquals(feed.getFormat(), stream.getFormat());
  }
}
