package com.grahambartley.hardwired.client.anim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceClipTest {

  private static final String CLIP =
      """
      {"format_version":"1.8.0","animations":{"loop":{"loop":true,"animation_length":2.0,
      "bones":{
       "wheel":{"rotation":{"0.0":[0,0,0],"2.0":[0,0,360]}},
       "pin":{"position":[1,2,3]},
       "split":{"rotation":{"0.0":{"post":[0,0,10]},"1.0":{"pre":[0,0,20],"post":[0,0,40]},
         "2.0":[0,0,40]}},
       "molang":{"rotation":["math.sin(query.anim_time)",0,5]}
      }}}}
      """;

  @ParameterizedTest(name = "wheel at {0}s is {1}")
  @CsvSource({"0,0", "0.5,90", "1,180", "1.5,270", "2.5,90", "4.5,90"})
  void rotationLoopsLinearly(float seconds, float degrees) {
    assertEquals(degrees, DeviceClip.parse(CLIP).sample("wheel", seconds).rotation()[2], 0.001F);
  }

  @ParameterizedTest(name = "split at {0}s is {1}")
  @CsvSource({"0.5,15", "1.5,40"})
  void preAndPostValuesSplitAKeyframe(float seconds, float degrees) {
    assertEquals(degrees, DeviceClip.parse(CLIP).sample("split", seconds).rotation()[2], 0.001F);
  }

  @ParameterizedTest(name = "unknown bone at {0}s rests")
  @CsvSource({"0", "1.5"})
  void unknownBoneRests(float seconds) {
    DeviceClip.Pose pose = DeviceClip.parse(CLIP).sample("nope", seconds);
    assertArrayEquals(new float[3], pose.rotation());
    assertArrayEquals(new float[3], pose.position());
  }

  @ParameterizedTest(name = "constant channel at {0}s")
  @CsvSource({"0", "1"})
  void aChannelWithNoTimesIsConstantAndFlipsX(float seconds) {
    assertArrayEquals(
        new float[] {-1, 2, 3}, DeviceClip.parse(CLIP).sample("pin", seconds).position());
  }

  @ParameterizedTest(name = "molang text reads as zero, z {0}")
  @CsvSource({"5"})
  void molangReadsAsZero(float z) {
    assertArrayEquals(
        new float[] {-0F, -0F, z}, DeviceClip.parse(CLIP).sample("molang", 0).rotation());
  }

  @ParameterizedTest(name = "length {1} from {0}")
  @CsvSource({
    "'{\"animations\":{\"loop\":{\"animation_length\":1.5}}}',1.5",
    "'{\"animations\":{\"x\":{\"bones\":{\"a\":{\"rotation\":{\"3.0\":[0,0,0]}}}}}}',3",
    "'{\"animations\":{\"loop\":{}}}',0.05"
  })
  void lengthFallsBackToTheLastKey(String json, float length) {
    assertEquals(length, DeviceClip.parse(json).length(), 0.001F);
  }
}
