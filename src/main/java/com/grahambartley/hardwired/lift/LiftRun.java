package com.grahambartley.hardwired.lift;

import com.grahambartley.hardwired.device.DeviceStatus;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public final class LiftRun {

  private static final String PHASE = "phase";
  private static final String LEVEL = "level";
  private static final String FROM = "from";
  private static final String TO = "to";
  private static final String PACE = "pace";
  private static final String TICKS = "ticks";
  private static final String BLOCKS = "blocks";
  private static final String PAUSE = "pause";
  private static final String REASON = "reason";
  private static final String CALLS = "calls";
  private static final String GATES = "gates";

  private final LiftQueue queue = new LiftQueue();
  private LiftPhase phase = LiftPhase.RESTING;
  private double level;
  private LiftTrip trip;
  private int ticks;
  private int blocksStarted;
  private int pause;
  private DeviceStatus stopReason = DeviceStatus.READY;
  private List<BlockPos> knownGates = List.of();
  private int serial;

  public LiftQueue queue() {
    return queue;
  }

  public LiftPhase phase() {
    return phase;
  }

  public double level() {
    return level;
  }

  public LiftTrip trip() {
    return trip;
  }

  public int ticks() {
    return ticks;
  }

  public int serial() {
    return serial;
  }

  public DeviceStatus stopReason() {
    return stopReason;
  }

  public int blocksStarted() {
    return blocksStarted;
  }

  public List<BlockPos> knownGates() {
    return knownGates;
  }

  public void rememberGates(List<BlockPos> gates) {
    knownGates = List.copyOf(gates);
  }

  public boolean restingAt(int floor) {
    return phase == LiftPhase.RESTING && level == floor;
  }

  public void placeAt(double startLevel) {
    level = startLevel;
  }

  public void begin(LiftTrip next, LiftPhase moving) {
    trip = next;
    phase = moving;
    ticks = 0;
    blocksStarted = 0;
    serial++;
  }

  public void advance() {
    ticks++;
    level = trip.levelAt(ticks);
  }

  public int ticksAhead() {
    return ticks + 1;
  }

  public void startBlock(int block) {
    blocksStarted = block;
  }

  public void stop(DeviceStatus reason, int pauseTicks) {
    trip = null;
    phase = LiftPhase.STOPPED;
    stopReason = reason;
    pause = pauseTicks;
    queue.clear();
    serial++;
  }

  public void rest(double restLevel, int pauseTicks) {
    trip = null;
    phase = LiftPhase.RESTING;
    level = restLevel;
    pause = pauseTicks;
    stopReason = DeviceStatus.READY;
    serial++;
  }

  public boolean pausing() {
    if (pause > 0) {
      pause--;
      return true;
    }
    return false;
  }

  public void clearPause() {
    pause = 0;
  }

  public void write(NbtCompound nbt) {
    nbt.putString(PHASE, phase.name());
    nbt.putDouble(LEVEL, level);
    nbt.putInt(PAUSE, pause);
    nbt.putString(REASON, stopReason.id());
    nbt.putIntArray(CALLS, queue.toArray());
    nbt.putLongArray(GATES, knownGates.stream().mapToLong(BlockPos::asLong).toArray());
    if (trip != null) {
      nbt.putDouble(FROM, trip.fromLevel());
      nbt.putDouble(TO, trip.toLevel());
      nbt.putInt(PACE, trip.ticksPerBlock());
      nbt.putInt(TICKS, ticks);
      nbt.putInt(BLOCKS, blocksStarted);
    }
  }

  public void read(NbtCompound nbt) {
    phase = LiftPhase.byName(nbt.getString(PHASE));
    level = nbt.getDouble(LEVEL);
    pause = nbt.getInt(PAUSE);
    stopReason = DeviceStatus.byId(nbt.getString(REASON), DeviceStatus.READY);
    queue.load(nbt.getIntArray(CALLS));
    knownGates = Arrays.stream(nbt.getLongArray(GATES)).mapToObj(BlockPos::fromLong).toList();
    trip =
        phase.travelling() && nbt.contains(PACE)
            ? new LiftTrip(nbt.getDouble(FROM), nbt.getDouble(TO), nbt.getInt(PACE))
            : null;
    if (trip == null) {
      phase = phase.travelling() ? LiftPhase.RESTING : phase;
      ticks = 0;
      blocksStarted = 0;
    } else {
      ticks = nbt.getInt(TICKS);
      blocksStarted = nbt.getInt(BLOCKS);
    }
    serial++;
  }
}
