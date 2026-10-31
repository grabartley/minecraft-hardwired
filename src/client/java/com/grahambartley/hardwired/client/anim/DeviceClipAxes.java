package com.grahambartley.hardwired.client.anim;

final class DeviceClipAxes {

  private DeviceClipAxes() {}

  static float[] rotation(float[] bedrock) {
    return new float[] {-bedrock[0], -bedrock[1], bedrock[2]};
  }

  static float[] position(float[] bedrock) {
    return new float[] {-bedrock[0], bedrock[1], bedrock[2]};
  }
}
