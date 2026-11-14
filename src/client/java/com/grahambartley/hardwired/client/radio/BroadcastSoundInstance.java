package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.radio.RadioTone;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import net.minecraft.client.sound.AudioStream;
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
        .thenApplyAsync(
            stream -> SongSeek.seek(stream, songMillis, this::isDone),
            Util.getDownloadWorkerExecutor())
        .thenApply(stream -> new RadioToneStream(stream, RadioTone.Band.RADIO));
  }
}
