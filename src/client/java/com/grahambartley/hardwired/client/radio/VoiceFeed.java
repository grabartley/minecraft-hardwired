package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.network.VoiceFramePayload;
import com.grahambartley.hardwired.radio.AdpcmCodec;
import com.grahambartley.hardwired.radio.VoiceFormat;
import com.grahambartley.hardwired.radio.VoiceJitter;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

final class VoiceFeed {

  private static final Map<BlockPos, VoiceChannel> CHANNELS = new HashMap<>();

  private VoiceFeed() {}

  static void receive(VoiceFramePayload frame) {
    if (!ClientSettingsStore.radio().hearVoiceBroadcasts()) {
      return;
    }
    byte[] pcm = VoiceFormat.toBytes(AdpcmCodec.decode(frame.adpcm()));
    CHANNELS
        .computeIfAbsent(frame.desk(), desk -> new VoiceChannel())
        .receive(pcm, Util.getMeasuringTimeMs());
  }

  static boolean onAir(@Nullable BlockPos desk) {
    VoiceChannel channel = desk == null ? null : CHANNELS.get(desk);
    return channel != null && channel.onAir(Util.getMeasuringTimeMs());
  }

  static VoiceJitter attach(BlockPos desk) {
    return CHANNELS.computeIfAbsent(desk, station -> new VoiceChannel()).attach();
  }

  static void detach(BlockPos desk, VoiceJitter tap) {
    VoiceChannel channel = CHANNELS.get(desk);
    if (channel != null) {
      channel.detach(tap);
    }
  }

  static void clear() {
    CHANNELS.clear();
  }
}
