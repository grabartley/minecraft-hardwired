package com.grahambartley.hardwired.client.radio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.sound.sampled.TargetDataLine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MicrophoneSourceTest {

  private static TargetDataLine lineGiving(int bytesPerRead) {
    TargetDataLine line = mock(TargetDataLine.class);
    when(line.read(any(byte[].class), anyInt(), anyInt()))
        .thenAnswer(
            call -> {
              byte[] bytes = call.getArgument(0);
              int offset = call.getArgument(1);
              int length = Math.min(bytesPerRead, call.getArgument(2));
              for (int i = offset; i < offset + length; i++) {
                bytes[i] = 1;
              }
              return length;
            });
    return line;
  }

  @ParameterizedTest(name = "{0} bytes a read")
  @ValueSource(ints = {2, 3, 6, 100})
  void aFrameIsFilledHoweverTheLineSplitsIt(int bytesPerRead) throws Exception {
    short[] frame = new short[3];
    MicrophoneSource.fill(lineGiving(bytesPerRead), frame);
    assertArrayEquals(new short[] {257, 257, 257}, frame);
  }

  @ParameterizedTest(name = "read returns {0} after {1} good reads")
  @CsvSource({"0,0", "0,1", "-1,0"})
  void aLineThatStopsGivingIsAMissingMic(int result, int goodReads) {
    TargetDataLine line = lineGiving(2);
    var stub = when(line.read(any(byte[].class), anyInt(), anyInt()));
    for (int i = 0; i < goodReads; i++) {
      stub = stub.thenReturn(2);
    }
    stub.thenReturn(result);
    MicFailure failure =
        assertThrows(MicFailure.class, () -> MicrophoneSource.fill(line, new short[3]));
    assertEquals(MicProblem.NO_INPUT, failure.problem());
  }

  @ParameterizedTest(name = "{0} closes")
  @ValueSource(ints = {1, 2})
  void closingAnUnopenedSourceIsHarmless(int closes) {
    MicrophoneSource source = new MicrophoneSource("none");
    for (int i = 0; i < closes; i++) {
      source.close();
    }
  }
}
