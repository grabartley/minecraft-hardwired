package com.grahambartley.hardwired.device;

public interface DialDevice {

  float dial(String value, float tickDelta);

  boolean isMoving();

  float clipSpeed();
}
