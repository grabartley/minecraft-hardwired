package com.grahambartley.hardwired.device;

public final class RadioRun {

  private RadioRun() {}

  public static DeviceStatus demand(boolean hasDisc, boolean songEnded, int speakers) {
    if (!hasDisc) {
      return DeviceStatus.NO_DISC;
    }
    if (songEnded) {
      return DeviceStatus.SONG_ENDED;
    }
    return speakers > 0 ? DeviceStatus.RUNNING : DeviceStatus.NO_SPEAKER;
  }

  public static boolean songEnded(int playedTicks, int songTicks) {
    return playedTicks >= songTicks;
  }

  public static int playedAfter(int playedTicks, DeviceStatus status, int beatTicks) {
    return switch (status) {
      case RUNNING -> playedTicks + beatTicks;
      case SONG_ENDED -> playedTicks;
      default -> 0;
    };
  }
}
