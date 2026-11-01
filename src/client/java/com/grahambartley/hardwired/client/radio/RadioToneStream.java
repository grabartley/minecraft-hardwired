package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.radio.RadioTone;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sound.AudioStream;

final class RadioToneStream implements AudioStream {

  private static final int BYTES_PER_SAMPLE = Short.BYTES;
  private static final double FULL_SCALE = Short.MAX_VALUE;

  private final AudioStream stream;
  private final RadioTone tone;
  private final int channels;
  private int channel;

  RadioToneStream(AudioStream stream) {
    this.stream = stream;
    this.channels = stream.getFormat().getChannels();
    this.tone = new RadioTone(stream.getFormat().getSampleRate(), channels);
  }

  @Override
  public AudioFormat getFormat() {
    return stream.getFormat();
  }

  @Override
  public ByteBuffer read(int size) throws IOException {
    ByteBuffer pcm = stream.read(size);
    return pcm == null ? null : shaped(pcm);
  }

  private ByteBuffer shaped(ByteBuffer pcm) {
    ByteBuffer samples =
        pcm.duplicate()
            .order(getFormat().isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    int end = samples.limit() - BYTES_PER_SAMPLE;
    for (int at = samples.position(); at <= end; at += BYTES_PER_SAMPLE) {
      double shaped = tone.shape(channel, samples.getShort(at) / FULL_SCALE);
      samples.putShort(at, (short) Math.round(shaped * FULL_SCALE));
      channel = (channel + 1) % channels;
    }
    return pcm;
  }

  @Override
  public void close() throws IOException {
    stream.close();
  }
}
