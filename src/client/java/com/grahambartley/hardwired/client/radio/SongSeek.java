package com.grahambartley.hardwired.client.radio;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sound.BufferedAudioStream;

public final class SongSeek {

  private static final long MILLIS_PER_SECOND = 1000L;

  private SongSeek() {}

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
