package com.grahambartley.hardwired.client.radio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.BufferedAudioStream;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

public final class BroadcastSoundInstance extends DeviceSoundInstance {

  private final LongSupplier songMillis;

  public BroadcastSoundInstance(
      SoundEvent song,
      BlockPos pos,
      int hearing,
      long seed,
      LongSupplier songMillis,
      DoubleSupplier loudness) {
    super(song, pos, hearing, seed, true, loudness);
    this.songMillis = songMillis;
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
