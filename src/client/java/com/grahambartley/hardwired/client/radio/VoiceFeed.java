package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.network.VoiceFramePayload;
import com.grahambartley.hardwired.radio.AdpcmCodec;
import com.grahambartley.hardwired.radio.VoiceFormat;
import com.grahambartley.hardwired.radio.VoiceJitter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

final class VoiceFeed {

  private static final long TAIL_MILLIS = 2000;
  private static final int RECENT_FRAMES = 3;
  private static final int PREFILL_BYTES = VoiceFormat.millisToBytes(350);
  private static final int MOST_BYTES = VoiceFormat.millisToBytes(1500);

  private static final Map<BlockPos, Channel> CHANNELS = new HashMap<>();

  private static final class Channel {

    private final List<VoiceJitter> taps = new ArrayList<>();
    private final Deque<byte[]> recent = new ArrayDeque<>();
    private long lastFrame = -TAIL_MILLIS;
  }

  private VoiceFeed() {}

  static void receive(VoiceFramePayload frame) {
    if (!ClientSettingsStore.radio().hearVoiceBroadcasts()) {
      return;
    }
    Channel channel = CHANNELS.computeIfAbsent(frame.desk(), desk -> new Channel());
    byte[] pcm = VoiceFormat.toBytes(AdpcmCodec.decode(frame.adpcm()));
    channel.lastFrame = Util.getMeasuringTimeMs();
    channel.recent.addLast(pcm);
    if (channel.recent.size() > RECENT_FRAMES) {
      channel.recent.removeFirst();
    }
    channel.taps.forEach(tap -> tap.offer(pcm));
  }

  static boolean onAir(@Nullable BlockPos desk) {
    Channel channel = desk == null ? null : CHANNELS.get(desk);
    return channel != null && Util.getMeasuringTimeMs() - channel.lastFrame < TAIL_MILLIS;
  }

  static VoiceJitter attach(BlockPos desk) {
    Channel channel = CHANNELS.computeIfAbsent(desk, station -> new Channel());
    VoiceJitter tap = new VoiceJitter(PREFILL_BYTES, MOST_BYTES);
    channel.recent.forEach(tap::offer);
    channel.taps.add(tap);
    return tap;
  }

  static void detach(BlockPos desk, VoiceJitter tap) {
    Channel channel = CHANNELS.get(desk);
    if (channel != null) {
      channel.taps.remove(tap);
    }
  }

  static void clear() {
    CHANNELS.clear();
  }
}
