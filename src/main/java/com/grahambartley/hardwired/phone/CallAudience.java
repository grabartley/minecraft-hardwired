package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.radio.VoiceFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CallAudience {

  private CallAudience() {}

  public static List<UUID> of(
      Map<Long, UUID> members,
      boolean connected,
      long phone,
      UUID speaker,
      boolean voiceCalls,
      int frameBytes) {
    if (!voiceCalls
        || !connected
        || !speaker.equals(members.get(phone))
        || !VoiceFormat.frameFits(frameBytes)) {
      return List.of();
    }
    return members.values().stream().filter(id -> !id.equals(speaker)).toList();
  }
}
