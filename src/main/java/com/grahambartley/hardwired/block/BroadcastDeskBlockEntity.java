package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.radio.DeskMic;
import com.grahambartley.hardwired.radio.DeskRun;
import com.grahambartley.hardwired.radio.Frequency;
import com.grahambartley.hardwired.radio.SongClock;
import com.grahambartley.hardwired.radio.Station;
import com.grahambartley.hardwired.radio.StationRegistry;
import com.grahambartley.hardwired.radio.TowerFinder;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class BroadcastDeskBlockEntity extends ElectronicBlockEntity {

  private static final String FREQUENCY = "frequency";
  private static final String DISC = "disc";
  private static final String WANTS_AIR = "wants_air";
  private static final String SONG_WANTED = "song_wanted";
  private static final String SONG_START = "song_start";
  private static final String ON_AIR_SINCE = "on_air_since";
  private static final String LAST_LIVE = "last_live";
  private static final String SPEAKER = "speaker";

  private int frequency = Frequency.DEFAULT;
  private ItemStack disc = ItemStack.EMPTY;
  private boolean wantsAir;
  private boolean songWanted;
  private long songStart = SongClock.STOPPED;
  private long onAirSince = DeskRun.NOT_ON_AIR;
  private long lastLive = DeskRun.NOT_ON_AIR;
  private TowerFinder.Found tower;
  private final DeskMic mic = new DeskMic();

  public BroadcastDeskBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.BROADCAST_DESK, pos, state);
  }

  public int frequency() {
    return frequency;
  }

  public void setFrequency(int kilohertz) {
    frequency = Frequency.snap(kilohertz);
    sync();
  }

  public boolean wantsAir() {
    return wantsAir;
  }

  public void setWantsAir(boolean wanted) {
    wantsAir = wanted;
    if (!wanted) {
      stopSong();
      mic.clear();
    }
    sync();
  }

  public boolean hasDisc() {
    return !disc.isEmpty();
  }

  public ItemStack disc() {
    return disc;
  }

  public void putDisc(ItemStack stack) {
    disc = stack;
    sync();
  }

  public ItemStack takeDisc() {
    ItemStack taken = disc;
    disc = ItemStack.EMPTY;
    stopSong();
    sync();
    return taken;
  }

  public boolean songPlaying() {
    return songWanted && songStart != SongClock.STOPPED;
  }

  public long songStart() {
    return songStart;
  }

  public Optional<RegistryEntry<JukeboxSong>> song() {
    return world == null
        ? Optional.empty()
        : JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), disc);
  }

  public void startSong(long now) {
    songWanted = true;
    songStart = now;
    sync();
  }

  public void stopSong() {
    songWanted = false;
    songStart = SongClock.STOPPED;
    sync();
  }

  public DeskMic mic() {
    return mic;
  }

  public void takeMic(UUID player, long nowMillis) {
    UUID before = mic.speaker();
    if (mic.take(player, nowMillis) && !player.equals(before)) {
      sync();
    }
  }

  public void releaseMic(UUID player) {
    if (mic.release(player)) {
      sync();
    }
  }

  public void clearMic() {
    mic.clear();
    sync();
  }

  public void useTower(TowerFinder.Found found) {
    tower = found;
  }

  public void afterBeat(DeviceStatus status, long now) {
    boolean live = status == DeviceStatus.RUNNING;
    long shownStart = songStart;
    songStart = DeskRun.songStartAfterBeat(songWanted, songStart, status, now, lastLive);
    if (songStart != SongClock.STOPPED
        && song()
            .map(entry -> SongClock.ended(now, songStart, entry.value().getLengthInTicks()))
            .orElse(true)) {
      stopSong();
    }
    onAirSince = DeskRun.onAirSinceAfterBeat(onAirSince, live, now, lastLive);
    if (live) {
      lastLive = now;
    }
    markDirty();
    if (songStart != shownStart) {
      sync();
    }
  }

  public void publish(ServerWorld server) {
    StationRegistry stations = StationRegistry.of(server);
    if (status() != DeviceStatus.RUNNING || tower == null) {
      stations.withdraw(pos);
      return;
    }
    stations.publish(
        new Station(
            pos, tower.foot(), frequency, tower.reach(), onAirSince, playingSongId(), songStart));
  }

  private Identifier playingSongId() {
    return songPlaying()
        ? song().flatMap(RegistryEntry::getKey).map(RegistryKey::getValue).orElse(null)
        : null;
  }

  @Override
  public void markRemoved() {
    super.markRemoved();
    if (world instanceof ServerWorld server) {
      StationRegistry.of(server).withdraw(pos);
    }
  }

  private void writeDesk(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    nbt.putInt(FREQUENCY, frequency);
    if (hasDisc()) {
      nbt.put(DISC, disc.encode(registries));
    }
    nbt.putBoolean(WANTS_AIR, wantsAir);
    nbt.putBoolean(SONG_WANTED, songWanted);
    nbt.putLong(SONG_START, songStart);
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    writeDesk(nbt, registries);
    nbt.putLong(ON_AIR_SINCE, onAirSince);
    nbt.putLong(LAST_LIVE, lastLive);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    frequency = nbt.contains(FREQUENCY) ? Frequency.snap(nbt.getInt(FREQUENCY)) : Frequency.DEFAULT;
    disc =
        nbt.contains(DISC, NbtElement.COMPOUND_TYPE)
            ? ItemStack.fromNbt(registries, nbt.getCompound(DISC)).orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;
    wantsAir = nbt.getBoolean(WANTS_AIR);
    songWanted = nbt.getBoolean(SONG_WANTED);
    songStart = nbt.contains(SONG_START) ? nbt.getLong(SONG_START) : SongClock.STOPPED;
    mic.show(nbt.containsUuid(SPEAKER) ? nbt.getUuid(SPEAKER) : null);
    onAirSince = nbt.contains(ON_AIR_SINCE) ? nbt.getLong(ON_AIR_SINCE) : DeskRun.NOT_ON_AIR;
    lastLive = nbt.contains(LAST_LIVE) ? nbt.getLong(LAST_LIVE) : DeskRun.NOT_ON_AIR;
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    super.removeFromCopiedStackNbt(nbt);
    nbt.remove(FREQUENCY);
    nbt.remove(DISC);
    nbt.remove(WANTS_AIR);
    nbt.remove(SONG_WANTED);
    nbt.remove(SONG_START);
    nbt.remove(ON_AIR_SINCE);
    nbt.remove(LAST_LIVE);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    NbtCompound nbt = super.toInitialChunkDataNbt(registries);
    writeDesk(nbt, registries);
    if (mic.speaker() != null) {
      nbt.putUuid(SPEAKER, mic.speaker());
    }
    return nbt;
  }
}
