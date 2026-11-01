package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VoiceFormatTest {

  @ParameterizedTest(name = "{0} and {1}")
  @CsvSource({"0,1", "-1,1", "32767,-32768", "255,-256"})
  void samplesSurviveLittleEndianBytes(short first, short second) {
    short[] samples = {first, second};
    byte[] bytes = VoiceFormat.toBytes(samples);
    assertEquals(samples.length * VoiceFormat.BYTES_PER_SAMPLE, bytes.length);
    assertArrayEquals(samples, VoiceFormat.toSamples(bytes, bytes.length));
  }

  @ParameterizedTest(name = "{0} ms is {1} bytes")
  @CsvSource({"100,3200", "250,8000", "1000,32000"})
  void millisBecomeBytes(int millis, int bytes) {
    assertEquals(bytes, VoiceFormat.millisToBytes(millis));
  }
}
