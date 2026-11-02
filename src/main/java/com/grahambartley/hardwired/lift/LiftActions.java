package com.grahambartley.hardwired.lift;

import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.LiftFloorBlock;
import com.grahambartley.hardwired.device.ActionRate;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.network.LiftChoosePayload;
import com.grahambartley.hardwired.network.LiftViewPayload;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class LiftActions {

  private static final int MIN_TICKS_BETWEEN_USES = 5;
  private static final int MIN_TICKS_BETWEEN_CHOICES = 5;
  private static final ActionRate USES = new ActionRate(MIN_TICKS_BETWEEN_USES);
  private static final ActionRate CHOICES = new ActionRate(MIN_TICKS_BETWEEN_CHOICES);

  private LiftActions() {}

  public static void forget(ServerPlayerEntity player) {
    USES.forget(player.getUuid());
    CHOICES.forget(player.getUuid());
  }

  public static void use(ServerPlayerEntity player, BlockPos gate) {
    ServerWorld world = player.getServerWorld();
    if (!DeviceActions.canUse(player, gate) || !USES.allow(player.getUuid(), world.getTime())) {
      return;
    }
    Optional<LiftShaft> found = LiftShaft.forGate(world, gate);
    if (found.isEmpty()) {
      refuse(player, gate, DeviceStatus.NO_LIFT);
      return;
    }
    LiftShaft shaft = found.get();
    int level = gate.getY();
    showOn(world, gate, DeviceStatus.READY);
    showOn(world, gate.up(), DeviceStatus.READY);
    Optional<LiftCarEntity> car = shaft.car().or(() -> spawn(shaft, level));
    if (car.isEmpty()) {
      refuse(player, gate, DeviceStatus.SHAFT_BLOCKED);
    } else if (car.get().run().restingAt(level)) {
      ServerPlayNetworking.send(
          player, new LiftViewPayload(gate, LiftShaft.levels(shaft.gates()), level));
    } else {
      call(player, gate, car.get().driver(world), level);
    }
  }

  private static Optional<LiftCarEntity> spawn(LiftShaft shaft, int level) {
    if (!shaft.pathClear(level, level)) {
      return Optional.empty();
    }
    LiftCarEntity car = shaft.carType().create(shaft.world());
    if (car == null) {
      return Optional.empty();
    }
    car.placeInShaft(shaft.head(), level);
    shaft.gateFacing(level).ifPresent(car::faceGate);
    shaft.world().spawnEntity(car);
    shaft.showGates(shaft.gates(), level, true);
    return Optional.of(car);
  }

  private static void call(ServerPlayerEntity player, BlockPos gate, LiftDriver driver, int level) {
    DeviceStatus refused = driver.refusal(level);
    if (refused != null) {
      refuse(player, gate, refused);
    } else {
      driver.call(level);
      player.sendMessage(Text.translatable("device.hardwired.lift.called"), true);
    }
  }

  private static void refuse(ServerPlayerEntity player, BlockPos gate, DeviceStatus status) {
    ServerWorld world = player.getServerWorld();
    showOn(world, gate, status);
    showOn(world, gate.up(), status);
    player.sendMessage(Text.translatable(status.translationKey()), true);
  }

  private static void showOn(ServerWorld world, BlockPos pos, DeviceStatus status) {
    if (world.getBlockEntity(pos) instanceof ElectronicBlockEntity entity) {
      entity.setStatus(status);
    }
  }

  public static void choose(ServerPlayerEntity player, LiftChoosePayload choice) {
    ServerWorld world = player.getServerWorld();
    BlockPos gate = choice.gate();
    if (!DeviceActions.canUse(player, gate)
        || !(world.getBlockState(gate).getBlock() instanceof LiftFloorBlock)
        || !CHOICES.allow(player.getUuid(), world.getTime())) {
      return;
    }
    Optional<LiftShaft> shaft = LiftShaft.forGate(world, gate);
    Optional<LiftCarEntity> car = shaft.flatMap(LiftShaft::car);
    if (car.isEmpty() || !car.get().run().restingAt(gate.getY())) {
      return;
    }
    List<Integer> levels = LiftShaft.levels(shaft.get().gates());
    if (!levels.contains(choice.level())) {
      return;
    }
    LiftDriver driver = car.get().driver(world);
    DeviceStatus refused = driver.refusal(choice.level());
    if (refused != null) {
      refuse(player, gate, refused);
    } else {
      driver.goNow(choice.level());
    }
  }
}
