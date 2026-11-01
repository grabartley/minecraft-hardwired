package com.grahambartley.hardwired.lift;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.power.PowerGrids;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class LiftDriver {

  public static final int DWELL_TICKS = 40;
  public static final int STOP_TICKS = 40;

  private static final int SYNC_TICKS = 20;
  private static final int ANNOUNCE_TICK = 2;
  private static final float CHAIN_VOLUME = 0.5F;
  private static final float CHAIN_PITCH = 0.9F;
  private static final float BELL_VOLUME = 1.0F;
  private static final float BELL_PITCH = 1.5F;

  private final LiftCarEntity car;
  private final LiftRun run;
  private final LiftShaft shaft;
  private final ServerWorld world;

  public LiftDriver(LiftCarEntity car, LiftRun run, LiftShaft shaft) {
    this.car = car;
    this.run = run;
    this.shaft = shaft;
    this.world = shaft.world();
  }

  public void tick() {
    if (!shaft.standing()) {
      car.discard();
      return;
    }
    switch (run.phase()) {
      case RESTING -> rest();
      case MOVING, LOWERING -> travel();
      case STOPPED -> {
        if (!run.pausing()) {
          afterStop();
        }
      }
    }
  }

  private void rest() {
    if (car.age % SYNC_TICKS == 0) {
      shaft.car().filter(first -> first != car).ifPresent(first -> car.discard());
      shaft.showGates(route(), run.level(), true);
    }
    if (!run.pausing() && !run.queue().isEmpty()) {
      depart(run.queue().poll());
    }
  }

  private List<BlockPos> route() {
    List<BlockPos> gates = shaft.gates();
    if (gates.isEmpty()) {
      gates = run.knownGates().stream().map(shaft::gateAt).flatMap(Optional::stream).toList();
    }
    run.rememberGates(gates);
    return gates;
  }

  private void depart(int level) {
    if (run.level() == level) {
      arrive(level);
      return;
    }
    DeviceStatus refused = refusal(level);
    if (refused != null) {
      LiftRiders.tell(
          LiftRiders.inColumn(shaft, run.level()), Text.translatable(refused.translationKey()));
      return;
    }
    shaft.showGates(route(), run.level(), false);
    LiftRiders.board(car, shaft);
    shaft.gateFacing(level).ifPresent(car::faceGate);
    run.begin(new LiftTrip(run.level(), level, ticksPerBlock()), LiftPhase.MOVING);
    car.tripChanged();
    pulse();
  }

  public DeviceStatus refusal(int level) {
    if (!shaft.pathClear(run.level(), level)) {
      return DeviceStatus.SHAFT_BLOCKED;
    }
    PowerGrids power = WireEngine.of(world).power();
    return power.hasPower(shaft.head()) ? null : power.stopReason(shaft.head());
  }

  private void travel() {
    PowerGrids power = WireEngine.of(world).power();
    if (run.phase() == LiftPhase.MOVING && !power.hasPower(shaft.head())) {
      stopFor(power.stopReason(shaft.head()));
      return;
    }
    LiftTrip trip = run.trip();
    int started = trip.blocksStartedBy(run.ticksAhead());
    if (LiftCharge.owes(started, run.blocksStarted()) && !enterBlock(trip, started)) {
      return;
    }
    run.advance();
    car.moveToLevel(run.level());
    car.keepTripInStep();
    if (run.ticks() == ANNOUNCE_TICK && run.phase() == LiftPhase.MOVING) {
      announce(trip);
    }
    if (trip.arrivedBy(run.ticks())) {
      arrive((int) Math.round(trip.toLevel()));
    }
  }

  private boolean enterBlock(LiftTrip trip, int block) {
    int target = (int) Math.round(trip.toLevel());
    if (!shaft.pathClear(run.level(), trip.endOfBlock(block))
        || !LiftShaft.levels(route()).contains(target)) {
      blocked(trip.direction());
      return false;
    }
    run.startBlock(block);
    world.playSound(
        null,
        car.getBlockPos(),
        SoundEvents.BLOCK_CHAIN_STEP,
        SoundCategory.BLOCKS,
        CHAIN_VOLUME,
        CHAIN_PITCH);
    return true;
  }

  private void blocked(int direction) {
    List<Integer> levels = LiftShaft.levels(route());
    OptionalInt safe = LiftFloors.lastSafe(levels, run.level(), direction);
    tellRiders(DeviceStatus.SHAFT_BLOCKED);
    if (run.phase() == LiftPhase.MOVING
        && safe.isPresent()
        && shaft.pathClear(run.level(), safe.getAsInt())) {
      run.begin(new LiftTrip(run.level(), safe.getAsInt(), ticksPerBlock()), LiftPhase.MOVING);
      car.tripChanged();
      return;
    }
    stop(DeviceStatus.SHAFT_BLOCKED);
  }

  private void stopFor(DeviceStatus reason) {
    tellRiders(reason);
    stop(reason);
  }

  private void stop(DeviceStatus reason) {
    run.stop(reason, STOP_TICKS);
    car.tripChanged();
    pulse();
  }

  private void afterStop() {
    OptionalInt below = LiftFloors.atOrBelow(LiftShaft.levels(route()), run.level());
    if (below.isEmpty() || !shaft.pathClear(run.level(), below.getAsInt())) {
      letRidersDown();
    } else if (below.getAsInt() == run.level()) {
      arrive(below.getAsInt());
    } else {
      LiftTrip gentle =
          new LiftTrip(
              run.level(), below.getAsInt(), ticksPerBlock() * LiftSafety.LOWERING_SLOWDOWN);
      run.begin(gentle, LiftPhase.LOWERING);
      car.tripChanged();
      pulse();
    }
  }

  private void letRidersDown() {
    double drop = shaft.dropBelow(run.level());
    LiftRiders.aboard(car).forEach(rider -> LiftRiders.letDown(rider, drop));
    run.rest(run.level(), 0);
    car.tripChanged();
    pulse();
  }

  private void arrive(int level) {
    boolean ride = run.phase() == LiftPhase.MOVING;
    List<BlockPos> gates = route();
    run.rest(level, DWELL_TICKS);
    car.moveToLevel(level);
    car.tripChanged();
    shaft.showGates(gates, level, true);
    world.playSound(
        null,
        car.getBlockPos().up(),
        SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
        SoundCategory.BLOCKS,
        BELL_VOLUME,
        BELL_PITCH);
    Optional<BlockPos> gate = LiftRiders.nearestGate(gates, level);
    for (ServerPlayerEntity rider : LiftRiders.aboard(car)) {
      gate.ifPresentOrElse(
          spot -> LiftRiders.stepOut(world, rider, spot), () -> LiftRiders.letDown(rider, 0));
      if (ride) {
        ModCriteria.DEVICE.trigger(rider, ModCriteria.LIFT_RIDDEN);
      }
    }
    pulse();
  }

  public void call(int level) {
    run.queue().add(level);
  }

  public void goNow(int level) {
    if (run.queue().add(level) && run.queue().toArray().length == 1) {
      run.clearPause();
    }
  }

  public void release(ServerPlayerEntity rider) {
    Optional<BlockPos> gate = LiftRiders.nearestGate(route(), rider.getY());
    if (gate.isPresent()) {
      LiftRiders.stepOut(world, rider, gate.get());
    } else {
      LiftRiders.letDown(rider, shaft.dropBelow(run.level()));
    }
  }

  public void abandon() {
    List<BlockPos> gates = route();
    LiftRiders.aboard(car).forEach(this::release);
    shaft.showGates(gates, run.level(), false);
    shaft.mirror(gates, DeviceStatus.READY);
  }

  private void announce(LiftTrip trip) {
    List<Integer> levels = LiftShaft.levels(route());
    int floor = LiftFloors.number(levels, (int) Math.round(trip.toLevel()));
    LiftRiders.tell(
        LiftRiders.aboard(car), Text.translatable("device.hardwired.lift.going", floor));
  }

  private void tellRiders(DeviceStatus reason) {
    LiftRiders.tell(LiftRiders.aboard(car), Text.translatable(reason.translationKey()));
  }

  private void pulse() {
    ElectronicsDriver.pulse(world, shaft.head());
  }

  private static int ticksPerBlock() {
    return Hardwired.SERVER_CONFIG.lift().ticksPerBlock();
  }
}
