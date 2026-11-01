package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.device.DeviceStatus;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class DeskMic {

  public static final int MOST_FRAME_BYTES = AdpcmCodec.frameBytes(VoiceFormat.FRAME_SAMPLES);
  public static final long SILENT_MILLIS = 3000;

  @Nullable private UUID speaker;
  private boolean taken;
  private final VoiceRateLimit limit = new VoiceRateLimit(0);
  private long lastHeard;

  public static boolean mayBeLive(DeviceStatus status, boolean voiceBroadcasts) {
    return status == DeviceStatus.RUNNING && voiceBroadcasts;
  }

  @Nullable
  public UUID speaker() {
    return speaker;
  }

  public void show(@Nullable UUID shown) {
    speaker = shown;
  }

  public boolean take(UUID player, long nowMillis) {
    if (speaker != null && !speaker.equals(player)) {
      return false;
    }
    if (speaker == null) {
      lastHeard = nowMillis;
    }
    speaker = player;
    taken = true;
    return true;
  }

  public boolean release(UUID player) {
    if (!player.equals(speaker)) {
      return false;
    }
    clear();
    return true;
  }

  public void clear() {
    speaker = null;
    taken = false;
  }

  public boolean accepts(UUID player, int frameBytes, long nowMillis) {
    if (!taken
        || !player.equals(speaker)
        || frameBytes <= AdpcmCodec.HEADER_BYTES
        || frameBytes > MOST_FRAME_BYTES
        || !limit.allows(nowMillis)) {
      return false;
    }
    lastHeard = nowMillis;
    return true;
  }

  public boolean gaveUp(long nowMillis) {
    return speaker != null && nowMillis - lastHeard > SILENT_MILLIS;
  }
}
