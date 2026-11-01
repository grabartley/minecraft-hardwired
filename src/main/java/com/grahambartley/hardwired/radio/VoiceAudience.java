package com.grahambartley.hardwired.radio;

import java.util.Collection;

public final class VoiceAudience {

  public record Device(double x, double y, double z, double hearing) {

    boolean heardFrom(double fromX, double fromY, double fromZ) {
      return Math.hypot(Math.hypot(x - fromX, y - fromY), z - fromZ) <= hearing;
    }
  }

  private VoiceAudience() {}

  public static boolean hears(double x, double y, double z, Collection<Device> devices) {
    return devices.stream().anyMatch(device -> device.heardFrom(x, y, z));
  }
}
