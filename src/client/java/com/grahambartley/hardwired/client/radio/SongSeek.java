package com.grahambartley.hardwired.client.radio;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.BufferedAudioStream;

public final class SongSeek {

  private static final long MILLIS_PER_SECOND = 1000L;

  private SongSeek() {}

  public static AudioStream seek(
      AudioStream stream, LongSupplier targetMillis, BooleanSupplier cancelled) {
    try {
      if (stream instanceof BufferedAudioStream buffered) {
        skipTo(buffered, targetMillis, cancelled);
      }
      if (cancelled.getAsBoolean()) {
        stream.close();
        throw new CancellationException();
      }
    } catch (IOException failure) {
      closeAfter(stream, failure);
      throw new UncheckedIOException(failure);
    }
    return stream;
  }

  private static void closeAfter(AudioStream stream, IOException failure) {
    try {
      stream.close();
    } catch (IOException second) {
      failure.addSuppressed(second);
    }
  }

  public static long skipTo(
      BufferedAudioStream stream, LongSupplier targetMillis, BooleanSupplier cancelled)
      throws IOException {
    AudioFormat format = stream.getFormat();
    SampleCounter counter = new SampleCounter();
    boolean more = true;
    while (more
        && !cancelled.getAsBoolean()
        && millisOf(counter.samples, format) < targetMillis.getAsLong()) {
      more = stream.read(counter);
    }
    return counter.samples;
  }

  public static long millisOf(long samples, AudioFormat format) {
    return (long)
        (samples * MILLIS_PER_SECOND / (double) (format.getChannels() * format.getSampleRate()));
  }

  private static final class SampleCounter implements FloatConsumer {

    private long samples;

    @Override
    public void accept(float sample) {
      samples++;
    }
  }
}
