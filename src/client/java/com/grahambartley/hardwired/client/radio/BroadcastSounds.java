package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.block.SoundingBlockEntity;
import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.network.VoiceFramePayload;
import com.grahambartley.hardwired.radio.ReceptionMix;
import com.grahambartley.hardwired.radio.SongClock;
import com.grahambartley.hardwired.radio.StreamPicker;
import com.grahambartley.hardwired.radio.StreamShare;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class BroadcastSounds {

  private static final int RETRY_TICKS = 20;
  private static final long NANOS_PER_MILLI = 1_000_000L;

  private static final Set<SoundingBlockEntity> DEVICES = new HashSet<>();
  private static final Map<SoundingBlockEntity, Playing> PLAYING = new HashMap<>();

  private record Playing(
      BroadcastSoundInstance sound, Identifier song, long songStart, long retryAt) {

    boolean plays(SoundingBlockEntity device) {
      return song.equals(device.song()) && songStart == device.songStart();
    }
  }

  private BroadcastSounds() {}

  public static void register() {
    ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register(
        (entity, world) -> {
          if (entity instanceof SoundingBlockEntity device) {
            DEVICES.add(device);
          }
        });
    ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(
        (entity, world) -> {
          if (entity instanceof SoundingBlockEntity device) {
            DEVICES.remove(device);
          }
        });
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> forgetAll(client));
    ClientPlayNetworking.registerGlobalReceiver(
        VoiceFramePayload.ID,
        (payload, context) -> {
          if (ClientSettingsStore.radio().hearVoiceBroadcasts()) {
            VoiceFeed.receive(payload.desk(), payload.adpcm());
          }
        });
    ClientTickEvents.END_CLIENT_TICK.register(BroadcastSounds::tick);
  }

  private static void forgetAll(MinecraftClient client) {
    PLAYING.values().forEach(playing -> silence(client, playing));
    PLAYING.clear();
    DEVICES.clear();
    VoiceSounds.forgetAll(client);
    StaticSounds.forgetAll(client);
    VoiceFeed.clear();
  }

  private static void silence(MinecraftClient client, Playing playing) {
    playing.sound().silence();
    client.getSoundManager().stop(playing.sound());
  }

  private static void tick(MinecraftClient client) {
    if (client.world == null || client.isPaused()) {
      return;
    }
    Vec3d listener = client.gameRenderer.getCamera().getPos();
    boolean audible = client.options.getSoundVolume(SoundCategory.RECORDS) > 0;
    StreamShare.Granted<SoundingBlockEntity> granted =
        StreamShare.share(
            wantsOf(heardFrom(client, listener)),
            StreamPicker.MOST_STREAMS - CallVoiceSounds.playing());
    Iterator<Map.Entry<SoundingBlockEntity, Playing>> playing = PLAYING.entrySet().iterator();
    while (playing.hasNext()) {
      Map.Entry<SoundingBlockEntity, Playing> entry = playing.next();
      if (!granted.songs().contains(entry.getKey()) || !entry.getValue().plays(entry.getKey())) {
        silence(client, entry.getValue());
        playing.remove();
      }
    }
    VoiceSounds.update(client, audible ? granted.voices() : Set.of());
    if (audible) {
      granted.songs().forEach(device -> keepPlaying(client, device));
    }
    StaticSounds.tick(client, DEVICES, listener, audible);
  }

  private static List<StreamShare.Wants<SoundingBlockEntity>> wantsOf(
      List<SoundingBlockEntity> nearestFirst) {
    return nearestFirst.stream()
        .map(
            device ->
                new StreamShare.Wants<>(
                    device, VoiceFeed.onAir(device.station()), device.sounding()))
        .toList();
  }

  private static List<SoundingBlockEntity> heardFrom(MinecraftClient client, Vec3d listener) {
    RadioSettings settings = SyncedServerConfig.get().radio();
    List<StreamPicker.Candidate<SoundingBlockEntity>> candidates = new ArrayList<>();
    for (SoundingBlockEntity device : DEVICES) {
      if (device.getWorld() == client.world && device.status() == DeviceStatus.RUNNING) {
        candidates.add(
            new StreamPicker.Candidate<>(
                device,
                Vec3d.ofCenter(device.getPos()).distanceTo(listener),
                device.hearing(settings)));
      }
    }
    return StreamPicker.nearest(candidates, candidates.size());
  }

  private static void keepPlaying(MinecraftClient client, SoundingBlockEntity device) {
    long now = client.world.getTime();
    Playing current = PLAYING.get(device);
    if (current != null
        && (client.getSoundManager().isPlaying(current.sound()) || now < current.retryAt())) {
      return;
    }
    Optional<JukeboxSong> song = songOf(client.world, device.song());
    if (song.isEmpty() || SongClock.ended(now, device.songStart(), song.get().getLengthInTicks())) {
      return;
    }
    if (current != null) {
      client.getSoundManager().stop(current.sound());
    }
    BroadcastSoundInstance sound = soundFor(client, device, song.get(), now);
    PLAYING.put(device, new Playing(sound, device.song(), device.songStart(), now + RETRY_TICKS));
    client.getSoundManager().play(sound);
  }

  private static BroadcastSoundInstance soundFor(
      MinecraftClient client, SoundingBlockEntity device, JukeboxSong song, long now) {
    long startMillis = SongClock.millisIn(now, device.songStart());
    long startNanos = System.nanoTime();
    BlockPos pos = device.getPos();
    return new BroadcastSoundInstance(
        song.soundEvent().value(),
        pos,
        device.hearing(SyncedServerConfig.get().radio()),
        device.songStart(),
        () -> startMillis + (System.nanoTime() - startNanos) / NANOS_PER_MILLI,
        () ->
            ReceptionMix.dippedForVoice(
                ReceptionMix.stationLoudness(device.volume(), device.strength()),
                VoiceFeed.onAir(device.station())));
  }

  private static Optional<JukeboxSong> songOf(ClientWorld world, Identifier id) {
    return world
        .getRegistryManager()
        .getOptional(RegistryKeys.JUKEBOX_SONG)
        .flatMap(
            songs ->
                songs.getOrEmpty(
                    RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Objects.requireNonNull(id))));
  }
}
