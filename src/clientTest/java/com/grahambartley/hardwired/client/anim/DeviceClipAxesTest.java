package com.grahambartley.hardwired.client.anim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceClipAxesTest {

  @ParameterizedTest(name = "{0},{1},{2}")
  @CsvSource({"1,2,3", "-4,0,9"})
  void bedrockAxesAreFlippedToBlockSpace(float x, float y, float z) {
    assertArrayEquals(new float[] {-x, -y, z}, DeviceClipAxes.rotation(new float[] {x, y, z}), 0F);
    assertArrayEquals(new float[] {-x, y, z}, DeviceClipAxes.position(new float[] {x, y, z}), 0F);
  }
}
