package com.grahambartley.hardwired.client.glow;

@FunctionalInterface
interface GlowVertexSink {

  void vertex(float x, float y, float z, float red, float green, float blue, float strength);
}
