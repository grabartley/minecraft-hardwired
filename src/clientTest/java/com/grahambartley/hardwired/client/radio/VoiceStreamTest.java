package com.grahambartley.hardwired.client.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.radio.VoiceFormat;
import com.grahambartley.hardwired.radio.VoiceJitter;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VoiceStreamTest {

  private static final int ONE_SECOND = VoiceFormat.millisToBytes(1000);

  @ParameterizedTest(name = "asked for {0} gets {1}")
  @CsvSource({"32000,8000", "8000,8000", "100,100"})
  void aReadNeverReturnsMoreThanAChunkSoTheQueueStaysShort(int size, int expected) {
    assertEquals(expected, new VoiceStream(new VoiceJitter(1, 16)).read(size).remaining());
  }

  @ParameterizedTest(name = "read {0}")
  @ValueSource(ints = {1, 50})
  void anEmptyStreamNeverEndsItReturnsSilence(int reads) {
    VoiceStream stream = new VoiceStream(new VoiceJitter(1, 16));
    for (int i = 0; i < reads; i++) {
      ByteBuffer buffer = stream.read(ONE_SECOND);
      assertEquals(8000, buffer.remaining());
      assertEquals(0, buffer.get(0));
    }
  }

  @ParameterizedTest(name = "rate {0}")
  @ValueSource(floats = {16000f})
  void theFormatIsSixteenKilohertzMono(float rate) {
    AudioFormat format = new VoiceStream(new VoiceJitter(1, 16)).getFormat();
    assertEquals(rate, format.getSampleRate());
    assertEquals(1, format.getChannels());
    assertEquals(16, format.getSampleSizeInBits());
  }
}
