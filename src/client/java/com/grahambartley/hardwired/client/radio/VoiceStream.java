package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.radio.VoiceFormat;
import com.grahambartley.hardwired.radio.VoiceJitter;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sound.AudioStream;
import org.lwjgl.BufferUtils;

final class VoiceStream implements AudioStream {

  private static final AudioFormat FORMAT =
      new AudioFormat(
          VoiceFormat.SAMPLE_RATE, VoiceFormat.BYTES_PER_SAMPLE * Byte.SIZE, 1, true, false);
  private static final int CHUNK_BYTES = VoiceFormat.millisToBytes(250);

  private final VoiceJitter jitter;

  VoiceStream(VoiceJitter jitter) {
    this.jitter = jitter;
  }

  @Override
  public AudioFormat getFormat() {
    return FORMAT;
  }

  @Override
  public ByteBuffer read(int size) {
    byte[] pcm = jitter.take(Math.min(size, CHUNK_BYTES));
    ByteBuffer buffer = BufferUtils.createByteBuffer(pcm.length);
    buffer.put(pcm).flip();
    return buffer;
  }

  @Override
  public void close() {}
}
