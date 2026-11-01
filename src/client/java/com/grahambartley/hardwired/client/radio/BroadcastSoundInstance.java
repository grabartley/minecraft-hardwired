package com.grahambartley.hardwired.client.radio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.BufferedAudioStream;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public final class BroadcastSoundInstance extends MovingSoundInstance {

  private final LongSupplier songMillis;
  private final DoubleSupplier loudness;
  private final int hearing;

  public BroadcastSoundInstance(
      SoundEvent song,
      BlockPos pos,
      int hearing,
      long seed,
      LongSupplier songMillis,
      DoubleSupplier loudness) {
    super(song, SoundCategory.RECORDS, Random.create(seed));
    this.songMillis = songMillis;
    this.loudness = loudness;
    this.hearing = hearing;
    this.x = pos.getX() + 0.5;
    this.y = pos.getY() + 0.5;
    this.z = pos.getZ() + 0.5;
    this.volume = (float) loudness.getAsDouble();
  }

  @Override
  public void tick() {
    volume = (float) loudness.getAsDouble();
  }

  public void silence() {
    setDone();
  }

  @Override
  public WeightedSoundSet getSoundSet(SoundManager manager) {
    WeightedSoundSet set = super.getSoundSet(manager);
    if (sound != null
        && sound != SoundManager.MISSING_SOUND
        && sound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
      sound =
          new Sound(
              sound.getIdentifier(),
              sound.getVolume(),
              sound.getPitch(),
              sound.getWeight(),
              Sound.RegistrationType.FILE,
              true,
              false,
              hearing);
    }
    return set;
  }

  @Override
  public boolean shouldAlwaysPlay() {
    return true;
  }

  @Override
  public CompletableFuture<AudioStream> getAudioStream(
      SoundLoader loader, Identifier id, boolean repeatInstantly) {
    return loader
        .loadStreamed(id, false)
        .thenApplyAsync(this::seek, Util.getDownloadWorkerExecutor());
  }

  private AudioStream seek(AudioStream stream) {
    try {
      if (stream instanceof BufferedAudioStream buffered) {
        SongSeek.skipTo(buffered, songMillis, this::isDone);
      }
      if (isDone()) {
        stream.close();
        throw new CancellationException();
      }
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
    return stream;
  }
}
