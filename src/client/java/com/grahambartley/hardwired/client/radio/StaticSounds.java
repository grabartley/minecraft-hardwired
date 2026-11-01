package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.block.SoundingBlockEntity;
import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.radio.CrackleClock;
import com.grahambartley.hardwired.radio.Frequency;
import com.grahambartley.hardwired.radio.ReceptionMix;
import com.grahambartley.hardwired.radio.StreamPicker;
import com.grahambartley.hardwired.registry.ModSounds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class StaticSounds {

  private static final int MOST_NOISES = 12;
  private static final double FAINTEST_HISS = 0.01;

  private static final Map<SoundingBlockEntity, NoiseSoundInstance> HISSES = new HashMap<>();
  private static final Map<BlockPos, Long> NEXT_CRACKLE = new HashMap<>();
  private static final Map<SoundingBlockEntity, Integer> DIALS = new HashMap<>();
  private static final Map<SoundingBlockEntity, Long> LAST_SWEEP = new HashMap<>();

  private StaticSounds() {}

  static void tick(
      MinecraftClient client,
      Collection<SoundingBlockEntity> devices,
      Vec3d listener,
      boolean audible) {
    List<SoundingBlockEntity> heard = audible ? noisyNear(devices, listener) : List.of();
    Set<SoundingBlockEntity> hissing = new HashSet<>();
    for (SoundingBlockEntity device : heard) {
      if (hissLoudness(device) > FAINTEST_HISS) {
        hissing.add(device);
      }
    }
    Iterator<Map.Entry<SoundingBlockEntity, NoiseSoundInstance>> hisses =
        HISSES.entrySet().iterator();
    while (hisses.hasNext()) {
      Map.Entry<SoundingBlockEntity, NoiseSoundInstance> entry = hisses.next();
      if (!hissing.contains(entry.getKey())) {
        entry.getValue().silence();
        client.getSoundManager().stop(entry.getValue());
        hisses.remove();
      }
    }
    hissing.forEach(device -> keepHissing(client, device));
    long now = client.world.getTime();
    Map<BlockPos, List<SoundingBlockEntity>> stations = byStation(heard);
    NEXT_CRACKLE.keySet().retainAll(stations.keySet());
    stations.forEach((station, tuned) -> crackleWhenDue(client, station, tuned, now));
    watchDials(client, devices, new HashSet<>(heard), now);
  }

  static void forgetAll(MinecraftClient client) {
    HISSES
        .values()
        .forEach(
            hiss -> {
              hiss.silence();
              client.getSoundManager().stop(hiss);
            });
    HISSES.clear();
    NEXT_CRACKLE.clear();
    DIALS.clear();
    LAST_SWEEP.clear();
  }

  private static List<SoundingBlockEntity> noisyNear(
      Collection<SoundingBlockEntity> devices, Vec3d listener) {
    RadioSettings settings = SyncedServerConfig.get().radio();
    List<StreamPicker.Candidate<SoundingBlockEntity>> candidates = new ArrayList<>();
    for (SoundingBlockEntity device : devices) {
      if (device.status() == DeviceStatus.RUNNING || device.hisses()) {
        candidates.add(
            new StreamPicker.Candidate<>(
                device,
                Vec3d.ofCenter(device.getPos()).distanceTo(listener),
                device.hearing(settings)));
      }
    }
    return StreamPicker.nearest(candidates, MOST_NOISES);
  }

  private static int noise() {
    return ClientSettingsStore.radio().radioNoisePercent();
  }

  private static double hissLoudness(SoundingBlockEntity device) {
    return device.status() == DeviceStatus.RUNNING
        ? ReceptionMix.stationHiss(device.volume(), noise(), device.strength())
        : ReceptionMix.emptyDialHiss(device.volume(), noise());
  }

  private static void keepHissing(MinecraftClient client, SoundingBlockEntity device) {
    NoiseSoundInstance current = HISSES.get(device);
    if (current != null && client.getSoundManager().isPlaying(current)) {
      return;
    }
    NoiseSoundInstance hiss =
        noise(client, device, ModSounds.RADIO_HISS, true, () -> hissLoudness(device));
    HISSES.put(device, hiss);
    client.getSoundManager().play(hiss);
  }

  private static Map<BlockPos, List<SoundingBlockEntity>> byStation(
      List<SoundingBlockEntity> heard) {
    Map<BlockPos, List<SoundingBlockEntity>> stations = new HashMap<>();
    for (SoundingBlockEntity device : heard) {
      if (device.status() == DeviceStatus.RUNNING && device.station() != null) {
        stations.computeIfAbsent(device.station(), station -> new ArrayList<>()).add(device);
      }
    }
    return stations;
  }

  private static void crackleWhenDue(
      MinecraftClient client, BlockPos station, List<SoundingBlockEntity> tuned, long now) {
    Long due = NEXT_CRACKLE.get(station);
    if (due != null && now >= due) {
      tuned.forEach(device -> crackle(client, device));
    }
    if (due == null || now >= due) {
      double strongest = tuned.stream().mapToDouble(SoundingBlockEntity::strength).max().orElse(0);
      NEXT_CRACKLE.put(
          station, now + CrackleClock.gapTicks(strongest, client.world.getRandom().nextDouble()));
    }
  }

  private static void crackle(MinecraftClient client, SoundingBlockEntity device) {
    double loudness = ReceptionMix.crackle(device.volume(), noise(), device.strength());
    client
        .getSoundManager()
        .play(noise(client, device, ModSounds.RADIO_CRACKLE, false, () -> loudness));
  }

  private static void watchDials(
      MinecraftClient client,
      Collection<SoundingBlockEntity> devices,
      Set<SoundingBlockEntity> heard,
      long now) {
    DIALS.keySet().retainAll(new HashSet<>(devices));
    LAST_SWEEP.keySet().retainAll(new HashSet<>(devices));
    for (SoundingBlockEntity device : devices) {
      Integer dial = DIALS.put(device, device.dial());
      if (dial != null
          && dial >= Frequency.MIN
          && dial != device.dial()
          && heard.contains(device)
          && now - LAST_SWEEP.getOrDefault(device, Long.MIN_VALUE / 2)
              >= CrackleClock.SWEEP_GAP_TICKS) {
        LAST_SWEEP.put(device, now);
        double loudness = ReceptionMix.sweep(device.volume(), noise());
        client
            .getSoundManager()
            .play(noise(client, device, ModSounds.RADIO_SWEEP, false, () -> loudness));
      }
    }
  }

  private static NoiseSoundInstance noise(
      MinecraftClient client,
      SoundingBlockEntity device,
      SoundEvent event,
      boolean looping,
      DoubleSupplier loudness) {
    return new NoiseSoundInstance(
        event,
        device.getPos(),
        device.hearing(SyncedServerConfig.get().radio()),
        client.world.getRandom().nextLong(),
        looping,
        loudness);
  }
}
